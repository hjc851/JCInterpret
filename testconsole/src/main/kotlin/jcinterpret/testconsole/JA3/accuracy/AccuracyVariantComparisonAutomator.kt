package jcinterpret.testconsole.JA3.accuracy

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jcinterpret.testconsole.JA3.comparator.NoiseFilteredSingleProjectComparator
import jcinterpret.testconsole.JA3.comparator.SingleProjectComparator
import jcinterpret.testconsole.utils.Forker
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import kotlin.streams.toList

object AccuracyVariantComparisonAutomator {
    val variant_graphs = Paths.get("/media/haydencheers/Big Data/SymbExec/variant_graph")
    val base_graphs = Paths.get("/media/haydencheers/Big Data/SymbExec/graphs")

    var results_out = Paths.get("/media/haydencheers/Big Data/SymbExec/variant_results_nonoise_subsetcoeffic")

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
        val MAX_PARALLEL = 8
        val sem = Semaphore(MAX_PARALLEL)

        val mapper = ObjectMapper()
            .registerModule(KotlinModule())

        for (ds in ds_names) {
            val ds_variant_root = variant_graphs.resolve(ds)
            val ds_base_root = base_graphs.resolve(ds)
            val ds_results = results_out.resolve("$ds.txt")

            if (!Files.exists(ds_variant_root)) {
                System.err.println("DS ${ds} variants not found")
                continue
            }

            if (!Files.exists(ds_base_root)) {
                System.err.println("DS ${ds} bases not found")
                continue
            }

            val sims = ConcurrentHashMap<String, ConcurrentHashMap<String, Double>>()

            val variant_bases = Files.list(ds_variant_root)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .use { it.toList() }

            for (variant_base in variant_bases) {
                val base_id = variant_base.fileName.toString()
                val base_path = ds_base_root.resolve(base_id)

                if (!Files.exists(base_path)) {
                    System.err.println("Cannot find ${ds} ${base_id}")
                    continue
                }

                val variants = Files.list(variant_base)
                    .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                    .use { it.toList() }

                for (variant in variants) {
                    val outf = Files.createTempFile("jcinterpret_variant_comp_tmp", ".json")

                    sem.acquire()
                    CompletableFuture.runAsync {
                        val res = Forker.exec(
                            NoiseFilteredSingleProjectComparator::class.java,
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

                        val lid = base_id
                        val rid = base_id + "-" + variant.fileName.toString()

                        if (res == 0) {
                            val result = Files.newBufferedReader(outf).use { reader ->
                                mapper.readValue(reader, NoiseFilteredSingleProjectComparator.SingleComparisonResult::class.java)
                            }

                            sims.getOrPut(lid) { ConcurrentHashMap() }.put(rid, result.lsim)
                            sims.getOrPut(rid) { ConcurrentHashMap() }.put(lid, result.rsim)

                            println("Finished $ds ${lid} - ${rid}")

                        } else {
                            System.err.println("Failed $ds ${lid} - ${rid}")
                        }

                    }.whenComplete { void, throwable ->
                        throwable?.printStackTrace(System.err)
                        sem.release()
                        Files.deleteIfExists(outf)
                    }
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

            println("Finished $ds")
        }

        System.exit(0)
    }
}