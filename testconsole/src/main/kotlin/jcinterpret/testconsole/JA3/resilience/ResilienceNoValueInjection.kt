package jcinterpret.testconsole.JA3.resilience

import com.fasterxml.jackson.databind.node.ObjectNode
import jcinterpret.testconsole.JA3.accuracy.AccuracyScoreEvaluator
import jcinterpret.testconsole.plagiariser.mapper
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

object ResilienceNoValueInjection {

    val src_root = Paths.get("/media/haydencheers/Big Data/SymbExec/src/")
    val results_root = Paths.get("/media/haydencheers/Big Data/SymbExec/results")

    val ds_names = listOf(
        "Algorithms_20",
        "Algorithms_40",
        "Algorithms_60",
        "Collected_20",
        "Collected_40",
        "Collected_60"
    )

    val assignDefaultValueKey = "class spplagiarise.obfuscation.filters.VariableDeclarationAssignDefaultValueFilter"
    val declareRedundantConstantsKey = "class spplagiarise.obfuscation.filters.DeclareRedundantConstantsFilter"

    @JvmStatic
    fun main(args: Array<String>) {
        for (ds in ds_names) {
            val ds_root = src_root.resolve(ds)

            val progs = Files.list(ds_root)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .use { it.toList() }

            val non_value_injection_programs = mutableListOf<String>()

            for (prog in progs) {
                val analyticsf = prog.resolve("analytics.json")

                if (Files.exists(analyticsf)) {
                    val json = Files.newBufferedReader(analyticsf).use { reader ->
                        mapper.readValue(reader, ObjectNode::class.java)
                    }

                    val mods = json.get("modifications") as ObjectNode

                    if (mods.get(assignDefaultValueKey) == null && mods.get(declareRedundantConstantsKey) == null) {
                        non_value_injection_programs.add(prog.fileName.toString())
                    }
                }
            }

            val base_prog_names = if (ds.contains("Collected")) {
                listOf(
                    "P1",
                    "P2",
                    "P3",
                    "P4",
                    "P5"
                )
            } else {
                progs.map { it.fileName.toString() }
                    .filterNot { it.contains("-L1-") || it.contains("-L2-") || it.contains("-L3-") || it.contains("-L4-") || it.contains("-L5-") }
                    .sorted()
            }

            val safe_l1 = non_value_injection_programs.filter { it.contains("-L1-") }
            val safe_l2 = non_value_injection_programs.filter { it.contains("-L2-") }
            val safe_l3 = non_value_injection_programs.filter { it.contains("-L3-") }
            val safe_l4 = non_value_injection_programs.filter { it.contains("-L4-") }
            val safe_l5 = non_value_injection_programs.filter { it.contains("-L5-") }

            val ds_results = collectScoreForDataset(ds)

            val l1_scores = ds_results.filter { (it.lhs.contains("-L1-") || it.rhs.contains("-L1-")) && (safe_l1.contains(it.lhs) || safe_l1.contains(it.rhs)) }
            val l2_scores = ds_results.filter { (it.lhs.contains("-L2-") || it.rhs.contains("-L2-")) && (safe_l2.contains(it.lhs) || safe_l2.contains(it.rhs)) }
            val l3_scores = ds_results.filter { (it.lhs.contains("-L3-") || it.rhs.contains("-L3-")) && (safe_l3.contains(it.lhs) || safe_l3.contains(it.rhs)) }
            val l4_scores = ds_results.filter { (it.lhs.contains("-L4-") || it.rhs.contains("-L4-")) && (safe_l4.contains(it.lhs) || safe_l4.contains(it.rhs)) }
            val l5_scores = ds_results.filter { (it.lhs.contains("-L5-") || it.rhs.contains("-L5-")) && (safe_l5.contains(it.lhs) || safe_l5.contains(it.rhs)) }

            println(ds)
            for (prog in base_prog_names) {
                val l1 = l1_scores.filter { it.lhs == prog || it.rhs == prog }.map { it.sim }.average() * 100.0
                val l2 = l2_scores.filter { it.lhs == prog || it.rhs == prog }.map { it.sim }.average() * 100.0
                val l3 = l3_scores.filter { it.lhs == prog || it.rhs == prog }.map { it.sim }.average() * 100.0
                val l4 = l4_scores.filter { it.lhs == prog || it.rhs == prog }.map { it.sim }.average() * 100.0
                val l5 = l5_scores.filter { it.lhs == prog || it.rhs == prog }.map { it.sim }.average() * 100.0

                println("${l1}\t${l2}\t${l3}\t${l4}\t${l5}")
            }
            println()

//            val l1_avg = l1.map { it.sim }.average()
//            val l2_avg = l2.map { it.sim }.average()
//            val l3_avg = l3.map { it.sim }.average()
//            val l4_avg = l4.map { it.sim }.average()
//            val l5_avg = l5.map { it.sim }.average()
//
//            println(ds)
//            println(l1_avg)
//            println(l2_avg)
//            println(l3_avg)
//            println(l4_avg)
//            println(l5_avg)
//            println()
        }
    }

    private fun collectScoreForDataset(ds: String): List<AccuracyScoreEvaluator.Score> {
        val ds_results_root = results_root.resolve(ds)

        val scoreFiles = Files.list(ds_results_root)
            .filter { Files.isRegularFile(it) && !Files.isHidden(it) }
            .use { it.toList() }

        val scores = mutableListOf<AccuracyScoreEvaluator.Score>()

        for (scoreFile in scoreFiles) {
            val fileScores = if (ds.contains("Algorithms")) {
                Files.lines(scoreFile)
                    .map {
                        val comps = it.split("\t")
                        return@map AccuracyScoreEvaluator.Score(
                            comps[0],
                            comps[1],
                            comps[2].toDouble()
                        )
                    }
            } else {
                Files.lines(scoreFile)
                    .skip(1)
                    .map {
                        val comps = it.split("\t")
                        return@map AccuracyScoreEvaluator.Score(
                            scoreFile.fileName.toString().removeSuffix(".txt"),
                            comps[0],
                            comps[1].toDouble()
                        )
                    }
            }
            .use { it.toList() }
            .groupBy { setOf(it.lhs, it.rhs) }
            .map { it.value.maxBy { it.sim }!! }

            scores.addAll(fileScores)
        }

        return scores
    }
}