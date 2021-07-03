package jcinterpret.testconsole.JA3.resilience

import com.fasterxml.jackson.annotation.JsonProperty
import jcinterpret.document.DocumentUtils
import java.nio.file.Paths

object ResilienceScoreAnalyserACTools {

    val root = Paths.get("/media/haydencheers/Data/SymbExec/acscores")

    val dss = arrayOf(
        "Collected_20",
        "Collected_40",
        "Collected_60",
        "Algorithms_20",
        "Algorithms_40",
        "Algorithms_60"
    )

    val collectedBases = listOf(
        "P1",
        "P2",
        "P3",
        "P4",
        "P5"
    )

    val algorithmsBases = listOf(
        "CHANGEMAKING_DP_1",
        "MST_KRUSKALS_2",
        "SORT_BUBBLE_1",
        "SORT_QUICKSORT_4",
        "CHANGEMAKING_DP_2",
        "MST_KRUSKALS_3",
        "SORT_BUBBLE_2",
        "STRINGMATCH_BM_1",
        "CHANGEMAKING_ITER_1",
        "MST_PRIMMS_1",
        "SORT_MERGE_1",
        "STRINGMATCH_BM_2",
        "CHANGEMAKING_ITER_2",
        "MST_PRIMMS_2",
        "SORT_MERGE_2",
        "STRINGMATCH_KMP_1",
        "CHANGEMAKING_REC_1",
        "MST_PRIMMS_3",
        "SORT_QUICKSORT_1",
        "STRINGMATCH_KMP_4",
        "CHANGEMAKING_REC_2",
        "MST_REVERSEDELETE_1",
        "SORT_QUICKSORT_2",
        "STRINGMATCH_RK_1",
        "MST_KRUSKALS_1",
        "MST_REVERSEDELETE_2",
        "SORT_QUICKSORT_3",
        "STRINGMATCH_RK_2"
    )

    val ds_bases = mapOf(
        "Collected_20" to collectedBases,
        "Collected_40" to collectedBases,
        "Collected_60" to collectedBases,
        "Algorithms_20" to algorithmsBases,
        "Algorithms_40" to algorithmsBases,
        "Algorithms_60" to algorithmsBases
    )

    val tools = listOf(
        "JPlag",
        "Plaggie",
        "Sherlock-Sydney",
        "Sherlock-Warwick",
        "Sim-3.0.2_Wine32",
        "Naive Program Dependence Graph"
    )

    data class Comparison (
        val lhs: String,
        val rhs: String,

        @JsonProperty("similarity")
        val sim: Double
    )

    @JvmStatic
    fun main(args: Array<String>) {
        for (tool in tools) {
            println(tool)

            for (ds in dss) {
                println(ds)

                val resultroot = root.resolve(ds)
                val bases = ds_bases[ds]!!

                val scoref = resultroot.resolve("scores-${tool}.json")
                val scores = DocumentUtils.readJson(scoref, Array<Comparison>::class)

                for (base in bases) {
                    val baseScores = scores.filter { it.lhs == base }
                        .sortedBy { it.rhs }

                    val l1 = baseScores.take(10).filterNot { it.sim == -1.0 }
                    val l2 = baseScores.drop(10).take(20).filterNot { it.sim == -1.0 }
                    val l3 = baseScores.drop(30).take(30).filterNot { it.sim == -1.0 }
                    val l4 = baseScores.drop(60).take(40).filterNot { it.sim == -1.0 }
                    val l5 = baseScores.drop(100).take(50).filterNot { it.sim == -1.0 }

                    val l1avg = l1.map { it.sim }.average()
                    val l2avg = l2.map { it.sim }.average()
                    val l3avg = l3.map { it.sim }.average()
                    val l4avg = l4.map { it.sim }.average()
                    val l5avg = l5.map { it.sim }.average()

                    print(base)

                    print(String.format("\t%2.2f", l1avg))
                    print(String.format("\t%2.2f", l2avg))
                    print(String.format("\t%2.2f", l3avg))
                    print(String.format("\t%2.2f", l4avg))
                    print(String.format("\t%2.2f", l5avg))

                    println()
                }

                println()
            }

            println("-------------------")
            println()
        }
    }
}