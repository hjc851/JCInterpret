package jcinterpret.testconsole.utils

import jcinterpret.comparison.iterative.IterativeGraphComparator
import org.graphstream.graph.Node
import kotlin.math.max
import kotlin.math.min
import kotlin.streams.toList

data class WeightedTraceModel(val model: TraceModel, val weight: Int)

class ExecutionGraphCondenser(val threshold: Double) {
    fun condenseTraces(traces: List<TraceModel>): List<WeightedTraceModel> {
        val reducedTraces = traces.toMutableList()
        val weightedModels = mutableListOf<WeightedTraceModel>()

        var l = 0
        while (l < reducedTraces.size) {
            val lhs = reducedTraces[l]
            val lsize = lhs.ex.graph.getNodeSet<Node>().count { it.degree > 0 }.toDouble()

            val ids = (l+1 until reducedTraces.size).toList()
                .parallelStream()
                .map { index ->
                    val rhs = reducedTraces[index]
                    val rsize = rhs.ex.graph.getNodeSet<Node>().count() { it.degree > 0 }.toDouble()

                    // Early Exit -- Not enough nodes that could match to meet threshold
                    if (min(lsize, rsize) / max(lsize, rsize) < threshold) {
                        return@map null
                    }

                    val tsim = IterativeGraphComparator.compare(lhs.ex.graph, rhs.ex.graph).unionSim

                    if (tsim >= threshold) {
                        println("\t\t\tRemoving $index (l=$l)")
                        return@map index
                    }

                    return@map null
                }.toList()
                .filterNotNull()

            ids.sorted()
                .reversed()
                .forEach { reducedTraces.removeAt(it) }

            l++
            weightedModels.add(WeightedTraceModel(lhs, ids.size+1))
        }

        return weightedModels
    }
}