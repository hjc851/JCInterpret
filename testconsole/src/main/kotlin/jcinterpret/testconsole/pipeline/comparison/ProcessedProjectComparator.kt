package jcinterpret.testconsole.pipeline.comparison

import jcinterpret.comparison.iterative.IterativeGraphComparator
import jcinterpret.document.DocumentUtils
import jcinterpret.testconsole.utils.BestMatchFinder
import jcinterpret.testconsole.utils.avg
import jcinterpret.testconsole.utils.max
import jcinterpret.testconsole.utils.min
import org.graphstream.graph.Graph
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

object ProcessedProjectComparator {
    fun compare(lhs: ProjectModel, rhs: ProjectModel): Result {

        val lsize = lhs.traces
            .values
            .flatMap { it.keys }
            .size

        val rsize = rhs.traces
            .values
            .flatMap { it.keys }
            .size

//        if (lsize == 0 || rsize == 0)
//            return Result(lhs, rhs, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

        if (lsize == 0 || rsize == 0)
            return Result(lhs, rhs, 0.0, 0.0)

//        val minCosts = Array(lsize) { DoubleArray(rsize) }
//        val maxCosts = Array(lsize) { DoubleArray(rsize) }
//        val avgCosts = Array(lsize) { DoubleArray(rsize) }

        val costs = Array(lsize) { DoubleArray(rsize) }

        val permits = lsize * rsize
        val sem = Semaphore(permits)

        var lcounter = 0

        val ltracearr = Array(lsize) { "" }
        val rtracearr = Array(rsize) { "" }

        for ((lep, ltraces) in lhs.traces) {
            for (ltrace in ltraces) {
                val (lid, lcomponents) = ltrace
                val lindex = lcounter++
                ltracearr[lindex] = (lep.fileName.toString() + "-" + lid)

                val ltaintpath = lcomponents.first { it.fileName.toString() == "${lid}-taint.ser"}
                val lscpath = lcomponents.first { it.fileName.toString() == "${lid}-scs.ser"}

                val ltaint = DocumentUtils.readObject(ltaintpath, Graph::class)
                val lscs = DocumentUtils.readObject(lscpath, Graph::class)

                var rcounter = 0
                for ((rep, rtraces) in rhs.traces) {
                    for (rtrace in rtraces) {
                        val (rid, rcomponents) = rtrace
                        val rindex = rcounter++
                        rtracearr[rindex] = (rep.fileName.toString() + "-" + rid)

                        val rtaintpath = rcomponents.first { it.fileName.toString() == "${rid}-taint.ser"}
                        val rscpath = rcomponents.first { it.fileName.toString() == "${rid}-scs.ser"}

                        val rtaint = DocumentUtils.readObject(rtaintpath, Graph::class)
                        val rscs = DocumentUtils.readObject(rscpath, Graph::class)

                        val taintsimf = IterativeGraphComparator.compareAsync(ltaint, rtaint)
                        val scsimf = IterativeGraphComparator.compareAsync(lscs, rscs)

//                        println("Acquiring for $lid vs $rid")
                        sem.acquire()

                        taintsimf.thenCombine(scsimf) { taintsim, scsim ->
//                            println("Compared $lid vs $rid graphs")
                            taintsim to scsim
                        }.thenAccept { (taintsim, scsim) ->
                            val sim = (taintsim + scsim) / 2.0
                            costs[lindex][rindex] = 1.0 - sim

//                            val minSim = (taintsim.min() + scsim.min()).div(2)
//                            val maxSim = (taintsim.max() + scsim.max()).div(2)
//                            val avgSim = (taintsim.avg() + scsim.avg()).div(2)
//
//                            minCosts[lindex][rindex] = 1.0 - minSim
//                            maxCosts[lindex][rindex] = 1.0 - maxSim
//                            avgCosts[lindex][rindex] = 1.0 - avgSim

//                            println("Releasing for $lid vs $rid: ${minSim}; ${avgSim}; ${maxSim};")
                            sem.release()
                        }.exceptionally {
                            println(it.localizedMessage)
                            sem.release()
                            return@exceptionally null
                        }
                    }
                }
            }
        }

        do {
//            println("Waiting on ${permits - sem.availablePermits()} of ${permits} permits ....")
        } while (!sem.tryAcquire(permits, 10, TimeUnit.SECONDS))
//        println("Acquired ${permits} permits")

        val lids = ltracearr.toList()
        val rids = rtracearr.toList()

        val (bestLMatches, bestRMatches) = BestMatchFinder.bestMatches(
            lids,
            rids,
            costs
        )

        val lsim = bestLMatches.map { it.third }
            .average() ?: 0.0

        val rsim = bestRMatches.map { it.third }
            .average() ?: 0.0

        return Result(lhs, rhs, lsim, rsim)

//        val (bestLMinMatches, bestRMinMatches) = BestMatchFinder.bestMatches(
//            lids,
//            rids,
//            minCosts
//        )
//        val (bestLMaxMatches, bestRMaxMatches) = BestMatchFinder.bestMatches(
//            lids,
//            rids,
//            maxCosts
//        )
//        val (bestLAvgMatches, bestRAvgmatches) = BestMatchFinder.bestMatches(
//            lids,
//            rids,
//            avgCosts
//        )
//
//        val lminsim = bestLMinMatches.map { it.third }
//            .sum()
//            .div(ltracearr.size)
//
//        val lmaxsim = bestLMaxMatches.map { it.third }
//            .sum()
//            .div(ltracearr.size)
//
//        val lavgsim = bestLAvgMatches.map { it.third }
//            .sum()
//            .div(ltracearr.size)
//
//        val rminsim = bestRMinMatches.map { it.third }
//            .sum()
//            .div(rtracearr.size)
//
//        val rmaxsim = bestRMaxMatches.map { it.third }
//            .sum()
//            .div(rtracearr.size)
//
//        val ravgsim = bestRAvgmatches.map { it.third }
//            .sum()
//            .div(rtracearr.size)
//
//        return Result (
//            lhs, rhs,
//            lminsim, lavgsim, lmaxsim,
//            rminsim, ravgsim, rmaxsim
//        )
    }

    data class Result (
        val l: ProjectModel,
        val r: ProjectModel,
        val lsim: Double,
        val rsim: Double
//        val lmin: Double,
//        val lavg: Double,
//        val lmax: Double,
//        val rmin: Double,
//        val ravg: Double,
//        val rmax: Double
    )
}

