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

        val algo = HungarianAlgorithm(costs)
        val result = algo.execute()

        return makeResult(litems, ritems, costs, matchThreshold, result)
    }
}