package jcinterpret.testconsole.automator.accuracy

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

object AccuracyScoreEvaluator {

    val ac_tools = listOf(
//        "Sherlock-Sydney",
//        "Sherlock-Warwick",
//        "Plaggie",
        "JPlag"
//        ,
//        "Sim-3.0.2_Wine32",
//        "Naive Program Dependence Graph"
    )

    val ac_tool_base_results = Paths.get("/media/haydencheers/Data/SymbExec/acscores")
    val ac_tool_variant_results = Paths.get("/media/haydencheers/Data/SymbExec/variant_results_ac")

    val bplag_base_results = Paths.get("/media/haydencheers/Data/SymbExec/results-pairwise/")
    val bplag_variant_results = Paths.get("/media/haydencheers/Data/SymbExec/variant_results")

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

    data class Score (
        val lhs: String,
        val rhs: String,
        val sim: Double
    )

    fun loadBPlagBase(ds: String): List<Score> {
        val scoreFile = bplag_base_results.resolve("${ds}.txt")
        val scores = Files.lines(scoreFile)
            .map {
                val comps = it.split("\t")
                return@map Score (
                    comps[0],
                    comps[1],
                    comps[2].toDouble() * 100
                )
            }.use { it.toList() }
            .groupBy { setOf(it.lhs, it.rhs) }
            .map { it.value.maxBy { it.sim }!! }

        return scores
    }

    fun loadBPlagVariants(ds: String): List<Score> {
        val scoreFile = bplag_variant_results.resolve("${ds}.txt")
        val scores = Files.lines(scoreFile)
            .map {
                val comps = it.split("\t")
                return@map Score (
                    comps[0],
                    comps[1],
                    comps[2].toDouble() * 100
                )
            }.use { it.toList() }
            .groupBy { setOf(it.lhs, it.rhs) }
            .map { it.value.maxBy { it.sim }!! }

        return scores
    }

    fun loadACBase(ds: String, tool: String): List<Score> {
        val scoreFile = ac_tool_base_results.resolve("${tool}-${ds}.txt")
        val scores = Files.lines(scoreFile)
            .map {
                val comps = it.split("\t")
                return@map Score (
                    comps[0],
                    comps[1],
                    comps[2].toDouble()
                )
            }.use { it.toList() }
            .groupBy { setOf(it.lhs, it.rhs) }
            .map { it.value.maxBy { it.sim }!! }

        return scores
    }

    fun loadACVariants(ds: String, tool: String): List<Score> {
        val scoreFile = ac_tool_variant_results.resolve("${ds}-${tool}.txt")
        val scores = Files.lines(scoreFile)
            .map {
                val comps = it.split("\t")
                return@map Score (
                    comps[0],
                    comps[1],
                    comps[2].toDouble()
                )
            }.use { it.toList() }
            .groupBy { setOf(it.lhs, it.rhs) }
            .map { it.value.maxBy { it.sim }!! }

        return scores
    }

    @JvmStatic
    fun main(args: Array<String>) {
        for (ds in ds_names) {
            println(ds)

//            println("BPlag")
//            val bplag_base = loadBPlagBase(ds)
//            val bplag_variants = loadBPlagVariants(ds)
////            doRobustnessAnalysis(bplag_base, bplag_variants)
//            doAccuracyAnalysis(bplag_base, bplag_variants)
//            println()

            for (ac_tool in ac_tools) {
                println("${ac_tool}")
                val ac_tool_base = loadACBase(ds, ac_tool)
                val ac_tool_variants = loadACVariants(ds, ac_tool)
//                doRobustnessAnalysis(ac_tool_base, ac_tool_variants)
                doAccuracyAnalysis(ac_tool_base, ac_tool_variants)
                println()
            }

            println("------------")
            println()
        }
    }

    fun doAccuracyAnalysis(base: List<Score>, variants: List<Score>) {
        for (tchance in arrayOf(20, 40, 60)) {
            val tchance_str = "-T${tchance}"
            val chance_variants = variants.filter { it.lhs.contains(tchance_str) || it.rhs.contains(tchance_str) }

            for (l in 1 .. 5) {
                print("T${tchance}\tL${l}\t")

                val level_str = "-L${l}"
                val level_variants = chance_variants.filter { it.lhs.contains(level_str) || it.rhs.contains(level_str) }

                val combined = (base + level_variants).sortedByDescending { it.sim }

//                accuracyDetectingAll(combined, base, level_variants)
                optimalAccuracy(combined, base, level_variants)
            }
        }
    }

    private fun accuracyDetectingAll(combined: List<Score>, base: List<Score>, variants: List<Score>) {
        val variantCount = variants.size
        val foundVariants = mutableSetOf<Score>()
        val found = mutableSetOf<Score>()

        val itr = combined.iterator()
        while (foundVariants.size < variantCount && itr.hasNext()) {
            val score = itr.next()

            found.add(score)
            if (variants.contains(score))
                foundVariants.add(score)
        }

        val tp = found.filter { variants.contains(it) }.size.toDouble()
        val fp = found.filter { !variants.contains(it) }.size.toDouble()
        val fn = variants.filter { !found.contains(it) }.size.toDouble()

        val p = (tp) / (tp + fp)
        val r = (tp) / (tp + fn)
        val f = 2 * (p * r) / (p + r)

        val errors = (fp + fn).toInt()

        println("${String.format("%2.2f", p)}\t${String.format("%2.2f", r)}\t${String.format("%2.2f", f)}\t${errors}")
    }

    private fun optimalAccuracy(combined: List<Score>, base: List<Score>, variants: List<Score>) {
        data class AccuracyTuple (
            val p: Double,
            val r: Double,
            val f: Double,
            val errorCount: Int
        )

        val errorRates = mutableListOf<AccuracyTuple>()

        for (variant in variants) {
            val vidx = combined.indexOf(variant)

            val found = combined.subList(0, vidx+1)
            val rhs = combined.subList(vidx+1, combined.size)

            val tp = found.filter { variants.contains(it) }.size.toDouble()
            val fp = found.filter { !variants.contains(it) }.size.toDouble()
            val fn = variants.filter { !found.contains(it) }.size.toDouble()

            val p = (tp) / (tp + fp)
            val r = (tp) / (tp + fn)
            val f = 2 * (p * r) / (p + r)

            val errors = (fp + fn).toInt()

            errorRates.add(AccuracyTuple(
                p, r, f, errors
            ))
        }

        val (p, r, f, errors) = errorRates.minBy { it.errorCount }!!

        println("${String.format("%2.2f", p)}\t${String.format("%2.2f", r)}\t${String.format("%2.2f", f)}\t${errors}")
    }

    fun doRobustnessAnalysis(base: List<Score>, variants: List<Score>) {
        val base_avg = base.map { it.sim }.average()
        println("BASE AVG: ${base_avg}")

        for (tchance in arrayOf(20, 40, 60)) {
            print("T${tchance} ")

            val tchance_str = "-T${tchance}"
            val chance_variants = variants.filter { it.lhs.contains(tchance_str) || it.rhs.contains(tchance_str) }

            val chance_avg = chance_variants.map { it.sim }.average()
            println("AVG: ${chance_avg}")

            for (l in 1 .. 5) {
                print("L${l} ")

                val level_str = "-L${l}"
                val level_variants = chance_variants.filter { it.lhs.contains(level_str) || it.rhs.contains(level_str) }

                val level_avg = level_variants.map { it.sim }.average()
                println("AVG: ${level_avg}")
            }
        }
    }
}