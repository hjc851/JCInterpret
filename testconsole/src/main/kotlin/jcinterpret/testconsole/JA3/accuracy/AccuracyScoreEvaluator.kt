package jcinterpret.testconsole.JA3.accuracy

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import jcinterpret.testconsole.plagiariser.mapper
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

object AccuracyScoreEvaluator {

    val ac_tools = listOf<String>(
//        "Plaggie",
//        "Sim-3.0.2_Wine32",
//        "Naive Program Dependence Graph",
//        "Sherlock-Warwick",
//        "Sherlock-Sydney",
//        "JPlag"
//        ,
    )

    val ac_tool_base_results = Paths.get("/media/haydencheers/Big Data/SymbExec/acscores")
    val ac_tool_variant_results = Paths.get("/media/haydencheers/Big Data/SymbExec/variant_results_ac")

    val bplag_base_results = Paths.get("/media/haydencheers/Big Data/SymbExec/results-pairwise/")
//    val bplag_variant_results = Paths.get("/media/haydencheers/Big Data/SymbExec/variant_results")

//    val bplag_base_results = Paths.get("/media/haydencheers/Big Data/SymbExec/results-nonoise-subsetcoef-pairwise/")
    val bplag_variant_results = Paths.get("/media/haydencheers/Big Data/SymbExec/variant_results_nonoise_subsetcoeffic")

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
            .sortedByDescending { it.sim }
            .filter {
                val lid = it.lhs.split("_")[0]
                val rid = it.rhs.split("_")[0]
                return@filter lid != rid
            }

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
            .sortedByDescending { it.sim }
            .filter {
                val lid = it.lhs.split("_")[0]
                val rid = it.rhs.split("_")[0]
                return@filter lid != rid
            }

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

    val susRoot = Paths.get("/media/haydencheers/Data/PrEP/comp")
    private fun loadSuspiciousPairs(ds: String): Set<Pair<String, String>> {
        val susfile = susRoot.resolve("${ds}.json")

        if (Files.exists(susfile)) {
            val json = Files.newBufferedReader(susfile).use { reader ->
                mapper.readValue(reader, ObjectNode::class.java)
            }

            val sus = json.get("knownSuspicious") as ArrayNode
            return sus.map {
                it as ObjectNode
                Pair(it.get("lhs").asText(), it.get("rhs").asText())
            }.toSet()
        }

        return emptySet()
    }

    // TOOL - TCHANCE - LEVEL : ERR_COUNT

    val TOTAL_RESULTS: MutableMap<String, MutableMap<String, MutableMap<String, Int>>> = mutableMapOf()

    @JvmStatic
    fun main(args: Array<String>) {
        for (ds in ds_names) {
            println(ds)
            val sus = loadSuspiciousPairs(ds)

            println("BPlag")
            val _bplag_base = loadBPlagBase(ds)
            val bplag_base = _bplag_base.filter { !sus.contains(it.lhs.replace("-", "") to it.rhs.replace("-", "")) && !sus.contains(it.rhs.replace("-", "") to it.lhs.replace("-", "")) }

            //.filterNot { sus.contains(it.lhs to it.rhs) || sus.contains(it.rhs to it.lhs) }
            val bplag_variants = loadBPlagVariants(ds)
//            doRobustnessAnalysis(bplag_base, bplag_variants)
            doAccuracyAnalysis("BPlag", bplag_base, bplag_variants)
            println()

            for (ac_tool in ac_tools) {
                println("${ac_tool}")
                val _ac_tool_base = loadACBase(ds, ac_tool)
                val ac_tool_base = _ac_tool_base.filter { !sus.contains(it.lhs.replace("-", "") to it.rhs.replace("-", "")) && !sus.contains(it.rhs.replace("-", "") to it.lhs.replace("-", "")) }
                val ac_tool_variants = loadACVariants(ds, ac_tool)
//                doRobustnessAnalysis(ac_tool_base, ac_tool_variants)
                doAccuracyAnalysis(ac_tool, ac_tool_base, ac_tool_variants)
                println()
            }

            println("------------")
            println()
        }

        println()
        println("TOTAL RESULTS")

        for (t in arrayOf("T20", "T40", "T60", "T80", "T100")) {
            println(t)

            for (tool in TOTAL_RESULTS.keys) {
                val res = TOTAL_RESULTS.getValue(tool)
                val tres = res.getValue(t)

//                println("\\addlegendentry{${tool}}")
//                println("\\addplot coordinates {")

                val scores = arrayOf("L1", "L2", "L3", "L4", "L5").map { tres.getValue(it) }
                    .joinToString("\t")

                val tool = tool.replace("Sim-3.0.2_Wine32", "Sim")
                    .replace("Sherlock-Warwick", "Sherlock-W")
                    .replace("Sherlock-Sydney", "Sherlock-S")

                println("$tool\t$scores")
            }

            println()
        }
    }

    fun doAccuracyAnalysis(tool: String, base: List<Score>, variants: List<Score>) {
        for (tchance in arrayOf(20, 40, 60, 80, 100)) {
            val tchance_str = "-T${tchance}"
            val chance_variants = variants.filter { it.lhs.contains(tchance_str) || it.rhs.contains(tchance_str) }

            for (l in 1 .. 5) {
                print("T${tchance}\tL${l}\t")

                val level_str = "-L${l}"
                val level_variants = chance_variants.filter { it.lhs.contains(level_str) || it.rhs.contains(level_str) }

                val combined = (base + level_variants).sortedByDescending { it.sim }

//                accuracyDetectingAll(combined, base, level_variants)
                val errCount = optimalAccuracyErrs(combined, base, level_variants)

                val existingErrCount = TOTAL_RESULTS.getOrPut(tool) { mutableMapOf() }
                    .getOrPut("T${tchance}") { mutableMapOf() }
                    .getOrPut("L${l}") { 0 }

                TOTAL_RESULTS.getValue(tool).getValue("T${tchance}").set("L${l}", errCount + existingErrCount)
            }
        }
    }

    private fun optimalAccuracyErrs(combined: List<Score>, base: List<Score>, variants: List<Score>): Int {
        data class AccuracyTuple (
            val p: Double,
            val r: Double,
            val f: Double,
            val errorCount: Int,
            val tp: Int,
            val fp: Int,
            val fn: Int
        )

        var lastError: AccuracyTuple? = null

        for (score in combined) {
            val vidx = combined.indexOf(score)

            val found = combined.subList(0, vidx+1)
            val rhs = combined.subList(vidx+1, combined.size)

            val tp = found.filter { variants.contains(it) }.size.toDouble()
            val fp = found.filter { !variants.contains(it) }.size.toDouble()
            val fn = variants.filter { !found.contains(it) }.size.toDouble()

            val _tp = found.filter { variants.contains(it) }
            val _fp = found.filter { !variants.contains(it) }
            val _fn = variants.filter { !found.contains(it) }

            val p = (tp) / (tp + fp)
            val r = (tp) / (tp + fn)
            val f = 2 * (p * r) / (p + r)

            val errors = (fp + fn).toInt()
            val error = AccuracyTuple(
                p, r, f, errors,
                _tp.size, _fp.size, _fn.size
            )

            if (lastError != null) {
                if (error.errorCount > lastError.errorCount)
                    break
            }

            lastError = error
        }

        val (p, r, f, errors) = lastError!!

        println("${String.format("%2.2f", p)}\t${String.format("%2.2f", r)}\t${String.format("%2.2f", f)}\t${errors}")
        return errors
    }
}