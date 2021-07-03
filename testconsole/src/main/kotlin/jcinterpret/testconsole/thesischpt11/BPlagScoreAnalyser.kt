package jcinterpret.testconsole.thesischpt11

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

object BPlagScoreAnalyser {
    val score_root = Paths.get("/media/haydencheers/Research Data/ThesisChpt1011/bplag-scores")

    val chances = arrayOf(10,20,40,60,80,100)
    val levels = arrayOf(1,2,3,4,5)

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

    @JvmStatic
    fun main(args: Array<String>) {
        val scorefiles = Files.list(score_root)
            .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".txt") }
            .use { it.toList() }

        val score_groups: MutableMap<String, MutableMap<String, MutableList<Double>>> = mutableMapOf()

        for (file in scorefiles) {
//            println(file.fileName)

            val scores = loadScores(file)
            val base_score = scores.first()
            val variant_scores = scores.drop(1)

            val base_sim = base_score.score

            for (chance in chances) {
                val chance_scores = variant_scores.filter { it.rhs.contains("-C${chance}-") }

                for (level in levels) {
                    val level_scores = chance_scores.filter { it.rhs.contains("-L${level}-") }
                    var avg_level_score = level_scores.map { base_sim - it.score }.average() * 100
                    if (avg_level_score < 0 || avg_level_score.isNaN()) avg_level_score = 0.0

                    score_groups.getOrPut("C${chance}") { mutableMapOf() }
                        .getOrPut("L${level}") { mutableListOf() }
                        .add(avg_level_score)

//                    println("C${chance}\tL${level}\t${avg_level_score}")
                }
            }

//            println()
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

    }
}