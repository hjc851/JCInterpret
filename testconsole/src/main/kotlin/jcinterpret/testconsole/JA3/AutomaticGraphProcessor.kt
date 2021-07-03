package jcinterpret.testconsole.JA3

import jcinterpret.testconsole.pipeline.SingleGraphProcessor
import jcinterpret.testconsole.utils.Forker
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Semaphore
import kotlin.streams.toList

object AutomaticGraphProcessor {
    @JvmStatic
    fun main(args: Array<String>) {
        val src = Paths.get("/media/haydencheers/Data/SymbExec/traces")
        val out = Paths.get("/media/haydencheers/Data/SymbExec/graphs")

        val dataSets = listOf(
//            "COMP2230_A1_2018",
//            "COMP2240_A1_2018",
//            "COMP2240_A2_2018",
//            "COMP2240_A3_2018",
//            "SENG1110_A1_2017",
//            "SENG1110_A2_2017"
//            ,
//            "SENG2050_A1_2017",
//            "SENG2050_A2_2017",
//            "SENG2050_A1_2018",
//            "SENG2050_A2_2018",
//            "SENG2050_A1_2019",
//            "SENG2050_A2_2019"

//            "Algorithms_20",
//            "Algorithms_40",
//            "Algorithms_60"

            "Collected_20",
            "Collected_40",
            "Collected_60"
        )

        val MAX_PARALLEL = 8
        val sem = Semaphore(MAX_PARALLEL)

        for (ds in dataSets) {
            val dsroot = src.resolve(ds)
            val projects = Files.list(dsroot)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .use { it.toList() }
                .sortedBy { it.fileName.toString() }

            println(ds)
            val start = Instant.now()

            for (project in projects) {
                val outd = out.resolve(ds).resolve(project.fileName.toString())

                if (Files.exists(outd)) continue

                sem.acquire()

                CompletableFuture.runAsync {
                    val res = Forker.exec(
                        SingleGraphProcessor::class.java,
                        arrayOf(
                            project.toAbsolutePath().toString(),
                            outd.toAbsolutePath().toString()
                        ),
                        arrayOf(
                            "-Xmx20G",
                            "-Djava.util.concurrent.ForkJoinPool.common.parallelism=8"
                        )
                    )

                    if (res != 0) {
                        System.err.println("Failed: ${project.fileName}")

                        if (Files.exists(outd))
                            Files.walk(outd)
                                .sorted(Comparator.reverseOrder())
                                .forEach(Files::delete)
                    }
                }.whenComplete { void, throwable ->
                    sem.release()
                    throwable?.printStackTrace()
                }
            }

            sem.acquire(MAX_PARALLEL)
            sem.release(MAX_PARALLEL)

            val finish = Instant.now()
            val elapsed = Duration.between(start, finish)
            println("Elapsed: ${elapsed.seconds}s")

            println()
            println("--------------------------------------")
            println()
        }

        System.exit(0)
    }
}