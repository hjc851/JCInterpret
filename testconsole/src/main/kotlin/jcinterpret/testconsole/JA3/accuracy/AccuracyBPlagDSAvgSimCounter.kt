package jcinterpret.testconsole.JA3.accuracy

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

object AccuracyBPlagDSAvgSimCounter {

    val root = Paths.get("/media/haydencheers/Data/SymbExec/results-pairwise")

    val ds_names = listOf(
        "COMP2230_A1_2018",
        "COMP2240_A1_2018",
        "COMP2240_A2_2018",
        "COMP2240_A3_2018",
        "SENG1110_A1_2017",
        "SENG1110_A2_2017",
        "SENG2050_A1_2017",
        "SENG2050_A2_2017",
        "SENG2050_A1_2018",
        "SENG2050_A2_2018",
        "SENG2050_A1_2019",
        "SENG2050_A2_2019"
    )

    @JvmStatic
    fun main(args: Array<String>) {
        for (ds in ds_names) {
            val ds_score_f = root.resolve("${ds}.txt")
            val scores = Files.lines(ds_score_f)
                .map { it.split("\t").last().toDouble() }
                .use { it.toList() }

            val avg = scores.average()
            val min = scores.min()
            val max = scores.max()

            println("${ds}\t${avg}\t${min}\t${max}")
        }
    }
}