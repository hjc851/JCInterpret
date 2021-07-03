package jcinterpret.testconsole.thesischpt11

import jcinterpret.testconsole.pipeline.SingleGraphProcessor
import jcinterpret.testconsole.utils.Forker
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.streams.toList

object BPlagGraphProcessor {
    val trace_root = Paths.get("/home/haydencheers/traces")
    val graph_root = Paths.get("/media/haydencheers/Data/ThesisChpt11/graphs")

    val MAX_PARALLEL = 4
    val sem = Semaphore(MAX_PARALLEL)
    val pool = Executors.newFixedThreadPool(MAX_PARALLEL)

    @JvmStatic
    fun main(args: Array<String>) {
        val traces = Files.list(trace_root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }
            .sorted()

        for (trace in traces) {
            val outd = graph_root.resolve(trace.fileName)
            if (Files.exists(outd)) continue

            sem.acquire()
            CompletableFuture.runAsync(Runnable {
                val res = Forker.exec(
                    SingleGraphProcessor::class.java,
                    arrayOf(
                        trace.toAbsolutePath().toString(),
                        outd.toAbsolutePath().toString()
                    ),
                    arrayOf(
                        "-Xmx16G",
                        "-Djava.util.concurrent.ForkJoinPool.common.parallelism=16"
                    ),
                    waitFor = 10
                )

                if (res == 0) {
                    println("Finished ${trace.fileName}")
                } else {
                    System.err.println("Failed: ${trace.fileName}")

                    if (Files.exists(outd))
                        Files.walk(outd)
                            .sorted(Comparator.reverseOrder())
                            .forEach(Files::delete)
                }
            }, pool).whenComplete { void, throwable ->
                throwable?.printStackTrace()
                sem.release()
            }


        }
    }
}