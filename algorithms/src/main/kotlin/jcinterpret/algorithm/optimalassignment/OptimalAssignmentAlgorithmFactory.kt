package jcinterpret.algorithm.optimalassignment

object OptimalAssignmentAlgorithmFactory: OptimalAssignmentAlgorithm() {
    var provider: OptimalAssignmentAlgorithm = HungarianOptimalAssignmentAlgorithm

    override fun execute(costs: Array<DoubleArray>): IntArray {
        return provider.execute(costs)
    }

    override fun <X, Y> execute(
        litems: List<X>,
        ritems: List<Y>,
        costs: Array<DoubleArray>,
        matchThreshold: Double
    ): OptimalAssignmentResult<X, Y> {
        return provider.execute(litems, ritems, costs, matchThreshold)
    }
}