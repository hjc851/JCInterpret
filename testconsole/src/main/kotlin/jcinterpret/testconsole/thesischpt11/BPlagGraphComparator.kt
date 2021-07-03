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

object BPlagGraphComparator {

    val root = Paths.get("/media/haydencheers/Data/ThesisChpt11/SENG2050_A1_2018")
    val graph_root = Paths.get("/media/haydencheers/Data/ThesisChpt11/graphs")
    val score_out = Paths.get("/media/haydencheers/Data/ThesisChpt11/bplag-scores")

    val MAX_PARALLEL = 8
    val sem = Semaphore(MAX_PARALLEL)

    val mapper = ObjectMapper()
        .registerModule(KotlinModule())

    @JvmStatic
    fun main(args: Array<String>) {
        val roots = Files.list(root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .map { it.fileName.toString() }
            .use { it.toList() }

        val graphs = Files.list(graph_root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }

        val groups = roots.map { root -> root to  graphs.filter { it.fileName.toString().startsWith(root) } }

        for ((base_name, group) in groups) {
            val base_out_f = score_out.resolve("${base_name}.txt")
            if (Files.exists(base_out_f)) continue

            val base_out = Files.newBufferedWriter(base_out_f)

            val base_path = group.single { it.fileName.toString() == base_name }
            val variants = group.sorted()

            for (variant in variants) {
                val outf = Files.createTempFile("_sp", ".json")

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
                        ),
                        waitFor = 10
                    )

                    val lid = base_path.fileName.toString()
                    val rid = variant.fileName.toString()

                    if (res == 0) {
                        val result = Files.newBufferedReader(outf).use { reader ->
                            mapper.readValue(reader, SingleProjectComparator.SingleComparisonResult::class.java)
                        }

                        val sim = min(result.lsim, result.rsim)
                        base_out.write("${lid}\t${rid}\t${sim}\n")

                        println("Finished ${lid} - ${rid}")

                    } else {
                        System.err.println("Failed ${lid} - ${rid}")
                    }

                }.whenComplete { void, throwable ->
                    throwable?.printStackTrace()
                    sem.release()
                }

            }

            sem.acquire(MAX_PARALLEL)
            sem.release(MAX_PARALLEL)

            base_out.close()
        }

        System.exit(0)
    }
}