package jcinterpret.testconsole.utils

import jcinterpret.core.trace.ExecutionTrace
import jcinterpret.graph.analysis.concern.SecondaryConcern
import jcinterpret.graph.analysis.concern.SecondaryConcernFinder
import jcinterpret.graph.analysis.taint.TaintedSubgraph
import jcinterpret.graph.analysis.taint.TaintedSubgraphFinder
import jcinterpret.graph.execution.ExecutionGraph
import jcinterpret.graph.execution.ExecutionGraphBuilder
import jcinterpret.graph.optimisation.LiteralChainGraphPruner

data class TraceModel (
    val projId: String,
    val idx: Int,
    val ex: ExecutionGraph,
    val taint: TaintedSubgraph,
    val secondaryConcerns: List<SecondaryConcern>
)

object TraceModelBuilder {
    fun build(id: String, idx: Int, trace: ExecutionTrace): TraceModel {
//        println("\t\tBuilding graph")
        val egraph = ExecutionGraphBuilder.build("", trace)
        val pgraph = LiteralChainGraphPruner.prune(egraph.graph)
//        println("\t\tFinding taint")
        val taint = TaintedSubgraphFinder.find(pgraph)
//        println("\t\tFinding secondary concerns")
        val sc = SecondaryConcernFinder.find(pgraph, taint)

        return TraceModel(id, idx, egraph, taint, sc)
    }
}

fun buildTraceModel(id: String, idx: Int, trace: ExecutionTrace): TraceModel = TraceModelBuilder.build(id, idx, trace)
