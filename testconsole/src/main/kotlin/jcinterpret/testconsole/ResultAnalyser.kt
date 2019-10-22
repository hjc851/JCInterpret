package jcinterpret.testconsole

import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val src = Paths.get(args[0])

    val reader = Files.newBufferedReader(src)

    while (true) {
        val title = reader.readLine()
        if (title == "--END--") break

        val comps = title.split(":")
        val lhscomp = comps[0].split("-")
        val rhscomp = comps[1].split("-")

        val lid = lhscomp[0]
        val lcount = lhscomp[1]

        val rid = rhscomp[0]
        val rcount = rhscomp[1]

        println("$lid vs $rid ($lcount vs $rcount)")

        val compCount = reader.readLine().toInt()
        reader.readLine()
        for (i in 0 until compCount) {
            reader.readLine()
            reader.readLine()
            reader.readLine()
            reader.readLine()
            reader.readLine()
        }

        reader.readLine()

        reader.readLine()   // MINMATCH
        reader.readLine()   // L-R
        val lmincount = reader.readLine().toInt()
        for (i in 0 until lmincount) {
            reader.readLine()
        }

        reader.readLine()   // R-L
        val rmincount = reader.readLine().toInt()
        for (i in 0 until rmincount) {
            reader.readLine()
        }

        reader.readLine()   // MAXMATCH
        reader.readLine()   // L-R
        val lmaxcount = reader.readLine().toInt()
        for (i in 0 until lmaxcount) {
            reader.readLine()
        }

        reader.readLine()   // R-L
        val rmaxcount = reader.readLine().toInt()
        for (i in 0 until rmaxcount) {
            reader.readLine()
        }

        reader.readLine()   // AVGMATCH
        reader.readLine()   // L-R
        val lavgcount = reader.readLine().toInt()
        for (i in 0 until lavgcount) {
            reader.readLine()
        }

        reader.readLine()   // R-L
        val ravgcount = reader.readLine().toInt()
        for (i in 0 until ravgcount) {
            reader.readLine()
        }

        reader.readLine()   // TOTALSIM
        val lminsim = reader.readLine()
            .split(":")[1]
            .toDouble()

        val lmaxsim = reader.readLine()
            .split(":")[1]
            .toDouble()

        val lavgsim = reader.readLine()
            .split(":")[1]
            .toDouble()

        val rminsim = reader.readLine()
            .split(":")[1]
            .toDouble()

        val rmaxsim = reader.readLine()
            .split(":")[1]
            .toDouble()

        val ravgsim = reader.readLine()
            .split(":")[1]
            .toDouble()

        println("LMIN $lminsim")
        println("RMIN $rminsim")
        println("LMAX $lmaxsim")
        println("RMAX $rmaxsim")
        println("LAVG $lavgsim")
        println("RAVG $ravgsim")

        reader.readLine()
    }


}