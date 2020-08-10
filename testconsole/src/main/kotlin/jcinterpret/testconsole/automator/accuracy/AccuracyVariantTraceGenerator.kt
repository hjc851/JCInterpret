package jcinterpret.testconsole.automator.accuracy

import jcinterpret.document.ConfigDocument
import jcinterpret.testconsole.pipeline.TraceGenerator
import jcinterpret.testconsole.plagiariser.mapper
import jcinterpret.testconsole.utils.FileUtils
import jcinterpret.testconsole.utils.Forker
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Semaphore
import kotlin.streams.toList

object AccuracyVariantTraceGenerator {
    val inp = Paths.get("/media/haydencheers/Data/SymbExec/variant_src")
    val out = Paths.get("/media/haydencheers/Data/SymbExec/variant_trace")

    val libs = Files.list(Paths.get("/media/haydencheers/Data/SymbExec/lib"))
        .filter { Files.isRegularFile(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".jar") }
        .use { it.toList() }

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

    val T_CHANCES = arrayOf(20, 40, 60, 80, 100)

    @JvmStatic
    fun main(args: Array<String>) {
        val MAX_PARALLEL = 32
        val sem = Semaphore(MAX_PARALLEL)

        val lib_names = libs.map { "lib/${it.fileName}" }

        for (ds in ds_names) {
            val ds_root = inp.resolve(ds)
            val ds_out = out.resolve(ds)

            if (!Files.exists(ds_root)) {
                System.err.println("DS ${ds} does not exist")
                continue
            }

            val bases = Files.list(ds_root)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .use { it.toList() }

            for (base_root in bases) {
                val base_id = base_root.fileName.toString()
                val base_out = ds_out.resolve(base_id)

                if (!Files.exists(base_out))
                    Files.createDirectories(base_out)

                // Levels
                for (l in 1 .. 5) {
                    val level_root = base_root.resolve("L$l")

                    if (!Files.exists(level_root)) {
                        System.err.println("DS ${ds} ${base_id} L${l} does not exist")
                        continue
                    }

                    for (tchance in T_CHANCES) {
                        val chance_root = level_root.resolve("T${tchance}")

                        if (!Files.exists(level_root)) {
                            System.err.println("DS ${ds} ${base_id} L${l} T${tchance} does not exist")
                            continue
                        }

                        for (vid in 1 .. 10) {
                            val variant_src = chance_root.resolve("V${vid}")
                            val variant_out = base_out.resolve("T${tchance}-L${l}-${vid}")

                            if (!Files.exists(level_root)) {
                                System.err.println("DS ${ds} ${base_id} L${l} T${tchance} V${vid} does not exist")
                                continue
                            }

                            if (Files.exists(variant_out)) {
                                continue
                            }

                            // Execute trace generator
                            val work = Files.createTempDirectory("jcinterpret_work")
                            val src = Files.createDirectory(work.resolve("src"))
                            val libdir = Files.createDirectories(work.resolve("lib"))
                            val out = Files.createDirectory(work.resolve("out"))

                            // Write the config file
                            val config = ConfigDocument(
                                "tmp",
                                "src",
                                "out",
                                lib_names,
                                null,
                                null,
                                false,
                                2,
                                3
                            )

                            val cfg = work.resolve("config.json")
                            Files.newBufferedWriter(cfg).use { writer ->
                                mapper.writeValue(writer, config)
                            }

                            // Copy over src
                            FileUtils.copyDir(variant_src, src.resolve(variant_src.fileName.toString()))

                            // Copy over libs
                            for (lib in libs) {
                                Files.copy(lib, libdir.resolve(lib.fileName.toString()))
                            }

                            // Spin off thread
                            sem.acquire()

                            CompletableFuture.runAsync {
                                // Execute
                                val res = Forker.exec(
                                    TraceGenerator::class.java,
                                    arrayOf(
                                        cfg.toAbsolutePath().toString()
                                    ),
                                    props = arrayOf(
                                        "-Xmx20G"
                                    )
                                )

                                if (res == 0) {
                                    println("Finished ${ds} ${base_id} L${l} T${tchance} V${vid}")

                                    // Copy over the trace
                                    val trace_out = out.resolve("src").resolve("V${vid}")

                                    if (!Files.exists(trace_out)) {
                                        System.err.println("No trace for ${ds} ${base_id} L${l} T${tchance} V${vid}")
                                    } else {
                                        FileUtils.copyDir(trace_out, variant_out)
                                    }

                                } else {
                                    System.err.println("Failed ${ds} ${base_id} L${l} T${tchance} V${vid}")
                                }

                            }.whenComplete { void, throwable ->
                                sem.release()
                                throwable?.printStackTrace(System.err)
                                FileUtils.deleteDirectory(work)
                            }
                        }
                    }
                }
            }
        }

        sem.acquire(MAX_PARALLEL)

        System.exit(0)
    }
}