package jcinterpret.testconsole.pipeline.comparison

import jcinterpret.comparison.iterative.IterativeGraphComparator
import jcinterpret.document.DocumentUtils
import jcinterpret.graph.serialization.EdgeSerializationAdapter
import jcinterpret.graph.serialization.GraphSerializationAdapter
import jcinterpret.graph.serialization.NodeSerializationAdapter
import jcinterpret.graph.serialization.toGraph
import jcinterpret.testconsole.pipeline.GraphManifest
import jcinterpret.testconsole.utils.BestMatchFinder
import jcinterpret.testconsole.utils.EntryPointTrace
import jcinterpret.testconsole.utils.ProjectModel
import jcinterpret.testconsole.utils.ProjectModelBuilder
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.max

class NoSecondaryConcernsException: Exception()

object ProcessedProjectComparator {

    var TAINT_MATCH_THRESHOLD = 0.8
    var NOISE_COMPONENT_THRESHOLD = 2   // Will remove components with size less than or equal to this value

    @JvmStatic
    fun main(args: Array<String>) {

        val scores = mutableListOf<Double>()

        for (i in 0 .. 49) {
            val lhs = ProjectModelBuilder.build(Paths.get("/media/haydencheers/Big Data/SymbExec/graphs/Algorithms_60/CHANGEMAKING_DP_1"))
            val rhs = ProjectModelBuilder.build(Paths.get("/media/haydencheers/Big Data/SymbExec/graphs/Algorithms_60/CHANGEMAKING_DP_1-L5-${i}"))

            val result = compareExecutionGraphsRemovingNoiseWithSubsetCoefficient(lhs, rhs)
            val sim = max(result.lsim, result.rsim)
            scores.add(sim)

            println("${i}\t${sim}")
        }

        println()

        val avg = scores.average()
        println("Average ${avg}")
    }

    fun GraphSerializationAdapter.removingNoise(): GraphSerializationAdapter {
//        val nodes = this.getNodeSet<Node>()
        val visited = nodes.map { it to false }.toMap().toMutableMap()

        val components = mutableListOf<Set<NodeSerializationAdapter>>()

        outer@for (node in nodes) {
            if (visited.getValue(node) == true)
                continue@outer

            val toVisit = Stack<NodeSerializationAdapter>()
            toVisit.push(node)

            val thisComponent = mutableSetOf<NodeSerializationAdapter>()

            inner@while (toVisit.isNotEmpty()) {
                val currentNode = toVisit.pop()

                // If visited, continue
                if (visited.getValue(currentNode) == true)
                    continue@inner

                // Flag visited
                visited[currentNode] = true
                thisComponent.add(currentNode)

                // Need to check the neighbours
                this.edges
                    .filter { it.source == currentNode.id || it.target == currentNode.id }
                    .map { edge ->
                        if (edge.source == currentNode.id) this.nodes.single { it.id == edge.target }
                        else this.nodes.single { it.id == edge.source }
                    }.filter { !visited.getValue(it) }
                    .toCollection(toVisit)
            }

            components.add(thisComponent)
        }

        val nodeSize = nodes.size
        val visitedSize = components.map { it.size }.sum()

        val newNodes = mutableListOf<NodeSerializationAdapter>()
        newNodes.addAll(this.nodes)

        val newEdges = mutableListOf<EdgeSerializationAdapter>()
        newEdges.addAll(this.edges)

        components.filter { it.size <= NOISE_COMPONENT_THRESHOLD }
            .forEach { component ->
                for (node in component) {
                    newNodes.remove(node)
                    newEdges.removeIf { it.source == node.id || it.target == node.id }
                }
            }

        val removedCount = nodes.size - newNodes.size
//        println("Removed ${removedCount}")

        return GraphSerializationAdapter(this.id, newNodes.toTypedArray(), newEdges.toTypedArray(), this.attributes)
    }

