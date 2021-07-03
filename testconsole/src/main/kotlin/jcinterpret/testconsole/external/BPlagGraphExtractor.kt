package jcinterpret.testconsole.external

import jcinterpret.core.ctx.frame.interpreted.sub
import jcinterpret.testconsole.pipeline.SingleGraphProcessor
import jcinterpret.testconsole.utils.FileUtils
import jcinterpret.testconsole.utils.Forker
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.streams.toList

object BPlagGraphExtractor {

    val traceRoot = Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/bplag/traces")
    val graphRoot = Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/bplag/graphs")

    val MAX_PARALLEL = 8
    val SEMAPHORE = Semaphore(MAX_PARALLEL)
    val POOL = Executors.newFixedThreadPool(MAX_PARALLEL)

    val X_MX_HEAP = "-Xmx16G"
    val D_STREAM_PARALLELISM = "-Djava.util.concurrent.ForkJoinPool.common.parallelism=8"

    @JvmStatic
    fun main(args: Array<String>) {

    val setRoots = Files.list(traceRoot)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .toList()

        for (setRoot in setRoots) {
            val setOut = graphRoot.resolve(setRoot.fileName)

            if (!Files.exists(setOut))
                Files.createDirectories(setOut)

            val submissions = Files.list(setRoot)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .toList()

            for (submission in submissions) {
                convertTracesToGraphs(submission, setOut.resolve(submission.fileName))
            }
        }

        SEMAPHORE.acquire(MAX_PARALLEL)

        println("Finished")
        System.exit(0)
    }

    private fun convertTracesToGraphs(src: Path, out: Path) {
        if (Files.exists(out))
            return

        val srcName = src.fileName.toString()

        SEMAPHORE.acquire()

        CompletableFuture.runAsync(Runnable {
            val res = Forker.exec(
                SingleGraphProcessor::class.java,
                arrayOf(
                    src.toAbsolutePath().toString(),
                    out.toAbsolutePath().toString()
                ),
                props = arrayOf(
                    X_MX_HEAP,
                    D_STREAM_PARALLELISM
                )
            )

            if (res != 0) {
                System.err.println("Failed ${srcName}")
                FileUtils.deleteDirectory(out)

            } else {
                println("Finished ${srcName}")
            }

        }, POOL).whenComplete { void, throwable ->
            throwable?.printStackTrace()

            SEMAPHORE.release()
        }
    }
}