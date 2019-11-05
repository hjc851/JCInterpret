package jcinterpret.testconsole.pipeline.comparison

import jcinterpret.comparison.iterative.IterativeGraphComparator
import jcinterpret.core.trace.TraceRecord
import jcinterpret.document.DocumentUtils
import jcinterpret.graph.serialization.GraphSerializationAdapter
import jcinterpret.graph.serialization.toGraph
import jcinterpret.testconsole.pipeline.GraphManifest
import jcinterpret.testconsole.utils.BestMatchFinder
import jcinterpret.testconsole.utils.EntryPointTrace
import jcinterpret.testconsole.utils.ProjectModel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class NoSecondaryConcernsException: Exception()

object ProcessedProjectComparator {
    fun compare(lhs: ProjectModel, rhs: ProjectModel): Result {
        // Nothing to compare
        if (lhs.totalTraceCount == 0 || rhs.totalTraceCount == 0)
            return Result(lhs, rhs, 0.0, 0.0)

        // Array of mapping costs (inverse similarities)
        val costs = Array(lhs.totalTraceCount) { DoubleArray(rhs.totalTraceCount) { 1.0 } }

        // Number of comparisons - semaphore to wait for comparisons to finish
        val permits = lhs.totalTraceCount * rhs.totalTraceCount
        val sem = Semaphore(permits)

        // Flatten all traces
        val ltraces = lhs.entryPointTraces.flatMap { it.value }
        val rtraces = rhs.entryPointTraces.flatMap { it.value }

        // Load the manifests
        val lmanifests = lhs.manifests
            .map { it.key to DocumentUtils.readObject(it.value, GraphManifest::class) }
            .toMap()

        val rmanifests = rhs.manifests
            .map { it.key to DocumentUtils.readObject(it.value, GraphManifest::class) }
            .toMap()

        // Do the comparisons
        for (l in 0 until ltraces.size) {
            val ltrace = ltraces[l]

            // Load the left graphs
            val lexecgraph = DocumentUtils.readObject(ltrace.executionGraphPath, GraphSerializationAdapter::class).toGraph()
            val ltaint = DocumentUtils.readObject(ltrace.taintGraphPath, GraphSerializationAdapter::class).toGraph()
            val lscs = DocumentUtils.readObject(ltrace.secondaryConcernGraphPath, GraphSerializationAdapter::class).toGraph()
//            val lassertions = DocumentUtils.readObject(ltrace.assertionsPath, Array<TraceRecord.Assertion>::class)

            for (r in 0 until rtraces.size) {
                val rtrace = rtraces[r]

                // Load the right graphs
                val rexecgraph = DocumentUtils.readObject(rtrace.executionGraphPath, GraphSerializationAdapter::class).toGraph()
                val rtaint = DocumentUtils.readObject(rtrace.taintGraphPath, GraphSerializationAdapter::class).toGraph()
                val rscs = DocumentUtils.readObject(rtrace.secondaryConcernGraphPath, GraphSerializationAdapter::class).toGraph()
//                val rassertions = DocumentUtils.readObject(rtrace.assertionsPath, Array<TraceRecord.Assertion>::class)

                // Spin off compare jobs
                val execsimf = IterativeGraphComparator.compareAsync(lexecgraph, rexecgraph)
                val taintsimf = IterativeGraphComparator.compareAsync(ltaint, rtaint)
                val scsimf = IterativeGraphComparator.compareAsync(lscs, rscs)

                // Acquire permit
                sem.acquire()

                // Future to join the tasks + handle score aggregation
                CompletableFuture.allOf(execsimf, taintsimf, scsimf)
                    .thenRun {
                        val execsim = execsimf.get()
                        val taintsim = taintsimf.get()
                        val scsim = scsimf.get()

//                        val sim = (taintsim.unionSim + scsim.unionSim) / 2.0 // (execsim.unionSim + 3*taintsim.unionSim + 2*scsim.unionSim) / 6.0
//                        val sim = (execsim.unionSim + 3*taintsim.unionSim + 2*scsim.unionSim) / 6.0

                        val sim = taintsim.unionSim
                        costs[l][r] = 1.0 - sim

                        sem.release()
                    }
                    .exceptionally {
                        println(it.localizedMessage)
                        sem.release()
                        return@exceptionally null
                    }
            }
        }

        // Wait for all the permits to be returned
        do {
        } while (!sem.tryAcquire(permits, 10, TimeUnit.SECONDS))

        // Find the best matches
        val (bestLMatches, bestRMatches) = BestMatchFinder.bestMatches(
            ltraces,
            rtraces,
            costs
        )

        // Calculate the similarity with weighted aggregation
        val lsim = aggregateScores(bestLMatches, lmanifests, rmanifests, aggregatingLeft = true)
        val rsim = aggregateScores(bestRMatches, lmanifests, rmanifests, aggregatingLeft = false)

        if (lsim == 0.0 || rsim == 0.0)
            Unit

        return Result(lhs, rhs, lsim, rsim)
    }

    private fun aggregateScores (
        matches: List<Triple<EntryPointTrace, EntryPointTrace, Double>>,
        lmanifests: Map<String, GraphManifest>,
        rmanifests: Map<String, GraphManifest>,
        aggregatingLeft: Boolean
    ): Double {

        var sum = 0.0
        var weight = 0

        for ((ltrace, rtrace, sim) in matches) {
            val lmanifest = lmanifests[ltrace.entryPoint]!!
            val lweight = lmanifest.graphWeights[ltrace.traceId]!!

            val rmanifest = rmanifests[rtrace.entryPoint]!!
            val rweight = rmanifest.graphWeights[rtrace.traceId]!!

            if (aggregatingLeft) {
                sum += sim * lweight
                weight += lweight
            } else {
                sum += sim * rweight
                weight += rweight
            }
        }

        val sim = sum / weight
        return sim

//        val oldsim = matches.map { it.third }.average()
//        val lsim = if (bestLMatches.isNotEmpty()) bestLMatches.map { it.third }.average() else 0.0
//        val rsim = if (bestRMatches.isNotEmpty()) bestRMatches.map { it.third }.average() else 0.0
    }

    data class Result (
        val l: ProjectModel,
        val r: ProjectModel,
        val lsim: Double,
        val rsim: Double
    )
}

