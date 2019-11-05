package jcinterpret.testconsole.utils

import jcinterpret.comparison.iterative.IterativeGraphComparator
import kotlin.streams.toList

data class WeightedTraceModel(val model: TraceModel, val weight: Int)

class ExecutionGraphCondenser(val threshold: Double) {
    fun condenseTraces(traces: List<TraceModel>): List<WeightedTraceModel> {
        val reducedTraces = traces.toMutableList()
        val weightedModels = mutableListOf<WeightedTraceModel>()

        var l = 0
        while (l < reducedTraces.size) {
            val lhs = reducedTraces[l]

            val ids = (l+1 until reducedTraces.size).toList()
                .parallelStream()
                .map { index ->
                    val rhs = reducedTraces[index]
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