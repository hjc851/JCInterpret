package jcinterpret.testconsole.pipeline

import jcinterpret.testconsole.pipeline.comparison.ProcessedProjectComparator
import jcinterpret.testconsole.utils.BestMatchFinder
import jcinterpret.testconsole.utils.ProjectModelBuilder
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

fun main(args: Array<String>) {
    BestMatchFinder.MATCH_THRESHOLD = 0.1

    val root = Paths.get(args[0])

    val projects = Files.list(root)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }
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

    println()

    val keys = sims.keys.sorted()

    for (l in 0 until keys.size) {
        val lkey = keys[l]
        val scores = sims[lkey]!!

        for (r in 0 until keys.size) {
            val rkey = keys[r]

            val id = "$lkey-$rkey"
            if (l == r) {
                println("Point3D(\"$id\", $l.0, $r.0, 1.0),")
            } else if (scores.containsKey(rkey)) {
                val sim = scores[rkey]!!

                println("Point3D(\"$id\", $l.0, $r.0, ${String.format("%.2f", sim)}),")
            }
        }
    }

    println()

    print("arrayOf(")
    for (index in 0 until keys.size) {
        print("\"${keys[index]}\"")
        if (index < keys.size-1)
            print(", ")
    }
    println(")")
    println()

    println("arrayOf(")
    for (l in 0 until keys.size) {
        val lkey = keys[l]
        val scores = sims[lkey]!!

        print("arrayOf(")
        for (r in 0 until keys.size) {
            val rkey = keys[r]

            if (l == r) {
                print("0.00")
            } else {
                val sim = scores[rkey]!!
                print(String.format("%.2f", 1.0 - sim))
            }

            if (r < keys.size - 1)
                print(", ")
        }

        print(")")

        if (l < keys.size - 1)
            print(",")

        println()
    }
    println(")")

    println()
    println("Finished")
}
