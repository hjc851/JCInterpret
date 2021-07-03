package jcinterpret.testconsole.JA3.accuracy

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jcinterpret.testconsole.JA3.comparator.NoiseFilteredSingleProjectComparator
import jcinterpret.testconsole.JA3.comparator.SingleProjectComparator
import jcinterpret.testconsole.utils.Forker
import jcinterpret.testconsole.utils.ProjectModelBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.IntStream
import kotlin.streams.toList

object PairwiseComparisonAutomator {
    @JvmStatic
    fun main(args: Array<String>) {
        val graphs = Paths.get("/media/haydencheers/Big Data/SymbExec/graphs")

        val dataSets = listOf(
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

        for (ds in dataSets) {
            val graphRoot = graphs.resolve(ds)
            val results = Paths.get("/media/haydencheers/Big Data/SymbExec/results-nonoise-subsetcoef-pairwise/${ds}.txt")

            if (Files.exists(results)) continue
            if (!Files.exists(results.parent)) Files.createDirectories(results.parent)

            val start = Instant.now()

            val scores =
                doComparison(
                    graphRoot
                )

            val end = Instant.now()
            val elapsed = Duration.between(start, end)

            Files.newBufferedWriter(results).use {
                for ((lid, rscores) in scores) {
                    for ((rid, sim) in rscores) {
                        it.appendln("${lid}\t${rid}\t${sim}")
                    }
                }
            }

            println(ds)
            println("${elapsed.seconds} (s)")
            println()
        }

        System.exit(0)
    }

    fun doComparison(root: Path): Map<String, Map<String, Double>> {
        val mapper = ObjectMapper().registerModule(KotlinModule())

        val projects = Files.list(root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }
            .sortedBy { it.fileName.toString() }
            .map(ProjectModelBuilder::build)

        val sims = ConcurrentHashMap<String, ConcurrentHashMap<String, Double>>()

        val MAX_PARALLEL = 8
        val sem = Semaphore(MAX_PARALLEL)

        var counter = AtomicInteger(0)
        val max = (projects.size) * (projects.size-1) / 2

        IntStream.range(0, projects.size).forEach { l ->
            val lhs = projects[l]
            val lid = lhs.projectId

            IntStream.range(l+1, projects.size).forEach { r ->
                val rhs = projects[r]
                val rid = rhs.projectId

                val outf = Files.createTempFile("JCInterpret_pairwise_tmp", ".json")
                val id = counter.incrementAndGet()

                sem.acquire()
                CompletableFuture.runAsync {
                    val res = Forker.exec(
                        NoiseFilteredSingleProjectComparator::class.java,
                        arrayOf(
                            lhs.rootPath.toAbsolutePath().toString(),
                            rhs.rootPath.toAbsolutePath().toString(),
                            outf.toAbsolutePath().toString()
                        ),
                        arrayOf(
                            "-Xmx20G",
                            "-Djava.util.concurrent.ForkJoinPool.common.parallelism=8"
                        )
                    )

                    if (res == 0) {
                        val result = Files.newBufferedReader(outf).use { reader ->
                            mapper.readValue(reader, NoiseFilteredSingleProjectComparator.SingleComparisonResult::class.java)
                        }

                        sims.getOrPut(lid) { ConcurrentHashMap() }.put(rid, result.lsim)
                        sims.getOrPut(rid) { ConcurrentHashMap() }.put(lid, result.rsim)

                        println("Finished ${lid} - ${rid} ($id of $max)")

                    } else {
                        System.err.println("Failed executing ${lid} - ${rid} ($id of $max)")
                    }

                }.whenComplete { void, throwable ->
                    sem.release()

                    throwable?.printStackTrace(System.err)

                    try { Files.deleteIfExists(outf) }
                    catch (e: Exception) { }
                }
            }
        }

        do {
            println("\t${Date()} Awaiting ${MAX_PARALLEL-sem.availablePermits()} permits")
        } while (!sem.tryAcquire(MAX_PARALLEL, 15, TimeUnit.SECONDS))

        return sims
    }
}


