package jcinterpret.testconsole.pipeline

import jcinterpret.testconsole.pipeline.comparison.NoSecondaryConcernsException
import jcinterpret.testconsole.pipeline.comparison.ProcessedProjectComparator
import jcinterpret.testconsole.utils.BestMatchFinder
import jcinterpret.testconsole.utils.ProjectModelBuilder
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

fun main(args: Array<String>) {
    BestMatchFinder.MATCH_THRESHOLD = 0.1

    val root = Paths.get(args[0])
    val plaintiff = args[1]

    val projects = Files.list(root)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }
        .map(ProjectModelBuilder::build)

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
        try {
            val result = ProcessedProjectComparator.compare(lhs, rhs)

            val (
                    _lhs, _rhs,
                    lsim, rsim
            ) = result

            println("LSIM: $lsim")
            println("RSIM: $rsim")
        } catch (e: NoSecondaryConcernsException) {
            println("No secondary concerns")
        }

        println()
    }
}




