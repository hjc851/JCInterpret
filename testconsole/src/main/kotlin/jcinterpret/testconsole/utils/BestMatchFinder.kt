package jcinterpret.testconsole.utils

object BestMatchFinder {

    var MATCH_THRESHOLD = 0.7

    data class NonexclusiveMatch<T> (
        val lmatches: List<Triple<T, T, Double>>,
        val rmatches: List<Triple<T, T, Double>>
    )

    fun <T> bestMatches(ltraces: List<T>, rtraces: List<T>, costs: Array<DoubleArray>): NonexclusiveMatch<T> {
        val bestMatchesL = mutableListOf<Triple<T, T, Double>>()
        val bestMatchesR = mutableListOf<Triple<T, T, Double>>()

        for (l in 0 until ltraces.size) {
            var highest = -0.01
            var highestIndex = -1

            for (r in 0 until rtraces.size) {
                val sim = 1.0 - costs[l][r]

                if (sim > highest) {// && sim > MATCH_THRESHOLD) {
                    highest = sim
                    highestIndex = r
                }
            }

            if (highestIndex != -1) {
                bestMatchesL.add(Triple(ltraces[l], rtraces[highestIndex], highest))
            }
        }

        for (r in 0 until rtraces.size) {
            var highest = -0.1
            var highestIndex = -1

            for (l in 0 until ltraces.size) {
                val sim = 1.0 - costs[l][r]

                if (sim > highest) {//  && sim > MATCH_THRESHOLD) {
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
