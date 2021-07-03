package jcinterpret.testconsole.thesischpt11

import jcinterpret.document.ConfigDocument
import jcinterpret.testconsole.pipeline.TraceGenerator
import jcinterpret.testconsole.plagiariser.mapper
import jcinterpret.testconsole.utils.FileUtils
import jcinterpret.testconsole.utils.Forker
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.streams.toList

object BPlagTraceExtractor {
    val base_root = Paths.get("/media/haydencheers/Data/ThesisChpt11/SENG2050_A1_2018")
    val variant_root = Paths.get("/media/haydencheers/Data/ThesisChpt11/variants")
    val lib_root = Paths.get("/media/haydencheers/Data/ThesisChpt11/lib")
    val lib_names = Files.list(lib_root)
        .filter { Files.isRegularFile(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".jar") }
        .map { "lib/" + it.fileName.toString() }
        .use { it.toList() }

    val trace_out = Paths.get("/home/haydencheers/traces")

    val MAX_PARALLEL = 16
    val sem = Semaphore(MAX_PARALLEL)
    val pool = Executors.newFixedThreadPool(MAX_PARALLEL)

    @JvmStatic
    fun main(args: Array<String>) {
        val bases = Files.list(base_root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }
            .sorted()

        for (base in bases) {
            val out = trace_out.resolve(base.fileName)
            if (!Files.exists(out)) {
                makeTrace(base, out)
            }
        }

        for (base in bases) {
            for (c in arrayOf(10, 20, 40, 60, 80, 100)) {
                for (l in 1 .. 5) {
                    val variant_count = (l * 10).div(2)

                    for (x in 1 .. variant_count) {
                        val variant = variant_root.resolve(base.fileName)
                            .resolve("C${c}")
                            .resolve("L${l}")
                            .resolve("V${x}")
                        val out = trace_out.resolve("${base.fileName}-C${c}-L${l}-V${x}")

                        if (Files.exists(variant) && !Files.exists(out)) {
                            makeTrace(variant, out)
                        }
                    }
                }
            }
        }

        sem.acquire(MAX_PARALLEL)
        System.exit(0)
    }

    private fun makeTrace(root: Path, save: Path) {
        val tmp = Files.createTempDirectory("_sp")
        val cfg = tmp.resolve("config.json")
        val src = Files.createDirectory(tmp.resolve("src"))
        val libs = (tmp.resolve("lib"))
        val out = Files.createDirectory(tmp.resolve("out"))

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

        mapper.writeValue(cfg.toFile(), config)

        FileUtils.copyDir(root, src.resolve(root.fileName))
        FileUtils.copyDir(lib_root, libs)

        sem.acquire()

        CompletableFuture.runAsync {
            val res = Forker.exec(
                    TraceGenerator::class.java,
                arrayOf(
                    cfg.toAbsolutePath().toString()
                ),
                props = arrayOf(
                    "-Xmx4G"
                )
            )

            if (res == 0) {
                println("Finished ${save.fileName}")

                // Copy over the trace
                val trace_out = out.resolve("src").resolve(root.fileName)

                if (!Files.exists(trace_out)) {
                    System.err.println("No trace for ${save.fileName}")
                } else {
                    FileUtils.copyDir(trace_out, save)
                }

            } else {
                System.err.println("Failed ${save.fileName}")
            }

        }.whenComplete { void, throwable ->
            throwable?.printStackTrace()
            sem.release()

            Files.walk(tmp)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        }
    }
}