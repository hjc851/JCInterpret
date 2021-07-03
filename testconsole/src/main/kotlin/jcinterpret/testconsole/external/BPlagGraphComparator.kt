package jcinterpret.testconsole.external

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jcinterpret.testconsole.JA3.comparator.NoiseFilteredSingleProjectComparator
import jcinterpret.testconsole.JA3.comparator.SingleProjectComparator
import jcinterpret.testconsole.utils.Forker
import java.io.File
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.*
import kotlin.streams.toList

object BPlagGraphComparator {
    val graphRoot = Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/bplag/graphs-allmethods")
    val scoreRoot = Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/bplag/scores-allmethods")
    
    val comparisons = listOf(
//        // Internal comparison of projects
//        Comparison.Internal(graphRoot.resolve("SENG2050_A1_2017")),
//        Comparison.Internal(graphRoot.resolve("SENG2050_A1_2018")),
//        Comparison.Internal(graphRoot.resolve("SENG2050_A1_2019")),
//        Comparison.Internal(graphRoot.resolve("SENG2050_A1_2020")),
        Comparison.Internal(graphRoot.resolve("SENG2050_A1_2021")),
//
//        Comparison.Internal(graphRoot.resolve("SENG2050_A2_2017")),
//        Comparison.Internal(graphRoot.resolve("SENG2050_A2_2018")),
//        Comparison.Internal(graphRoot.resolve("SENG2050_A2_2019")),
//        Comparison.Internal(graphRoot.resolve("SENG2050_A2_2020")),
        Comparison.Internal(graphRoot.resolve("SENG2050_A2_2021")),
//
//        Comparison.Internal(graphRoot.resolve("SENG1110_A1_2017")),
//        Comparison.Internal(graphRoot.resolve("SENG1110_A1_2020")),
//        Comparison.Internal(graphRoot.resolve("SENG1110_A1_2021")),
//
//        Comparison.Internal(graphRoot.resolve("SENG1110_A2_2017")),
//        Comparison.Internal(graphRoot.resolve("SENG1110_A2_2020")),
//        Comparison.Internal(graphRoot.resolve("SENG1110_A2_2021")),
//
//        // SENG2050 A1
//        Comparison.External(graphRoot.resolve("SENG2050_A1_2017"),graphRoot.resolve("SENG2050_A1_GITHUB")),
//        Comparison.External(graphRoot.resolve("SENG2050_A1_2018"),graphRoot.resolve("SENG2050_A1_GITHUB")),
//        Comparison.External(graphRoot.resolve("SENG2050_A1_2019"),graphRoot.resolve("SENG2050_A1_GITHUB")),
//        Comparison.External(graphRoot.resolve("SENG2050_A1_2020"),graphRoot.resolve("SENG2050_A1_GITHUB")),
        Comparison.External(graphRoot.resolve("SENG2050_A1_2021"),graphRoot.resolve("SENG2050_A1_GITHUB")),
//
//        // SENG2050 A2
//        Comparison.External(graphRoot.resolve("SENG2050_A2_2017"),graphRoot.resolve("SENG2050_A2_GITHUB")),
//        Comparison.External(graphRoot.resolve("SENG2050_A2_2018"),graphRoot.resolve("SENG2050_A2_GITHUB")),
//        Comparison.External(graphRoot.resolve("SENG2050_A2_2019"),graphRoot.resolve("SENG2050_A2_GITHUB")),
//        Comparison.External(graphRoot.resolve("SENG2050_A2_2020"),graphRoot.resolve("SENG2050_A2_GITHUB")),
        Comparison.External(graphRoot.resolve("SENG2050_A2_2021"),graphRoot.resolve("SENG2050_A2_GITHUB"))
//
//        // SENG1110 A1
//        Comparison.External(graphRoot.resolve("SENG1110_A1_2017"),graphRoot.resolve("SENG1110_A1_GITHUB")),
//        Comparison.External(graphRoot.resolve("SENG1110_A1_2020"),graphRoot.resolve("SENG1110_A1_GITHUB")),
//        Comparison.External(graphRoot.resolve("SENG1110_A1_2021"),graphRoot.resolve("SENG1110_A1_GITHUB")),
//
//        // SENG1110 A2
//        Comparison.External(graphRoot.resolve("SENG1110_A2_2017"),graphRoot.resolve("SENG1110_A2_GITHUB")),
//        Comparison.External(graphRoot.resolve("SENG1110_A2_2020"),graphRoot.resolve("SENG1110_A2_GITHUB")),
//        Comparison.External(graphRoot.resolve("SENG1110_A2_2021"),graphRoot.resolve("SENG1110_A2_GITHUB"))
    )