    fun compareExecutionGraphsRemovingNoise(lhs: ProjectModel, rhs: ProjectModel): Result {
        // Nothing to compare
        if (lhs.totalTraceCount == 0 || rhs.totalTraceCount == 0)
            return Result(lhs, rhs, 0.0, 0.0)

        // Array of mapping costs (inverse similarities)
        val ecosts = Array(lhs.totalTraceCount) { DoubleArray(rhs.totalTraceCount) { 1.0 } }

        // Number of comparisons - semaphore to wait for comparisons to finish
        val permits = 8// lhs.totalTraceCount * rhs.totalTraceCount
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

            // Load the left graph
            val lexecgraph = DocumentUtils.readObject(ltrace.executionGraphPath, GraphSerializationAdapter::class)
                .removingNoise()
                .toGraph()

            for (r in 0 until rtraces.size) {
                val rtrace = rtraces[r]

                // Load the right graph
                val rexecgraph = DocumentUtils.readObject(rtrace.executionGraphPath, GraphSerializationAdapter::class)
                    .removingNoise()
                    .toGraph()

                // Acquire permit
                sem.acquire()

                // Spin off compare jobs
                val execsimf = IterativeGraphComparator.compareAsync(lexecgraph, rexecgraph)

                // Future to join the tasks + handle score aggregation
                execsimf.thenRun {
                    val execsim = execsimf.get()
                    ecosts[l][r] = 1.0 - execsim.unionSim
//                    ecosts[l][r] = 1.0 - max(execsim.lrSim, execsim.rlSim)
                }.whenComplete { void, throwable ->
                    throwable?.printStackTrace()
                    sem.release()
                }
            }
        }

        // Wait for all the permits to be returned
        do {
        } while (!sem.tryAcquire(permits, 1, TimeUnit.SECONDS))

        // Find best matches
        val lBestMatches = ltraces.mapIndexed { index, trace ->
            var bestSim = 0.0
            var bestIndex = 0

            for (r in 0 until rtraces.size) {
                val sim = 1.0 - ecosts[index][r]

                if (sim > bestSim) {
                    bestSim = sim
                    bestIndex = r
                }
            }

            return@mapIndexed Triple(trace, rtraces[bestIndex], bestSim)
        }

        val rBestMatches = rtraces.mapIndexed { index, trace ->
            var bestSim = 0.0
            var bestIndex = 0

            for (l in 0 until ltraces.size) {
                val sim = 1.0 - ecosts[l][index]

                if (sim > bestSim) {
                    bestSim = sim
                    bestIndex = l
                }
            }

            return@mapIndexed Triple(ltraces[bestIndex], trace, bestSim)
        }

        // Calculate similarity
        val lsim = aggregateScores(arrayOf(lBestMatches), lmanifests, rmanifests, true)
        val rsim = aggregateScores(arrayOf(rBestMatches), lmanifests, rmanifests, false)

