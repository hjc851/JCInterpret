package jcinterpret.algorithm.optimalassignment

class GreedyAlgorithm(val costs: Array<DoubleArray>) {
    fun execute(): IntArray {
        val result = IntArray(costs.size) { -1 }
        val matchedColIndicies = BooleanArray(costs[0].size) { false }

        costs.forEachIndexed { rowIndex, row ->
            val sortedResults = row.mapIndexed { index, sim -> index to sim }
                .filter { !matchedColIndicies[it.first] }
                .sortedBy { it.second }

            val bestResult = sortedResults.firstOrNull()

            if (bestResult != null) {
                matchedColIndicies[bestResult.first] = true
                result[rowIndex] = bestResult.first
            }
        }

        return result
    }
}