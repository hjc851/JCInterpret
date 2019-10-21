package jcinterpret.testconsole

import jcinterpret.algorithm.optimalassignment.OptimalAssignmentAlgorithmFactory
import jcinterpret.comparison.iterative.IterativeGraphComparator
import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.core.trace.ExecutionTrace
import jcinterpret.document.DocumentUtils
import jcinterpret.graph.analysis.concern.SecondaryConcern
import jcinterpret.graph.analysis.concern.SecondaryConcernFinder
import jcinterpret.graph.analysis.concern.toGraph
import jcinterpret.graph.analysis.taint.TaintedSubgraph
import jcinterpret.graph.analysis.taint.TaintedSubgraphFinder
import jcinterpret.graph.execution.ExecutionGraph
import jcinterpret.graph.execution.ExecutionGraphBuilder
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.streams.toList
import kotlin.text.StringBuilder

val MATCH_THRESHOLD = 0.7

lateinit var out: PrintWriter

fun main(args: Array<String>) {
    val root = Paths.get(args[0])

    val fout = Paths.get(args[1]).resolve("${root.fileName}_${Date()}.txt")
    Files.createFile(fout)
    out = PrintWriter(Files.newBufferedWriter(fout))

    val projects = Files.list(root)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }

    for (l in 0 until projects.size) {
        val lproj = projects[l]
        for (r in l+1 until projects.size) {
            val rproj = projects[r]

            compare(lproj, rproj)

            // Force run the
            System.gc()
        }
    }
}

val topWaitPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()-2)

private fun compare(lproj: Path, rproj: Path) {

    val lid = lproj.fileName.toString()
    val rid = rproj.fileName.toString()

    val ltraces = Files.list(lproj)
        .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser")}
        .use { it.toList() }
        .map { DocumentUtils.readObject(it, EntryPointExecutionTraces::class) }
        .flatMap { it.executionTraces.toList() }
        .mapIndexed { index, executionTrace -> process(lid, index, executionTrace) }

    val rtraces = Files.list(rproj)
        .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser")}
        .use { it.toList() }
        .map { DocumentUtils.readObject(it, EntryPointExecutionTraces::class) }
        .flatMap { it.executionTraces.toList() }
        .mapIndexed { index, executionTrace -> process(rid, index, executionTrace) }

    val minCosts = Array<DoubleArray>(ltraces.size) { DoubleArray(rtraces.size) }
    val maxCosts = Array<DoubleArray>(ltraces.size) { DoubleArray(rtraces.size) }

    out.println("$lid-${ltraces.size}:$rid-${rtraces.size}")

    val futures = mutableListOf<CompletableFuture<Void>>()
    ltraces.forEachIndexed { lindex, ltrace ->
        rtraces.forEachIndexed { rindex, rtrace ->
            val ltaint = ltrace.taint.graph
            val rtaint = rtrace.taint.graph

            val lsc = ltrace.secondaryConcerns.toGraph()
            val rsc = rtrace.secondaryConcerns.toGraph()

            val taintsimf = IterativeGraphComparator.compareAsync(ltaint, rtaint)
            val scsimf = IterativeGraphComparator.compareAsync(lsc, rsc)

            val waitf = CompletableFuture.runAsync(Runnable {
                val taintsim = taintsimf.get()
                val scsim = scsimf.get()

                val minSim = (taintsim.min() + scsim.min()).div(2)
                val maxSim = (taintsim.max() + scsim.max()).div(2)

                minCosts[lindex][rindex] = 1.0 - minSim
                maxCosts[lindex][rindex] = 1.0 - maxSim

                val msg = """
                        $lindex:$rindex
                        T:${taintsim.first}:${taintsim.second}
                        S:${scsim.first}:${scsim.second}
                        MIN:$minSim
                        MAX:$maxSim
                    """.trimIndent()

                out.println(msg)
            }, topWaitPool)

            futures.add(waitf)
        }
    }

    // Wait for all the futures to finish
    futures.forEach { it.get() }

    val minMatches
            = OptimalAssignmentAlgorithmFactory.execute(ltraces, rtraces, minCosts, MATCH_THRESHOLD)

    val maxMatches
            = OptimalAssignmentAlgorithmFactory.execute(ltraces, rtraces, maxCosts, MATCH_THRESHOLD)

    val resultMsg = StringBuilder()

    resultMsg.appendln("MINMATCH")
    minMatches.matches.forEach { (ltrace, rtrace, sim) ->
        resultMsg.appendln("${ltrace.idx}:${rtrace.idx}:$sim")
    }

    resultMsg.appendln("MAXMATCH")
    maxMatches.matches.forEach { (ltrace, rtrace, sim) ->
        resultMsg.appendln("${ltrace.idx}:${rtrace.idx}:$sim")
    }

    val minSimSum = minMatches.matches.map { it.third }.sum()
    val lminsim = minSimSum.div(minMatches.litems.size)
    val rminsim = minSimSum.div(minMatches.ritems.size)

    resultMsg.appendln("MINSIM")
    resultMsg.appendln("L:$lminsim")
    resultMsg.appendln("R:$rminsim")

    val maxSimSum = maxMatches.matches.map { it.third }.sum()
    val lmaxsim = maxSimSum.div(maxMatches.litems.size)
    val rmaxsim = maxSimSum.div(maxMatches.ritems.size)

    resultMsg.appendln("MAXSIM")
    resultMsg.appendln("L:$lmaxsim")
    resultMsg.appendln("R:$rmaxsim")

    out.print(resultMsg.toString())
}

data class TraceModel (
    val projId: String,
    val idx: Int,
    val ex: ExecutionGraph,
    val taint: TaintedSubgraph,
    val secondaryConcerns: List<SecondaryConcern>
)

fun process(id: String, idx: Int, trace: ExecutionTrace): TraceModel {
    val egraph = ExecutionGraphBuilder.build("", trace)
    val taint = TaintedSubgraphFinder.find(egraph.graph)
    val sc = SecondaryConcernFinder.find(egraph.graph, taint)

    return TraceModel(id, idx, egraph, taint, sc)
}

fun Pair<Double, Double>.min(): Double {
    return minOf(first, second)
}

fun Pair<Double, Double>.max(): Double {
    return maxOf(first, second)
}