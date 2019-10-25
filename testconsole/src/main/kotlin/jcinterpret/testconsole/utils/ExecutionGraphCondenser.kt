package jcinterpret.testconsole.utils

import jcinterpret.comparison.iterative.IterativeGraphComparator
import kotlin.streams.toList

class ExecutionGraphCondenser(val threshold: Double) {
    fun condenseTraces(traces: List<TraceModel>): List<TraceModel> {
        val reducedTraces = traces.toMutableList()

        var l = 0
        while (l < reducedTraces.size) {
            val lhs = reducedTraces[l]

            (0 until reducedTraces.size).toList()
                .parallelStream()
                .map { index ->
                    val rhs = reducedTraces[index]

                    if (System.identityHashCode(lhs) != System.identityHashCode(rhs)) return@map null

                    val tsim = IterativeGraphComparator.compare(lhs.ex.graph, rhs.ex.graph)
                    return@map if (tsim >= threshold) index
                    else null
                }.toList()
                .filterNotNull()
                .sorted()
                .reversed()
                .forEach { reducedTraces.removeAt(it) }

            l++
        }

        return reducedTraces
    }
}