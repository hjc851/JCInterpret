package jcinterpret.testconsole.utils

object BestMatchFinder {

    data class NonexclusiveMatch<T> (
        val lmatches: List<Triple<T, T, Double>>,
        val rmatches: List<Triple<T, T, Double>>
    )

    fun <T> bestMatches(ltraces: List<T>, rtraces: List<T>, costs: Array<DoubleArray>, validator: (l: Int, r: Int, sim: Double, isLeft: Boolean) -> Boolean = { l, r, sim, isLeft -> true }): NonexclusiveMatch<T> {
        val bestMatchesL = mutableListOf<Triple<T, T, Double>>()
        val bestMatchesR = mutableListOf<Triple<T, T, Double>>()

        // Find the best matches for LHS->RHS
        for (l in 0 until ltraces.size) {
            var highest = -0.01
            var highestIndex = -1

            for (r in 0 until rtraces.size) {
                val sim = 1.0 - costs[l][r]

                if (sim > highest && validator(l, r, sim, true)) {// && sim > MATCH_THRESHOLD) {
                    highest = sim
                    highestIndex = r
                }
            }

            if (highestIndex != -1) {
                bestMatchesL.add(Triple(ltraces[l], rtraces[highestIndex], highest))
            }
        }

        // Find the best matches for RHS->LHS
        for (r in 0 until rtraces.size) {
            var highest = -0.1
            var highestIndex = -1

            for (l in 0 until ltraces.size) {
                val sim = 1.0 - costs[l][r]

                if (sim > highest && validator(l, r, sim, false)) {//  && sim > MATCH_THRESHOLD) {
                    highest = sim
                    highestIndex = l
                }
            }

            if (highestIndex != -1) {
                bestMatchesR.add(Triple(ltraces[highestIndex], rtraces[r], highest))
            }
        }

        return NonexclusiveMatch(bestMatchesL, bestMatchesR)
    }
}
