package jcinterpret.testconsole.JA3.hierachy

import jcinterpret.testconsole.JA3.accuracy.AccuracyScoreEvaluator
import java.nio.file.Files
import java.nio.file.Paths

object HierachicalProgramClassifier {
    @JvmStatic
    fun main(args: Array<String>) {
        val scoref = Paths.get("/media/haydencheers/Data/SymbExec/results-pairwise/algorithms.txt")
        val scores = Files.newBufferedReader(scoref).use { reader ->
            reader.readLines().map { line ->
                val comps = line.split("\t")
                return@map AccuracyScoreEvaluator.Score(
                    comps[0],
                    comps[1],
                    comps[2].toDouble()
                )
            }
        }

        Unit
    }
}