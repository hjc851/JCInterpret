package jcinterpret.algorithm.optimalassignment

abstract class OptimalAssignmentAlgorithm {
    abstract fun execute(costs: Array<DoubleArray>): IntArray

    abstract fun <X, Y> execute (
        litems: List<X>,
        ritems: List<Y>,
        costs: Array<DoubleArray>,
        matchThreshold: Double = 0.0
    ): OptimalAssignmentResult<X, Y>

    protected fun <X, Y> makeResult (
        litems: List<X>,
        ritems: List<Y>,
        costs: Array<DoubleArray>,
        matchThreshold: Double,
        pairs: IntArray
    ): OptimalAssignmentResult<X, Y> {
        val matches = mutableListOf<Triple<X, Y, Double>>()

        pairs.forEachIndexed { lIndex, rIndex ->
            if (rIndex != -1) {
                val lhs = litems[lIndex]
                val rhs = ritems[rIndex]
                val similarity = 1.0 - costs[lIndex][rIndex]

                if (similarity >= matchThreshold) {
                    matches.add(Triple(lhs, rhs, similarity))
                }
            }
        }

        return OptimalAssignmentResult(litems, ritems, matches)
    }
}