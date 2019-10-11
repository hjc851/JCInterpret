package jcinterpret.algorithm.optimalassignment

data class OptimalAssignmentResult<X, Y> (
    val litems: List<X>,
    val ritems: List<Y>,
    val matches: List<Triple<X, Y, Double>>
) {
    fun invertScores(): OptimalAssignmentResult<X, Y> = OptimalAssignmentResult (
        litems,
        ritems,
        matches.map { Triple(it.first, it.second, 1.0 - it.third) }
    )
}