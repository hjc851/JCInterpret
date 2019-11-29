package jcinterpret.testconsole.pipeline

import jcinterpret.testconsole.pipeline.comparison.ProcessedProjectComparator
import jcinterpret.testconsole.utils.ProjectModelBuilder
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

fun main(args: Array<String>) {
    ProcessedProjectComparator.TAINT_MATCH_THRESHOLD = 0.8

    val root = Paths.get(args[0])

    val projects = Files.list(root)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }
        .sortedBy { it.fileName.toString() }
        .map(ProjectModelBuilder::build)

    val sims = mutableMapOf<String, MutableMap<String, Double>>()

    for (l in 0 until projects.size) {
        val lhs = projects[l]
        val lid = lhs.projectId

        for (r in l+1 until projects.size) {
            val rhs = projects[r]
            val rid = rhs.projectId

            println("Comparing $lid vs $rid")
            val result = ProcessedProjectComparator.compare(lhs, rhs)

            val (
                _lhs, _rhs,
                lsim, rsim
            ) = result

            println("LSIM: $lsim")
            println("RSIM: $rsim")

            sims.getOrPut(lid) { mutableMapOf() }.put(rid, lsim)
            sims.getOrPut(rid) { mutableMapOf() }.put(lid, rsim)

            println()
        }
    }

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
