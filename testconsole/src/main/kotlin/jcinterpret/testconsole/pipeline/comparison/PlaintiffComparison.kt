package jcinterpret.testconsole.pipeline.comparison

import jcinterpret.testconsole.utils.BestMatchFinder
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

fun main(args: Array<String>) {
    BestMatchFinder.MATCH_THRESHOLD = 0.1

    val root = Paths.get(args[0])
    val outDir = Paths.get(args[1])
    val plaintiff = args[2]

    if (!Files.exists(outDir)) Files.createDirectories(outDir)

    val projects = Files.list(root)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }

    val plaintiffProj = projects.first { it.fileName.toString() == plaintiff }
        .let(::buildProjectModel)

    val defendents = projects.filter { it.fileName.toString() != plaintiff }
        .map(::buildProjectModel)

    for (defendent in defendents) {
        val rid = defendent.root.fileName.toString()

        println("Comparing $plaintiff vs $rid")
        val result = ProcessedProjectComparator.compare(plaintiffProj, defendent)

        val (
            lhs, rhs,
            lminsim, lavgsim, lmaxsim,
            rminsim, ravgsim, rmaxsim
        ) = result

        println("LMIN:$lminsim")
        println("LAVG:$lavgsim")
        println("LMAX:$lmaxsim")

        println("RMIN:$rminsim")
        println("RAVG:$ravgsim")
        println("RMAX:$rmaxsim")

        println()
    }
}




