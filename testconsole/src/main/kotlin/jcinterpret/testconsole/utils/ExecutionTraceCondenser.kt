package jcinterpret.testconsole.utils

import jcinterpret.comparison.iterative.IterativeGraphComparator

class ExecutionTraceCondenser(val threshold: Double) {
    fun condenseTraces(traces: List<TraceModel>): List<TraceModel> {
        val reducedTraces = traces.toMutableList()

        var l = 0
        while (l < reducedTraces.size) {
            val lhs = reducedTraces[l]
            val riter = reducedTraces.iterator()

            while (riter.hasNext()) {
                val rhs = riter.next()

                if (System.identityHashCode(lhs) != System.identityHashCode(rhs)) {
                    val tsim = IterativeGraphComparator.compare(lhs.ex.graph, rhs.ex.graph)
                    if (tsim.avg() >= threshold)
                        riter.remove()
                }
            }

            l++
        }

        return reducedTraces
    }
}