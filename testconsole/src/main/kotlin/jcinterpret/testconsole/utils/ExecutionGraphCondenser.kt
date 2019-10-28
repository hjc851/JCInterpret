package jcinterpret.testconsole.utils

import jcinterpret.comparison.iterative.IterativeGraphComparator
import kotlin.streams.toList

class ExecutionGraphCondenser(val threshold: Double) {
    fun condenseTraces(traces: List<TraceModel>): List<TraceModel> {
        val reducedTraces = traces.toMutableList()

        var l = 0
        while (l < reducedTraces.size) {
            val lhs = reducedTraces[l]

            val ids = (0 until reducedTraces.size).toList()
                .parallelStream()
                .map { index ->
                    val rhs = reducedTraces[index]

                    if (System.identityHashCode(lhs) == System.identityHashCode(rhs)) return@map null

                    val tsim = IterativeGraphComparator.compare(lhs.ex.graph, rhs.ex.graph)

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
        }

        return reducedTraces
    }
}