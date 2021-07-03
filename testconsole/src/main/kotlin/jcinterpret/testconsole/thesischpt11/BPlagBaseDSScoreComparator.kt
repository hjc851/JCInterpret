package jcinterpret.testconsole.thesischpt11

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jcinterpret.testconsole.JA3.comparator.SingleProjectComparator
import jcinterpret.testconsole.utils.Forker
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Semaphore
import kotlin.math.min
import kotlin.streams.toList

object BPlagBaseDSScoreComparator {
    val graph_root = Paths.get("/media/haydencheers/Data/ThesisChpt11/graphs")
    val out_f = Paths.get("/media/haydencheers/Data/ThesisChpt11/bplag-base.txt")
    val src_root = Paths.get("/media/haydencheers/Data/ThesisChpt11/SENG2050_A1_2018")

    val MAX_PARALLEL = 8
    val sem = Semaphore(MAX_PARALLEL)

    val mapper = ObjectMapper()
        .registerModule(KotlinModule())

    @JvmStatic
    fun main(args: Array<String>) {
        val base_names = Files.list(src_root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .map { it.fileName.toString() }
            .use { it.toList() }

        val base_graphs = Files.list(graph_root)
            .filter { base_names.contains(it.fileName.toString()) }
            .use { it.toList() }

        val scores = mutableListOf<Triple<String, String, Double>>()

        for (l in 0 until base_graphs.size) {
            val lproj = base_graphs[l]

            for (r in (l+1) until base_graphs.size) {
                val rproj = base_graphs[r]

                sem.acquire()
                CompletableFuture.runAsync {
                    val tmpf = Files.createTempFile("_sp", ".json")

                    val res = Forker.exec(
                        SingleProjectComparator::class.java,
                        arrayOf(
                            lproj.toAbsolutePath().toString(),
                            rproj.toAbsolutePath().toString(),
                            tmpf.toAbsolutePath().toString()
                        ),
                        props = arrayOf(
                            "-Xmx15G",
                            "-Djava.util.concurrent.ForkJoinPool.common.parallelism=8"
                        ),
                        waitFor = 10
                    )

                    if (res == 0) {
                        val result = Files.newBufferedReader(tmpf).use { reader ->
                            BPlagGraphComparator.mapper.readValue(reader, SingleProjectComparator.SingleComparisonResult::class.java)
                        }

                        val sim = min(result.lsim, result.rsim)
                        scores.add(Triple(lproj.fileName.toString(), rproj.fileName.toString(), sim))

                        println("Finished ${lproj.fileName} - ${rproj.fileName}")
                    } else {
                        System.err.println("Failed ${lproj.fileName} - ${rproj.fileName}")
                    }

                }.whenComplete { void, throwable ->
                    throwable?.printStackTrace()
                    sem.release()
                }
            }
        }

        sem.acquire(MAX_PARALLEL)
        sem.release(MAX_PARALLEL)

        Files.newBufferedWriter(out_f).use {
            for (score in scores) {
                it.write("${score.first}\t${score.second}\t${score.third}\n")
            }
        }

        System.exit(0)
    }
}