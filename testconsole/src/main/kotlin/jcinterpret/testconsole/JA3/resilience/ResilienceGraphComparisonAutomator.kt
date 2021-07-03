package jcinterpret.testconsole.JA3.resilience

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jcinterpret.testconsole.JA3.comparator.SingleProjectComparator
import jcinterpret.testconsole.utils.Forker
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import kotlin.streams.toList

object ResilienceGraphComparisonAutomator {

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
        Paths.get("/media/haydencheers/Data/SymbExec/graphs/Algorithms_20"),
        Paths.get("/media/haydencheers/Data/SymbExec/graphs/Algorithms_40"),
        Paths.get("/media/haydencheers/Data/SymbExec/graphs/Algorithms_60")
    )

    val results_out = Paths.get("/media/haydencheers/Data/SymbExec/results")

    val MAX_PARALLEL = 8
    val sem = Semaphore(MAX_PARALLEL)

    val mapper = ObjectMapper()
        .registerModule(KotlinModule())

    @JvmStatic
    fun main(args: Array<String>) {
        for (root in graph_roots) {
            println(root)

            val programs = Files.list(root)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .use { it.toList() }

            val set_out = results_out.resolve(root.fileName.toString())
            if (!Files.exists(set_out)) Files.createDirectories(set_out)

            for (base in bases) {
                val ds_results = set_out.resolve("$base.txt")
                if (Files.exists(ds_results)) continue

                val sims = ConcurrentHashMap<String, ConcurrentHashMap<String, Double>>()

                val base_path = programs.single { it.fileName.toString() == base }
                val variants = programs.filter { it.fileName.toString().startsWith(base) && it.fileName.toString() != base }

                for (variant in variants) {
                    val outf = Files.createTempFile("jcinterpret_variant_comp_tmp", ".json")

                    sem.acquire()
                    CompletableFuture.runAsync {
                        val res = Forker.exec(
                            SingleProjectComparator::class.java,
                            arrayOf(
                                base_path.toAbsolutePath().toString(),
                                variant.toAbsolutePath().toString(),
                                outf.toAbsolutePath().toString()
                            ),
                            props = arrayOf(
                                "-Xmx15G",
                                "-Djava.util.concurrent.ForkJoinPool.common.parallelism=8"
                            )
                        )

                        val lid = base
                        val rid = variant.fileName.toString()

                        if (res == 0) {
                            val result = Files.newBufferedReader(outf).use { reader ->
                                mapper.readValue(reader, SingleProjectComparator.SingleComparisonResult::class.java)
                            }

                            sims.getOrPut(lid) { ConcurrentHashMap() }.put(rid, result.lsim)
                            sims.getOrPut(rid) { ConcurrentHashMap() }.put(lid, result.rsim)

                            println("Finished ${lid} - ${rid}")

                        } else {
                            System.err.println("Failed ${lid} - ${rid}")
                        }

                    }.whenComplete { void, throwable ->
                        throwable?.printStackTrace(System.err)
                        sem.release()
                        Files.deleteIfExists(outf)
                    }
                }

                sem.acquire(MAX_PARALLEL)
                sem.release(MAX_PARALLEL)

                Files.newBufferedWriter(ds_results).use {
                    for ((lid, rscores) in sims) {
                        for ((rid, sim) in rscores) {
                            it.appendln("${lid}\t${rid}\t${sim}")
                        }
                    }
                }

                println("Finished $base")
            }

            println()
        }
    }
}