        // Return result
        return Result(lhs, rhs, lsim, rsim)
    }

    fun compareExecutionGraphsRemovingNoiseWithSubsetCoefficient(lhs: ProjectModel, rhs: ProjectModel): Result {
        // Nothing to compare
        if (lhs.totalTraceCount == 0 || rhs.totalTraceCount == 0)
            return Result(lhs, rhs, 0.0, 0.0)

        // Array of mapping costs (inverse similarities)
        val ecosts = Array(lhs.totalTraceCount) { DoubleArray(rhs.totalTraceCount) { 1.0 } }

        // Number of comparisons - semaphore to wait for comparisons to finish
        val permits = 8// lhs.totalTraceCount * rhs.totalTraceCount
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

            // Load the left graph
            val lexecgraph = DocumentUtils.readObject(ltrace.executionGraphPath, GraphSerializationAdapter::class)
                .removingNoise()
                .toGraph()

            for (r in 0 until rtraces.size) {
                val rtrace = rtraces[r]

                // Load the right graph
                val rexecgraph = DocumentUtils.readObject(rtrace.executionGraphPath, GraphSerializationAdapter::class)
                    .removingNoise()
                    .toGraph()

                // Acquire permit
                sem.acquire()

                // Spin off compare jobs
                val execsimf = IterativeGraphComparator.compareAsync(lexecgraph, rexecgraph)

                // Future to join the tasks + handle score aggregation
                execsimf.thenRun {
                    val execsim = execsimf.get()
//                    ecosts[l][r] = 1.0 - execsim.unionSim
                    ecosts[l][r] = 1.0 - max(execsim.lrSim, execsim.rlSim)
                }.whenComplete { void, throwable ->
                    throwable?.printStackTrace()
                    sem.release()
                }
            }
        }

        // Wait for all the permits to be returned
        do {
        } while (!sem.tryAcquire(permits, 1, TimeUnit.SECONDS))

        // Find best matches
        val lBestMatches = ltraces.mapIndexed { index, trace ->
            var bestSim = 0.0
            var bestIndex = 0

            for (r in 0 until rtraces.size) {
                val sim = 1.0 - ecosts[index][r]

                if (sim > bestSim) {
                    bestSim = sim
                    bestIndex = r
                }
            }

            return@mapIndexed Triple(trace, rtraces[bestIndex], bestSim)
        }

        val rBestMatches = rtraces.mapIndexed { index, trace ->
            var bestSim = 0.0
            var bestIndex = 0

            for (l in 0 until ltraces.size) {
                val sim = 1.0 - ecosts[l][index]

                if (sim > bestSim) {
                    bestSim = sim
                    bestIndex = l
                }
            }

            return@mapIndexed Triple(ltraces[bestIndex], trace, bestSim)
        }

        // Calculate similarity
        val lsim = aggregateScores(arrayOf(lBestMatches), lmanifests, rmanifests, true)
        val rsim = aggregateScores(arrayOf(rBestMatches), lmanifests, rmanifests, false)

        // Return result
        return Result(lhs, rhs, lsim, rsim)
    }

    fun compareExecutionGraphs(lhs: ProjectModel, rhs: ProjectModel): Result {
        // Nothing to compare
        if (lhs.totalTraceCount == 0 || rhs.totalTraceCount == 0)
            return Result(lhs, rhs, 0.0, 0.0)

        // Array of mapping costs (inverse similarities)
        val ecosts = Array(lhs.totalTraceCount) { DoubleArray(rhs.totalTraceCount) { 1.0 } }

        // Number of comparisons - semaphore to wait for comparisons to finish
        val permits = 8// lhs.totalTraceCount * rhs.totalTraceCount
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

            // Load the left graph
            val lexecgraph = DocumentUtils.readObject(ltrace.executionGraphPath, GraphSerializationAdapter::class).toGraph()

            for (r in 0 until rtraces.size) {
                val rtrace = rtraces[r]

                // Load the right graph
                val rexecgraph = DocumentUtils.readObject(rtrace.executionGraphPath, GraphSerializationAdapter::class).toGraph()

                // Acquire permit
                sem.acquire()

                // Spin off compare jobs
                val execsimf = IterativeGraphComparator.compareAsync(lexecgraph, rexecgraph)

                // Future to join the tasks + handle score aggregation
                execsimf.thenRun {
                    val execsim = execsimf.get()
                    ecosts[l][r] = 1.0 - execsim.unionSim
                }.whenComplete { void, throwable ->
                    throwable?.printStackTrace()
                    sem.release()
                }
            }
        }

        // Wait for all the permits to be returned
        do {
        } while (!sem.tryAcquire(permits, 1, TimeUnit.SECONDS))

        // Find best matches
        val lBestMatches = ltraces.mapIndexed { index, trace ->
            var bestSim = 0.0
            var bestIndex = 0

            for (r in 0 until rtraces.size) {
                val sim = 1.0 - ecosts[index][r]

                if (sim > bestSim) {
                    bestSim = sim
                    bestIndex = r
                }
            }

            return@mapIndexed Triple(trace, rtraces[bestIndex], bestSim)
        }

        val rBestMatches = rtraces.mapIndexed { index, trace ->
            var bestSim = 0.0
            var bestIndex = 0

            for (l in 0 until ltraces.size) {
                val sim = 1.0 - ecosts[l][index]

                if (sim > bestSim) {
                    bestSim = sim
                    bestIndex = l
                }
            }

            return@mapIndexed Triple(ltraces[bestIndex], trace, bestSim)
        }

        // Calculate similarity
        val lsim = aggregateScores(arrayOf(lBestMatches), lmanifests, rmanifests, true)
        val rsim = aggregateScores(arrayOf(rBestMatches), lmanifests, rmanifests, false)

        // Return result
        return Result(lhs, rhs, lsim, rsim)
    }

    fun compareTaint(lhs: ProjectModel, rhs: ProjectModel): Result {
        // Nothing to compare
        if (lhs.totalTraceCount == 0 || rhs.totalTraceCount == 0)
            return Result(lhs, rhs, 0.0, 0.0)

        // Array of mapping costs (inverse similarities)
        val ecosts = Array(lhs.totalTraceCount) { DoubleArray(rhs.totalTraceCount) { 1.0 } }

        // Number of comparisons - semaphore to wait for comparisons to finish
        val permits = 8// lhs.totalTraceCount * rhs.totalTraceCount
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

            // Load the left graph
            val ltaint = DocumentUtils.readObject(ltrace.taintGraphPath, GraphSerializationAdapter::class).toGraph()

            for (r in 0 until rtraces.size) {
                val rtrace = rtraces[r]

                // Load the right graph
                val rtaint = DocumentUtils.readObject(rtrace.taintGraphPath, GraphSerializationAdapter::class).toGraph()

                // Acquire permit
                sem.acquire()

                // Spin off compare jobs
                val execsimf = IterativeGraphComparator.compareAsync(ltaint, rtaint)

                // Future to join the tasks + handle score aggregation
                execsimf.thenRun {
                    val execsim = execsimf.get()
                    ecosts[l][r] = 1.0 - execsim.unionSim
                }.whenComplete { void, throwable ->
                    throwable?.printStackTrace()
                    sem.release()
                }
            }
        }

        // Wait for all the permits to be returned
        do {
        } while (!sem.tryAcquire(permits, 1, TimeUnit.SECONDS))

        // Find best matches
        val lBestMatches = ltraces.mapIndexed { index, trace ->
            var bestSim = 0.0
            var bestIndex = 0

            for (r in 0 until rtraces.size) {
                val sim = 1.0 - ecosts[index][r]

                if (sim > bestSim) {
                    bestSim = sim
                    bestIndex = r
                }
            }

            return@mapIndexed Triple(trace, rtraces[bestIndex], bestSim)
        }

        val rBestMatches = rtraces.mapIndexed { index, trace ->
            var bestSim = 0.0
            var bestIndex = 0

            for (l in 0 until ltraces.size) {
                val sim = 1.0 - ecosts[l][index]

                if (sim > bestSim) {
                    bestSim = sim
                    bestIndex = l
                }
            }

            return@mapIndexed Triple(ltraces[bestIndex], trace, bestSim)
        }

        // Calculate similarity
        val lsim = aggregateScores(arrayOf(lBestMatches), lmanifests, rmanifests, true)
        val rsim = aggregateScores(arrayOf(rBestMatches), lmanifests, rmanifests, false)

        // Return result
        return Result(lhs, rhs, lsim, rsim)
    }

    fun compare(lhs: ProjectModel, rhs: ProjectModel): Result {
        // Nothing to compare
        if (lhs.totalTraceCount == 0 || rhs.totalTraceCount == 0)
            return Result(lhs, rhs, 0.0, 0.0)

        // Array of mapping costs (inverse similarities)
        val ecosts = Array(lhs.totalTraceCount) { DoubleArray(rhs.totalTraceCount) { 1.0 } }
        val tcosts = Array(lhs.totalTraceCount) { DoubleArray(rhs.totalTraceCount) { 1.0 } }
        val scosts = Array(lhs.totalTraceCount) { DoubleArray(rhs.totalTraceCount) { 1.0 } }
        val acosts = Array(lhs.totalTraceCount) { DoubleArray(rhs.totalTraceCount) { 1.0 } }

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

            for (r in 0 until rtraces.size) {
                val rtrace = rtraces[r]

                // Load the right graphs
                val rexecgraph = DocumentUtils.readObject(rtrace.executionGraphPath, GraphSerializationAdapter::class).toGraph()
                val rtaint = DocumentUtils.readObject(rtrace.taintGraphPath, GraphSerializationAdapter::class).toGraph()
                val rscs = DocumentUtils.readObject(rtrace.secondaryConcernGraphPath, GraphSerializationAdapter::class).toGraph()

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

                        ecosts[l][r] = 1.0 - execsim.unionSim
                        tcosts[l][r] = 1.0 - taintsim.unionSim
                        scosts[l][r] = 1.0 - scsim.unionSim
                        acosts[l][r] = 1.0 - ((taintsim.unionSim + scsim.unionSim) / 2.0)

                        sem.release()
                    }
                    .exceptionally {
                        it.printStackTrace(System.err)
                        sem.release()
                        return@exceptionally null
                    }
            }
        }

        // Wait for all the permits to be returned
        do {
        } while (!sem.tryAcquire(permits, 10, TimeUnit.SECONDS))

