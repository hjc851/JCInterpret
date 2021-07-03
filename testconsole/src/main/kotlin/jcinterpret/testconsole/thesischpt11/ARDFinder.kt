package jcinterpret.testconsole.thesischpt11

import java.nio.file.Paths

object ARDFinder {

    val base_root = Paths.get("/media/haydencheers/Research Data/ThesisChpt1011/base-scores")

    val tool_names = arrayOf(
        "JPlag",
        "Naive Program Dependence Graph",
        "Naive String Edit Distance",
        "Naive String Tiling",
        "Naive Token Edit Distance",
        "Naive Token Tiling",
        "Naive Tree Edit Distance",
        "Plaggie",
        "Sherlock-Sydney",
        "Sherlock-Warwick",
        "Sim-3.0.2_Wine32"
    )

    @JvmStatic
    fun main(args: Array<String>) {
        for (tool in tool_names) {
            val scores = AcademicScoreAccuracyAnalyser.loadScores(base_root.resolve("${tool}.txt"))
            val top = scores.sortedByDescending { it.score }
                .filterNot { it.score.isNaN() }
                .first()

            println("${tool} & ${"%.2f".format(top.score)}")
        }
    }
}