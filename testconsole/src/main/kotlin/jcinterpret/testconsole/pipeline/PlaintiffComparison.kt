package jcinterpret.testconsole.pipeline

import jcinterpret.testconsole.pipeline.comparison.NoSecondaryConcernsException
import jcinterpret.testconsole.pipeline.comparison.ProcessedProjectComparator
import jcinterpret.testconsole.utils.ProjectModelBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import kotlin.streams.toList

fun main(args: Array<String>) {
    val start = Instant.now()
    ProcessedProjectComparator.TAINT_MATCH_THRESHOLD = 0.8

    val root = Paths.get(args[0])
    val plaintiff = args[1]

    val projects = Files.list(root)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }
        .sortedBy { it.fileName.toString() }
        .map(ProjectModelBuilder::build)

    val sims = mutableMapOf<String, Double>()

    val lhs = projects.single { it.projectId == plaintiff }
    val defendents = projects.filterNot { it.projectId == plaintiff }

    if (lhs.entryPoints.isEmpty()) {
        println("No entry points in plaintiff. Exiting.")
        return
    }

    for (rhs in defendents) {
        val rid = rhs.projectId

        if (rhs.entryPoints.isEmpty()) {
            println("No entry points in defendent. Continuing ...")
            continue
        }

        println("Comparing $plaintiff vs $rid")
        val result = ProcessedProjectComparator.compare(lhs, rhs)

        val (
            _lhs, _rhs,
            lsim, rsim
        ) = result

        sims[rid] = (lsim + rsim) / 2.0

        println("LSIM: $lsim")
        println("RSIM: $rsim")

        println()
    }

    println("Results")
    val keys = sims.keys.sortedBy { it }

    for (key in keys) {
        val sim = sims[key]!!
        println("$key\t$sim")
    }

    val end = Instant.now()
    val elapsed = Duration.between(start, end)

    println()
    println("Elapsed time ${elapsed.seconds} (s)")
}