    val MAX_PARALLEL = 32
    val SEMAPHORE = Semaphore(MAX_PARALLEL)
    val POOL = Executors.newFixedThreadPool(MAX_PARALLEL)

    val LATCH = CountDownLatch(comparisons.size)

    val X_MX_HEAP = "-Xmx4G"
    val D_STREAM_PARALLELISM = "-Djava.util.concurrent.ForkJoinPool.common.parallelism=2"

    val MAPPER = ObjectMapper().registerModule(KotlinModule())

    @JvmStatic
    fun main(args: Array<String>) {

        if (!Files.exists(scoreRoot))
            Files.createDirectories(scoreRoot)

        for (comparison in comparisons) {
            runComparison(comparison)
        }

        LATCH.await()

        println("Finished all comparisons")
        System.exit(0)
    }

    private fun runComparison(comparison: Comparison) {
        val name = comparison.name()

        val scoref = scoreRoot.resolve("${name}.txt")
        if (Files.exists(scoref)) {
            println("Found score file. Skipping $name.")
            LATCH.countDown()
            return
        }

        val pairs = comparison.buildComparisonPairs()

        val scores = Collections.synchronizedList(mutableListOf<Triple<String, String, String>>())
        val futures = mutableListOf<CompletableFuture<Void>>()

        for ((lhs, rhs) in pairs) {
            val outf = Files.createTempFile("bplag_comp_score_tmp", ".json")

            SEMAPHORE.acquire()

            val fut = CompletableFuture.runAsync(Runnable {
                val lname = lhs.fileName.toString()
                val rname = rhs.fileName.toString()

                try {
                    val res = Forker.exec(
                        SingleProjectComparator::class.java,
                        arrayOf(
                            lhs.toAbsolutePath().toString(),
                            rhs.toAbsolutePath().toString(),
                            outf.toAbsolutePath().toString()
                        ),
                        arrayOf(
                            X_MX_HEAP,
                            D_STREAM_PARALLELISM
                        )
                    )

                    if (res == 0) {
                        val result = Files.newBufferedReader(outf).use { reader ->
                            MAPPER.readValue(reader, SingleProjectComparator.SingleComparisonResult::class.java)
                        }

                        scores.add(Triple(lname, rname, result.lsim.toString()))
                        scores.add(Triple(rname, rname, result.rsim.toString()))

                    } else {
                        scores.add(Triple(lname, rname, "FF"))
                        scores.add(Triple(rname, rname, "FF"))
                    }
                } catch (e: TimeoutException) {
                    scores.add(Triple(lname, rname, "DNF"))
                    scores.add(Triple(rname, rname, "DNF"))
                }

            }, POOL).whenComplete { _, throwable ->
                throwable?.printStackTrace()
                SEMAPHORE.release()

                try { Files.deleteIfExists(outf) }
                catch (e: Exception) { }
            }

            futures.add(fut)
        }

        CompletableFuture.allOf(*futures.toTypedArray()).whenComplete { _, throwable ->
            throwable?.printStackTrace()

            try {
                Files.newBufferedWriter(scoref).use { writer ->
                    scores.forEach { (lhs, rhs, result) ->
                        writer.write("${lhs}\t${rhs}\t${result}\n")
                    }
                }
            } catch (e: Exception) {
                System.err.println("Failed ${name}")
                e.printStackTrace()
            }

        }.whenComplete { _, throwable ->
            throwable?.printStackTrace()

            if (throwable == null) {
                println("Finished ${name}")
            }

            LATCH.countDown()
        }
    }
}

interface Comparison {

    fun name(): String
    fun buildComparisonPairs(): List<Pair<Path, Path>>

    data class Internal(val root: Path): Comparison {
        override fun name(): String {
            return root.fileName.toString()
        }

        override fun buildComparisonPairs(): List<Pair<Path, Path>> {
            val pairs = mutableListOf<Pair<Path, Path>>()

            val projs = Files.list(root)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .toList()

            for (l in projs.indices) {
                for (r in l+1 until projs.size) {
                    pairs.add(projs[l] to projs[r])
                }
            }

            return pairs
        }
    }
    
    data class External(val leftRoot: Path, val rightRoot: Path): Comparison {
        override fun name(): String {
            return "${leftRoot.fileName} - ${rightRoot.fileName}"
        }

        override fun buildComparisonPairs(): List<Pair<Path, Path>> {
            val pairs = mutableListOf<Pair<Path, Path>>()

            val lhs = Files.list(leftRoot)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .toList()

            val rhs = Files.list(rightRoot)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .toList()

            for (l in lhs) {
                for (r in rhs) {
                    pairs.add(l to r)
                }
            }

            return pairs
        }
    }
}