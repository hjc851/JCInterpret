package jcinterpret.testconsole.JA3.resilience

import jcinterpret.testconsole.JA3.comparator.SingleProjectComparator
import jcinterpret.testconsole.utils.Forker
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Semaphore
import kotlin.math.max
import kotlin.streams.toList

object ResilienceBaseBaseComparisonAutomator {
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

    val graph_roots = listOf(
//        Paths.get("/media/haydencheers/Big Data/SymbExec/graphs/Algorithms_20"),
//        Paths.get("/media/haydencheers/Big Data/SymbExec/graphs/Algorithms_40"),
        Paths.get("/media/haydencheers/Big Data/SymbExec/graphs/Algorithms_60")
    )

//    val bases = listOf(
//        "P1",
//        "P2",
//        "P3",
//        "P4",
//        "P5"
//    )
//
//    val graph_roots = listOf(
////        Paths.get("/media/haydencheers/Big Data/SymbExec/graphs/Collected_20"),
////        Paths.get("/media/haydencheers/Big Data/SymbExec/graphs/Collected_40"),
//        Paths.get("/media/haydencheers/Big Data/SymbExec/graphs/Collected_60")
//    )

    val MAX_PARALLEL = 8
    val sem = Semaphore(MAX_PARALLEL)

    @JvmStatic
    fun main(args: Array<String>) {
        for (root in graph_roots) {
            println(root)

            val programs = Files.list(root)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .use { it.toList() }

            for (base in bases) {
                val base_path = programs.single { it.fileName.toString() == base }
                val outf = Files.createTempFile("jcinterpret_variant_comp_tmp", ".json")

                sem.acquire()
                CompletableFuture.runAsync {
                    val res = Forker.exec(
                        SingleProjectComparator::class.java,
                        arrayOf(
                            base_path.toAbsolutePath().toString(),
                            base_path.toAbsolutePath().toString(),
                            outf.toAbsolutePath().toString()
                        ),
                        props = arrayOf(
                            "-Xmx15G",
                            "-Djava.util.concurrent.ForkJoinPool.common.parallelism=8"
                        )
                    )

                    if (res == 0) {
                        val result = Files.newBufferedReader(outf).use { reader ->
                            ResilienceGraphComparisonAutomator.mapper.readValue(reader, SingleProjectComparator.SingleComparisonResult::class.java)
                        }

                        val maxSim = max(result.lsim, result.rsim) * 100.0

                        println("$base\t${maxSim}")
                    }

                }.whenComplete { t, u ->
                    sem.release()
                }
            }

            sem.acquire(MAX_PARALLEL)
            sem.release(MAX_PARALLEL)
            println()
        }
    }
}