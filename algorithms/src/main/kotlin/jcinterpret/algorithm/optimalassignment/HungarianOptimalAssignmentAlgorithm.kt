package jcinterpret.algorithm.optimalassignment

object HungarianOptimalAssignmentAlgorithm: OptimalAssignmentAlgorithm() {

    override fun execute(costs: Array<DoubleArray>): IntArray {
        return HungarianAlgorithm(costs).execute()
    }

    override fun <X, Y> execute(
        litems: List<X>,
        ritems: List<Y>,
        costs: Array<DoubleArray>,
        matchThreshold: Double
    ): OptimalAssignmentResult<X, Y> {

        for (i in 0 until costs.size) {
            val row = costs[i]
            for (j in 0 until row.size) {
                val value = row[j]

                if (value.isNaN() || value.isInfinite())
                    costs[i][j] = Double.MAX_VALUE
            }
        }

        val algo = HungarianAlgorithm(costs)
        val result = algo.execute()

        return makeResult(litems, ritems, costs, matchThreshold, result)
    }
}