package jcinterpret.testconsole.utils

object BestMatchFinder {

    var MATCH_THRESHOLD = 0.5

    data class NonexclusiveMatch (
        val lmatches: List<Triple<TraceModel, TraceModel, Double>>,
        val rmatches: List<Triple<TraceModel, TraceModel, Double>>
    )

    fun bestMatches(ltraces: List<TraceModel>, rtraces: List<TraceModel>, costs: Array<DoubleArray>): NonexclusiveMatch {
        val bestMatchesL = mutableListOf<Triple<TraceModel, TraceModel, Double>>()
        val bestMatchesR = mutableListOf<Triple<TraceModel, TraceModel, Double>>()

        for (l in 0 until ltraces.size) {
            var highest = 0.0
            var highestIndex = -1

            for (r in 0 until rtraces.size) {
                val sim = 1.0 - costs[l][r]

                if (sim > highest && sim > MATCH_THRESHOLD) {
                    highest = sim
                    highestIndex = r
                }
            }

            if (highestIndex != -1) {
                bestMatchesL.add(Triple(ltraces[l], rtraces[highestIndex], highest))
            }
        }

        for (r in 0 until rtraces.size) {
            var highest = 0.0
            var highestIndex = -1

            for (l in 0 until ltraces.size) {
                val sim = 1.0 - costs[l][r]

                if (sim > highest && sim > MATCH_THRESHOLD) {
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
