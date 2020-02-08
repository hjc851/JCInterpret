package jcinterpret.testconsole.pipeline

import jcinterpret.testconsole.pipeline.comparison.ProcessedProjectComparator
import jcinterpret.testconsole.utils.ProjectModel
import jcinterpret.testconsole.utils.ProjectModelBuilder
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executors
import java.util.stream.IntStream
import kotlin.streams.toList

val ROOT_POOL = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()/2)

fun main(args: Array<String>) {
    ProcessedProjectComparator.TAINT_MATCH_THRESHOLD = 0.8

    val root = Paths.get(args[0])

    val projects = Files.list(root)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }
        .sortedBy { it.fileName.toString() }
        .map(ProjectModelBuilder::build)

    val sims = ConcurrentHashMap<String, ConcurrentHashMap<String, Double>>()
    val futures = mutableListOf<CompletableFuture<Void>>()

    IntStream.range(0, projects.size).forEach { l ->
        val lhs = projects[l]
        val lid = lhs.projectId

        IntStream.range(l+1, projects.size).forEach { r ->
            val rhs = projects[r]
            val rid = rhs.projectId

            val f = CompletableFuture.runAsync (Runnable {
                val result = ProcessedProjectComparator.compare(lhs, rhs)

                val (
                    _lhs, _rhs,
                    lsim, rsim
                ) = result

                val out = StringBuilder()
                out.appendln("Comparing ${lid} vs ${rid}")
                out.appendln("LSIM: ${lsim}")
                out.appendln("RSIM: ${rsim}")
                println(out.toString())

                sims.getOrPut(lid) { ConcurrentHashMap() }.put(rid, lsim)
                sims.getOrPut(rid) { ConcurrentHashMap() }.put(lid, rsim)
            }, ROOT_POOL)

            futures.add(f)
        }
    }

    for (future in futures) future.get()

    val groupKeys = sims.keys
        .groupBy { it.split("_")[0] }

    for ((lgroupName, lkeySet) in groupKeys) {
        println(lgroupName)

        for ((rgroupName, rkeyset) in groupKeys) {
            val avg = lkeySet.map { sims[it]!! }
                .flatMap { lmap ->
                    rkeyset.map { rkey -> lmap[rkey] ?: 1.0 }
                }.average()
            println("$rgroupName $avg")
        }

        println()
    }

    println()
    println("Finished")
}
