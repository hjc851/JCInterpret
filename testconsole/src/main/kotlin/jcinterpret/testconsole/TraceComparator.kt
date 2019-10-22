package jcinterpret.testconsole

import jcinterpret.comparison.iterative.IterativeGraphComparator
import jcinterpret.graph.analysis.concern.toGraph
import java.io.PrintWriter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

object TraceComparator {
    fun compare(ltraces: List<TraceModel>, rtraces: List<TraceModel>, logg: PrintWriter) {

        if (ltraces.isEmpty() || rtraces.isEmpty()) return

        val minCosts = Array(ltraces.size) { DoubleArray(rtraces.size) }
        val maxCosts = Array(ltraces.size) { DoubleArray(rtraces.size) }
        val avgCosts = Array(ltraces.size) { DoubleArray(rtraces.size) }

        val permits = ltraces.size * rtraces.size
        val sem = Semaphore(permits)

        ltraces.forEachIndexed { lindex, ltrace ->
            rtraces.forEachIndexed { rindex, rtrace ->

                val ltaint = ltrace.taint.graph
                val rtaint = rtrace.taint.graph

                sem.acquire()

                println("Submitting $lindex vs $rindex")
                val future = CompletableFuture.supplyAsync {
                    val lsc = ltrace.secondaryConcerns.toGraph()
                    val rsc = rtrace.secondaryConcerns.toGraph()

                    return@supplyAsync lsc to rsc
                }.thenCompose { (lsc, rsc) ->

                    val taintsimf = IterativeGraphComparator.compareAsync(ltaint, rtaint)
                    val scsimf = IterativeGraphComparator.compareAsync(lsc, rsc)

                    return@thenCompose taintsimf.thenCombine(scsimf) { taintsim, scsim ->
                        return@thenCombine taintsim to scsim
                    }
                }.thenAccept { (taintsim, scsim) ->
                    val minSim = (taintsim.min() + scsim.min()).div(2)
                    val maxSim = (taintsim.max() + scsim.max()).div(2)
                    val avgSim = (taintsim.avg() + scsim.avg()).div(2)

                    minCosts[lindex][rindex] = 1.0 - minSim
                    maxCosts[lindex][rindex] = 1.0 - maxSim
                    avgCosts[lindex][rindex] = 1.0 - avgSim

                    val msg = """
                        $lindex:$rindex
                        T:${taintsim.first}:${taintsim.second}
                        S:${scsim.first}:${scsim.second}
                        MIN:$minSim
                        MAX:$maxSim
                    """.trimIndent()

                    logg.println(msg)
                    sem.release()
                }.exceptionally {
                    println("Exception in $lindex vs $rindex ... releasing")
                    sem.release()
                    return@exceptionally null
                }
            }
        }

        // Wait for all the futures to finish
        do {
            val poolTaskSize = ForkJoinPool.commonPool().queuedSubmissionCount

            println("Waiting on ${permits - sem.availablePermits()} of ${permits} permits ....")
            println("${poolTaskSize} remaining in the pool")

        } while (!sem.tryAcquire(permits, 30, TimeUnit.SECONDS))

        println("Acquired ${permits} permits")

        logg.println()

        val (bestLMinMatches, bestRMinMatches) = BestMatchFinder.bestMatches(ltraces, rtraces, minCosts)
        val (bestLMaxMatches, bestRMaxMatches) = BestMatchFinder.bestMatches(ltraces, rtraces, maxCosts)
        val (bestLAvgMatches, bestRAvgmatches) = BestMatchFinder.bestMatches(ltraces, rtraces, avgCosts)

        logg.println("MINMATCH")
        logg.println("L-R")
        logg.println(bestLMinMatches.size)
        bestLMinMatches.forEach { (ltrace, rtrace, sim) ->
            logg.println("${ltrace.idx}:${rtrace.idx}:$sim")
        }

        logg.println("R-L")
        logg.println(bestRMinMatches.size)
        bestRMinMatches.forEach { (ltrace, rtrace, sim) ->
            logg.println("${ltrace.idx}:${rtrace.idx}:$sim")
        }

        logg.println("MAXMATCH")
        logg.println("L-R")
        logg.println(bestLMaxMatches.size)
        bestLMaxMatches.forEach { (ltrace, rtrace, sim) ->
            logg.println("${ltrace.idx}:${rtrace.idx}:$sim")
        }

        logg.println("R-L")
        logg.println(bestRMaxMatches.size)
        bestRMaxMatches.forEach { (ltrace, rtrace, sim) ->
            logg.println("${ltrace.idx}:${rtrace.idx}:$sim")
        }

        logg.println("AVGMATCH")
        logg.println("L-R")
        logg.println(bestLAvgMatches.size)
        bestLAvgMatches.forEach { (ltrace, rtrace, sim) ->
            logg.println("${ltrace.idx}:${rtrace.idx}:$sim")
        }

        logg.println("R-L")
        logg.println(bestRAvgmatches.size)
        bestRAvgmatches.forEach { (ltrace, rtrace, sim) ->
            logg.println("${ltrace.idx}:${rtrace.idx}:$sim")
        }

        val lminsim = bestLMinMatches.map { it.third }
            .sum()
            .div(ltraces.size)

        val lmaxsim = bestLMaxMatches.map { it.third }
            .sum()
            .div(ltraces.size)

        val lavgsim = bestLAvgMatches.map { it.third }
            .sum()
            .div(ltraces.size)

        val rminsim = bestRMinMatches.map { it.third }
            .sum()
            .div(rtraces.size)

        val rmaxsim = bestRMaxMatches.map { it.third }
            .sum()
            .div(rtraces.size)

        val ravgsim = bestRAvgmatches.map { it.third }
            .sum()
            .div(rtraces.size)


        logg.println("TOTALSIM")
        logg.println("LMIN:$lminsim")
        logg.println("LMAX:$lmaxsim")
        logg.println("LAVG:$lavgsim")
        logg.println("RMIN:$rminsim")
        logg.println("RMAX:$rmaxsim")
        logg.println("RAVG:$ravgsim")

        logg.println()

        return
    }
}