//        val (lBestMatches, rBestMatches) = BestMatchFinder.bestMatches(ltraces, rtraces, acosts)
//
//        val lsim = aggregateScores(arrayOf(lBestMatches), lmanifests, rmanifests, aggregatingLeft = true)
//        val rsim = aggregateScores(arrayOf(rBestMatches), lmanifests, rmanifests, aggregatingLeft = false)

        // Find the best matches by the tainted values
        val (ltaintBestMatches, rtaintBestMatches) = BestMatchFinder.bestMatches (
            ltraces,
            rtraces,
            tcosts
        ) { _, _, sim, _ -> sim >= TAINT_MATCH_THRESHOLD }

        // Map the taint match scores to their secondary concern scores
        val lTaintMatchScores = ltaintBestMatches.map { (ltrace, rtrace, score) ->
            val lindex = ltraces.indexOf(ltrace)
            val rindex = rtraces.indexOf(rtrace)
            val scsim = 1.0 - scosts[lindex][rindex]
            Triple(ltrace, rtrace, scsim)
        }

        val rTaintMatchScores = rtaintBestMatches.map { (ltrace, rtrace, score) ->
            val lindex = ltraces.indexOf(ltrace)
            val rindex = rtraces.indexOf(rtrace)
            val scsim = 1.0 - scosts[lindex][rindex]
            Triple(ltrace, rtrace, scsim)
        }

        // Find the indicies that are excluded for execution graph comparison
        val lExcludedLIdxs = ltaintBestMatches.map { ltraces.indexOf(it.first) }
        val lExcludedRIdxs = ltaintBestMatches.map { rtraces.indexOf(it.second) }

        val rExcludedLIdxs = rtaintBestMatches.map { ltraces.indexOf(it.first) }
        val rExcludedRIdxs = rtaintBestMatches.map { rtraces.indexOf(it.second) }

        // Find the best matches of the remainders by their secondary concerns
        val (lExecBestMatches, rExecBestMatches) = BestMatchFinder.bestMatches (
            ltraces, rtraces, acosts
        ) { lidx, ridx, _, isLeft ->
            if (isLeft) {
                !lExcludedLIdxs.contains(lidx) && !lExcludedRIdxs.contains(ridx)
            } else {
                !rExcludedLIdxs.contains(lidx) && !rExcludedRIdxs.contains(ridx)
            }
        }

        // Aggregate the similarity scores
        val lsim = aggregateScores(arrayOf(lTaintMatchScores, lExecBestMatches), lmanifests, rmanifests, aggregatingLeft = true)
        val rsim = aggregateScores(arrayOf(rTaintMatchScores, rExecBestMatches), lmanifests, rmanifests, aggregatingLeft = false)

        // Return result
        return Result(lhs, rhs, lsim, rsim)
    }

    private fun aggregateScores (
        matchSets: Array<List<Triple<EntryPointTrace, EntryPointTrace, Double>>>,
        lmanifests: Map<String, GraphManifest>,
        rmanifests: Map<String, GraphManifest>,
        aggregatingLeft: Boolean
    ): Double {

        var sum = 0.0
        var weight = 0

        for (matches in matchSets) {
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
        }

        val sim = sum / weight
        return sim
    }

    data class Result (
        val l: ProjectModel,
        val r: ProjectModel,
        val lsim: Double,
        val rsim: Double
    )
}

