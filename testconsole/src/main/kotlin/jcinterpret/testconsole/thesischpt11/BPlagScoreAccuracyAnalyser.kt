package jcinterpret.testconsole.thesischpt11

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

object BPlagScoreAccuracyAnalyser {
    val base_scores = Paths.get("/media/haydencheers/Data/ThesisChpt11/bplag-base.txt")
    val variant_score_root = Paths.get("/media/haydencheers/Data/ThesisChpt11/bplag-scores")

    val chances = arrayOf(10,20,40,60,80,100)
    val levels = arrayOf(1,2,3,4,5)

    private fun loadBaseScores(): List<Score> {
        return Files.lines(base_scores)
            .map {
                val comps = it.split("\t")
                return@map Score(
                    comps[0],
                    comps[1],
                    comps[2].toDouble()
                )
            }
            .use { it.toList() }
    }

    private fun loadVariantScores(): List<Score> {
        val variant_score_files = Files.list(variant_score_root)
            .filter { Files.isRegularFile(it) && !Files.isHidden(it) }
            .use { it.toList() }

        return variant_score_files.flatMap { file ->
            Files.lines(file)
                .skip(1)
                .map {
                    val comps = it.split("\t")
                    return@map Score(
                        comps[0],
                        comps[1],
                        comps[2].toDouble()
                    )
                }
                .use { it.toList() }
        }.filter { it.score != 0.0 }
            .filter { !it.score.isNaN() }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val base_scores = loadBaseScores()
        val variant_scores = loadVariantScores()

        val score_groups: MutableMap<String, MutableMap<String, MutableList<Pair<Int, Int>>>> = mutableMapOf()

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

        for (chance in AcademicScoreAccuracyAnalyser.chances) {
            print("C${chance}\t")

            for (level in AcademicScoreAccuracyAnalyser.levels) {
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