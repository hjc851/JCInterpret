package jcinterpret.testconsole

import jcinterpret.comparison.iterative.IterativeGraphComparator
import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.document.DocumentUtils
import jcinterpret.graph.analysis.concern.SecondaryConcernFinder
import jcinterpret.graph.analysis.concern.toGraph
import jcinterpret.graph.analysis.taint.TaintedSubgraphFinder
import jcinterpret.graph.execution.ExecutionGraphBuilder
import org.graphstream.ui.view.Viewer
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

fun main(args: Array<String>) {
    val root = Paths.get(args[0])

    val projects = Files.list(root)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }

    for (l in 0 until projects.size) {
        for (r in l+1 until projects.size) {
            TODO()
        }
    }



    for (project in projects) {
        val id = project.fileName.toString()

        val eptraces = Files.list(project)
            .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser")}
            .use { it.toList() }

        for (eptracepath in eptraces) {
            val epname = eptracepath.fileName.toString()
            val erptrace = DocumentUtils.readObject(eptracepath, EntryPointExecutionTraces::class)

            erptrace.executionTraces.forEachIndexed { index, et ->
                val eg = ExecutionGraphBuilder.build("$id - $epname - $index", et)
                val taint = TaintedSubgraphFinder.find(eg.graph)
                val sc = SecondaryConcernFinder.find(eg.graph, taint)

                eg.graph.display().apply { this.closeFramePolicy = Viewer.CloseFramePolicy.CLOSE_VIEWER }
                taint.graph.display().apply { this.closeFramePolicy = Viewer.CloseFramePolicy.CLOSE_VIEWER }
                sc.toGraph().display().apply { this.closeFramePolicy = Viewer.CloseFramePolicy.CLOSE_VIEWER }

                val result = IterativeGraphComparator.compare(eg.graph, eg.graph)
                println(result)

//                val pg = LiteralChainGraphPruner.prune(eg.graph)
//                val pgtaint = TaintedSubgraphFinder.find(pg)
//                val pgsc = SecondaryConcernFinder.find(pg, pgtaint)

//                pg.display().apply { this.closeFramePolicy = Viewer.CloseFramePolicy.CLOSE_VIEWER }
//                pgtaint.graph.display().apply { this.closeFramePolicy = Viewer.CloseFramePolicy.CLOSE_VIEWER }
//                pgsc.toGraph().display().apply { this.closeFramePolicy = Viewer.CloseFramePolicy.CLOSE_VIEWER }

                print("Press any key to continue ->")
                readLine()
            }
        }
    }
}