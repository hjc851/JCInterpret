package jcinterpret.testconsole

import jcinterpret.comparison.iterative.IterativeGraphComparator
import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.document.DocumentUtils
import jcinterpret.graph.analysis.concern.toGraph
import jcinterpret.testconsole.utils.buildTraceModel
import org.graphstream.ui.view.Viewer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

fun main(args: Array<String>) {
    val root = Paths.get(args[0])

    val projects = Files.list(root)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }

    for (l in 0 until projects.size) {
        val lproj = projects[l]
        for (r in l + 1 until projects.size) {
            val rproj = projects[r]

            println("${lproj.fileName} vs ${rproj.fileName}")
            compare(lproj, rproj)

            // Force run the
            System.gc()
        }
    }

}

private fun compare(lproj: Path, rproj: Path) {
    val lid = lproj.fileName.toString()
    val rid = rproj.fileName.toString()

    val ltraces = Files.list(lproj)
        .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser")}
        .use { it.toList() }
        .map { DocumentUtils.readObject(it, EntryPointExecutionTraces::class) }
        .flatMap { it.executionTraces.toList() }
        .mapIndexed { index, executionTrace ->
            buildTraceModel(
                lid,
                index,
                executionTrace
            )
        }

    val rtraces = Files.list(rproj)
        .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser")}
        .use { it.toList() }
        .map { DocumentUtils.readObject(it, EntryPointExecutionTraces::class) }
        .flatMap { it.executionTraces.toList() }
        .mapIndexed { index, executionTrace ->
            buildTraceModel(
                rid,
                index,
                executionTrace
            )
        }

    for (ltrace in ltraces) {
        for (rtrace in rtraces) {
            val ltaint = ltrace.taint.graph
            val rtaint = rtrace.taint.graph

            val lsc = ltrace.secondaryConcerns.toGraph()
            val rsc = rtrace.secondaryConcerns.toGraph()

            val gsimf = IterativeGraphComparator.compareAsync(ltrace.ex.graph, rtrace.ex.graph)
            val taintsimf = IterativeGraphComparator.compareAsync(ltaint, rtaint)
            val scsimf = IterativeGraphComparator.compareAsync(lsc, rsc)

            val gsim = gsimf.get()
            val taintsim = taintsimf.get()
            val scsim = scsimf.get()

            ltrace.ex.graph.display().apply { this.closeFramePolicy = Viewer.CloseFramePolicy.CLOSE_VIEWER }
            rtrace.ex.graph.display().apply { this.closeFramePolicy = Viewer.CloseFramePolicy.CLOSE_VIEWER }

            ltaint.display().apply { this.closeFramePolicy = Viewer.CloseFramePolicy.CLOSE_VIEWER }
            rtaint.display().apply { this.closeFramePolicy = Viewer.CloseFramePolicy.CLOSE_VIEWER }

            println("EG: " + gsim)
            println("TT: " + taintsim)
            println("SC: " + scsim)

            print("Press any key -> ")
            readLine()
        }
    }
}

//    for (project in projects) {
//        val id = project.fileName.toString()
//
//        val eptraces = Files.list(project)
//            .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser")}
//            .use { it.toList() }
//
//        for (eptracepath in eptraces) {
//            val epname = eptracepath.fileName.toString()
//            val erptrace = DocumentUtils.readObject(eptracepath, EntryPointExecutionTraces::class)
//
//            erptrace.executionTraces.forEachIndexed { index, et ->
//                val eg = ExecutionGraphBuilder.build("$id - $epname - $index", et)
//                val taint = TaintedSubgraphFinder.find(eg.graph)
//                val sc = SecondaryConcernFinder.find(eg.graph, taint)
//
//                eg.graph.display().apply { this.closeFramePolicy = Viewer.CloseFramePolicy.CLOSE_VIEWER }
//                taint.graph.display().apply { this.closeFramePolicy = Viewer.CloseFramePolicy.CLOSE_VIEWER }
//                sc.toGraph().display().apply { this.closeFramePolicy = Viewer.CloseFramePolicy.CLOSE_VIEWER }
//
//                val result = IterativeGraphComparator.compare(eg.graph, eg.graph)
//                println(result)
//
////                val pg = LiteralChainGraphPruner.prune(eg.graph)
////                val pgtaint = TaintedSubgraphFinder.find(pg)
////                val pgsc = SecondaryConcernFinder.find(pg, pgtaint)
//
////                pg.display().apply { this.closeFramePolicy = Viewer.CloseFramePolicy.CLOSE_VIEWER }
////                pgtaint.graph.display().apply { this.closeFramePolicy = Viewer.CloseFramePolicy.CLOSE_VIEWER }
////                pgsc.toGraph().display().apply { this.closeFramePolicy = Viewer.CloseFramePolicy.CLOSE_VIEWER }
//
//                print("Press any key to continue ->")
//                readLine()
//            }
//        }
//    }
//}