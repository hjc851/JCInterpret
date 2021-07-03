package jcinterpret.testconsole.thesischpt11

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

object AcademicScoreAnalyser {

    val score_root = Paths.get("/media/haydencheers/Research Data/ThesisChpt1011/academic-scores")

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

    private fun loadScores(file: Path): List<Score> {
        return Files.lines(file)
            .map { line ->
                val comps = line.split("\t")
                return@map Score (
                    comps[0],
                    comps[1],
                    comps[2].toDouble()
                )
            }.toList()
    }

    private fun loadBaseScores(file: Path): List<Score> {
        return Files.lines(file)
            .map { line ->
                val comps = line.split("\t")
                val score = comps[1].toDouble()
                return@map Score (
                    comps[0],
                    comps[0],
                    if (score.isNaN()) 100.0 else comps[1].toDouble()
                )
            }.toList()
    }

    val chances = arrayOf(10,20,40,60,80,100)
    val levels = arrayOf(1,2,3,4,5)

    @JvmStatic
    fun main(args: Array<String>) {
        for (tool in tool_names) {
            println(tool)

            val score_groups: MutableMap<String, MutableMap<String, MutableList<Double>>> = mutableMapOf()

            val score_file = score_root.resolve("${tool}.txt")
            val base_score_file = score_root.resolve("${tool}-base.txt")

            val variant_scores = loadScores(score_file)
            val base_scores = loadBaseScores(base_score_file)

            for (chance in chances) {
                val chance_scores = variant_scores.filter { it.rhs.contains("-C${chance}-") }

                for (level in levels) {
                    val level_scores = chance_scores.filter { it.rhs.contains("-L${level}-") }
                        .groupBy { it.lhs }

                    for ((base_id, variants) in level_scores) {
                        val base_score = base_scores.single { it.lhs == base_id }

                        val avg = variants.map { base_score.score - it.score }
                            .average()

                        score_groups.getOrPut("C${chance}") { mutableMapOf() }
                            .getOrPut("L${level}") { mutableListOf() }
                            .add(avg)
                    }
                }
            }

            for (chance in chances) {
                print("C${chance}\t")

                for (level in levels) {
                    val avg = score_groups.getValue("C${chance}")
                        .getValue("L${level}")
                        .average()

                    print("${avg}\t")
                }

                println()
            }

            println()
        }
    }
}