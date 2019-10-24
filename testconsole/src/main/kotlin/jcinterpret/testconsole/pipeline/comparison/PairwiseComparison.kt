package jcinterpret.testconsole.pipeline.comparison

import jcinterpret.testconsole.utils.BestMatchFinder
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

fun main(args: Array<String>) {
    BestMatchFinder.MATCH_THRESHOLD = 0.1

    val root = Paths.get(args[0])
//    val outDir = Paths.get(args[1])
//
//    if (!Files.exists(outDir)) Files.createDirectories(outDir)

    val projects = Files.list(root)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }
        .map(::buildProjectModel)

    for (l in 0 until projects.size) {
        val lhs = projects[l]
        val lid = lhs.root.fileName.toString()

        for (r in l+1 until projects.size) {
            val rhs = projects[r]
            val rid = rhs.root.fileName.toString()

            println("Comparing $lid vs $rid")
            val result = ProcessedProjectComparator.compare(lhs, rhs)

            val (
                _lhs, _rhs,
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

    println("Finished")
}
