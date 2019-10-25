package jcinterpret.testconsole.pipeline.comparison

import jcinterpret.testconsole.utils.BestMatchFinder
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
        .map(::buildProjectModel)

    val lhs = projects.single { it.root.fileName.toString() == plaintiff }
    val defendents = projects.filterNot { it.root.fileName.toString() == plaintiff }

    for (rhs in defendents) {
        val rid = rhs.root.fileName.toString()

        println("Comparing $plaintiff vs $rid")
        val result = ProcessedProjectComparator.compare(lhs, rhs)

//        val (
//            _lhs, _rhs,
//            lminsim, lavgsim, lmaxsim,
//            rminsim, ravgsim, rmaxsim
//        ) = result
//
//        println("LMIN:$lminsim")
//        println("LAVG:$lavgsim")
//        println("LMAX:$lmaxsim")
//
//        println("RMIN:$rminsim")
//        println("RAVG:$ravgsim")
//        println("RMAX:$rmaxsim")

        val (
            _lhs, _rhs,
            lsim, rsim
        ) = result

        println("LSIM: $lsim")
        println("RSIM: $rsim")

        println()
    }
}




