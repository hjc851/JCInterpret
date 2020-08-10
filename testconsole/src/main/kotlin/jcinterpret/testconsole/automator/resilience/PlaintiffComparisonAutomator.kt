package jcinterpret.testconsole.automator.resilience

import jcinterpret.testconsole.pipeline.comparison.ProcessedProjectComparator
import jcinterpret.testconsole.utils.ProjectModelBuilder
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.streams.toList

object PlaintiffComparisonAutomator {

//    val DS = "Collected_60"
//    val results = Paths.get("/media/haydencheers/Data/SymbExec/results/${DS}")
//    val pairs = listOf(
//        "/media/haydencheers/Data/SymbExec/graphs/${DS}/P1" to "P1",
//        "/media/haydencheers/Data/SymbExec/graphs/${DS}/P2" to "P2",
//        "/media/haydencheers/Data/SymbExec/graphs/${DS}/P3" to "P3",
//        "/media/haydencheers/Data/SymbExec/graphs/${DS}/P4" to "P4",
//        "/media/haydencheers/Data/SymbExec/graphs/${DS}/P5" to "P5"
//    )

    val DS = "Algorithms_60"
    val results = Paths.get("/media/haydencheers/Data/SymbExec/results/$DS")

    val template = "/media/haydencheers/Data/SymbExec/graphs/$DS/" to ""

    val bases = listOf(
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

    val pairs = bases.map {
        template.first + it to template.second + it
    }

    @JvmStatic
    fun main(args: Array<String>) {

        if (!Files.exists(results))
            Files.createDirectories(results)

        val start = Instant.now()
        ProcessedProjectComparator.TAINT_MATCH_THRESHOLD = 0.8

        for ((root, plaintiff) in pairs) {
            println("${root} - $plaintiff\n")

            val sims = doPlaintiffComp(
                Paths.get(root),
                plaintiff
            )

            val writer = Files.newBufferedWriter(results.resolve("${plaintiff}.txt"))
                .use {  writer ->
                writer.append("program\tsimilarity\n")
                for ((prog, sim) in sims) {
                    writer.append("${prog}\t${sim}\n")
                }
            }
        }

        val end = Instant.now()
        val elapsed = Duration.between(start, end)

        println()
        println("Elapsed time ${elapsed.seconds} (s)")
        println("Finished")
        System.exit(0)
    }
}

fun doPlaintiffComp(root: Path, plaintiff: String): Map<String, Double> {
    val projects = Files.list(root)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }
        .sortedBy { it.fileName.toString() }
        .map(ProjectModelBuilder::build)

    val sims = mutableMapOf<String, Double>()

    val lhs = projects.single { it.projectId == plaintiff }
    val defendents = projects.filterNot { it.projectId == plaintiff }

    if (lhs.entryPoints.isEmpty()) {
        println("No entry points in plaintiff. Exiting.")
        throw IllegalStateException()
    }

    for (rhs in defendents) {
        val rid = rhs.projectId

        if (rhs.entryPoints.isEmpty()) {
            println("No entry points in defendent. Continuing ...")
            continue
        }

        println("Comparing $plaintiff vs $rid")
        val result = ProcessedProjectComparator.compareExecutionGraphs(lhs, rhs)

        val (
            _lhs, _rhs,
            lsim, rsim
        ) = result

        sims[rid] = max(lsim, rsim) // (lsim + rsim) / 2.0
    }

    return sims
}