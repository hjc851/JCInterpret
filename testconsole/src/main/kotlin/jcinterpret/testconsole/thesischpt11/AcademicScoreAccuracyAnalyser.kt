package jcinterpret.testconsole.thesischpt11

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

object AcademicScoreAccuracyAnalyser {
    val score_root = Paths.get("/media/haydencheers/Research Data/ThesisChpt1011/academic-scores")
    val base_root = Paths.get("/media/haydencheers/Research Data/ThesisChpt1011/base-scores")

    val tool_names = arrayOf(
        "JPlag",
        "Plaggie",
        "Sherlock-Warwick",
        "Sim-3.0.2_Wine32",
        "Sherlock-Sydney",
        "Naive String Edit Distance",
        "Naive String Tiling",
        "Naive Token Edit Distance",
        "Naive Token Tiling",
        "Naive Tree Edit Distance",
        "Naive Program Dependence Graph"
    )

    fun loadScores(file: Path): List<Score> {
        return Files.lines(file)
            .map { line ->
                val comps = line.split("\t")
                return@map Score (
                    comps[0],
                    comps[1],
                    comps[2].toDouble()
                )
            }.toList()
            .filter { !it.score.isNaN() }
    }

    val chances = arrayOf(10,20,40,60,80,100)
    val levels = arrayOf(1,2,3,4,5)

    @JvmStatic
    fun main(args: Array<String>) {

        for (tool in tool_names) {
            println(tool)

            val score_groups: MutableMap<String, MutableMap<String, MutableList<Pair<Int, Int>>>> = mutableMapOf()

            val score_file = score_root.resolve("${tool}.txt")
            val base_score_file = base_root.resolve("${tool}.txt")

            val variant_scores = loadScores(score_file)
            val base_scores = loadScores(base_score_file).filterNot { it.score.isNaN() }

            for (chance in chances) {
                val chance_scores = variant_scores.filter { it.rhs.contains("-C${chance}-") }

                for (level in levels) {
                    val level_scores = chance_scores.filter { it.rhs.contains("-L${level}-") }
                        .groupBy { it.lhs }

                    for ((base_id, variants) in level_scores) {

                        val all_scores = (base_scores + variants)
                            .sortedByDescending { it.score }

                        val sus_scores = all_scores.take(level * 5)

                        val tp = sus_scores.filter { variants.contains(it) }
                        val errors = ((level * 5) - tp.size) * 2

                        score_groups.getOrPut("C${chance}") { mutableMapOf() }
                            .getOrPut("L${level}") { mutableListOf() }
                            .add(tp.size to errors)
                    }
                }
            }

            for (chance in chances) {
                print("C${chance}\t")

                for (level in levels) {
                    val tmp = score_groups.getValue("C${chance}")
                        .getValue("L${level}")

                    val tp = tmp.map { it.first }.sum().toDouble()
                    val err = tmp.map { it.second }.sum().toDouble()

                    val f = (tp) / (tp + 0.5 * err)

                    print("(L${level},${"%.2f".format(f)})")
//                    print("${"%.4f".format(f_score)}\t")
//                    print("${err_sum}}\t")
                }

                println()
            }

            println()
        }
    }
}