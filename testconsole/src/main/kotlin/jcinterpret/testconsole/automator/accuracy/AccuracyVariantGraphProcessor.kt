package jcinterpret.testconsole.automator.accuracy

import jcinterpret.testconsole.pipeline.SingleGraphProcessor
import jcinterpret.testconsole.utils.FileUtils
import jcinterpret.testconsole.utils.Forker
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Semaphore
import kotlin.streams.toList

object AccuracyVariantGraphProcessor {
    val trace_root = Paths.get("/media/haydencheers/Data/SymbExec/variant_trace")
    val graph_root = Paths.get("/media/haydencheers/Data/SymbExec/variant_graph")

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

        for (ds in ds_names) {
            val ds_trace_root = trace_root.resolve(ds)
            val ds_graph_out = graph_root.resolve(ds)

            if (!Files.exists(ds_trace_root)) {
                System.err.println("DS ${ds} does not exist")
                continue
            }

            if (!Files.exists(ds_graph_out)) Files.createDirectories(ds_graph_out)

            val bases = Files.list(ds_trace_root)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .use { it.toList() }

            for (base in bases) {
                val base_graph_out = ds_graph_out.resolve(base.fileName.toString())

                if (!Files.exists(base_graph_out)) Files.createDirectories(base_graph_out)

                val variant_traces = Files.list(base)
                    .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                    .use { it.toList() }

                for (variant in variant_traces) {
                    val variant_graph_out = base_graph_out.resolve(variant.fileName.toString())

                    if (Files.exists(variant_graph_out)) continue

                    sem.acquire()

                    CompletableFuture.runAsync {
                        val res = Forker.exec(
                            SingleGraphProcessor::class.java,
                            arrayOf(
                                variant.toAbsolutePath().toString(),
                                variant_graph_out.toAbsolutePath().toString()
                            ),
                            props = arrayOf(
                                "-Xmx20G",
                                "-Djava.util.concurrent.ForkJoinPool.common.parallelism=8"
                            )
                        )

                        if (res != 0) {
                            System.err.println("Failed ${ds} ${base.fileName} ${variant.fileName}")
                            FileUtils.deleteDirectory(variant_graph_out)

                        } else {
                            println("Finished ${ds} ${base.fileName} ${variant.fileName}")
                        }

                    }.whenComplete { void, throwable ->
                        sem.release()
                        throwable?.printStackTrace(System.err)
                    }
                }
            }
        }

        sem.acquire(MAX_PARALLEL)
        System.exit(0)
    }
}