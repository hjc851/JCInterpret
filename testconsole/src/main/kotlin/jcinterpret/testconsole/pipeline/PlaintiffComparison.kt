package jcinterpret.testconsole.pipeline

import jcinterpret.comparison.iterative.IterativeGraphComparator
import jcinterpret.document.DocumentUtils
import jcinterpret.testconsole.utils.BestMatchFinder
import jcinterpret.testconsole.utils.avg
import jcinterpret.testconsole.utils.max
import jcinterpret.testconsole.utils.min
import org.graphstream.graph.Graph
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.streams.toList

fun main(args: Array<String>) {
    val root = Paths.get(args[0])
    val outDir = Paths.get(args[1])
    val plaintiff = args[2]

    if (!Files.exists(outDir)) Files.createDirectories(outDir)

    val projects = Files.list(root)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }

    val plaintiffProj = projects.first { it.fileName.toString() == plaintiff }
        .let(::listProject)

    val defendents = projects.filter { it.fileName.toString() != plaintiff }
        .map(::listProject)

    for (defendent in defendents) {
        val id = defendent.root.fileName.toString()

//        val fout = outDir.resolve("${plaintiff}_${id}_${Date().time}.txt")
//        Files.createFile(fout)
//        val logg = PrintWriter(Files.newBufferedWriter(fout))
        val logg = PrintWriter(System.out)

        compareProjects(plaintiffProj, defendent, logg)

        logg.flush()
//        logg.close()
    }
}

fun compareProjects(lhs: ProjectModel, rhs: ProjectModel, logg: PrintWriter) {

    val lid = lhs.root.fileName.toString()
    val rid = rhs.root.fileName.toString()

    val lsize = lhs.traces
        .values
        .flatMap { it.keys }
        .size

    val rsize = rhs.traces
        .values
        .flatMap { it.keys }
        .size

    if (lsize == 0 || rsize == 0) return

    val minCosts = Array(lsize) { DoubleArray(rsize) }
    val maxCosts = Array(lsize) { DoubleArray(rsize) }
    val avgCosts = Array(lsize) { DoubleArray(rsize) }

    val permits = lsize * rsize
    val sem = Semaphore(permits)

    var lcounter = 0
    var rcounter = 0

    val ltracearr = mutableListOf<String>()//ArrayList<String>(lsize)
    val rtracearr = mutableListOf<String>()//ArrayList<String>(rsize)

    for ((lep, ltraces) in lhs.traces) {
        for (ltrace in ltraces) {
            val (lid, lcomponents) = ltrace
            val lindex = lcounter++
            ltracearr.add(lindex, lep.fileName.toString() + "-" + lid)

            val ltaintpath = lcomponents.first { it.fileName.toString() == "${lid}-taint.ser"}
            val lscpath = lcomponents.first { it.fileName.toString() == "${lid}-scs.ser"}

            val ltaint = DocumentUtils.readObject(ltaintpath, Graph::class)
            val lscs = DocumentUtils.readObject(lscpath, Graph::class)

            for ((rep, rtraces) in rhs.traces) {
                for (rtrace in rtraces) {
                    val (rid, rcomponents) = rtrace
                    val rindex = rcounter++
                    rtracearr.add(rindex, rep.fileName.toString() + "-" + rid)

                    val rtaintpath = rcomponents.first { it.fileName.toString() == "${rid}-taint.ser"}
                    val rscpath = rcomponents.first { it.fileName.toString() == "${rid}-scs.ser"}

                    val rtaint = DocumentUtils.readObject(rtaintpath, Graph::class)
                    val rscs = DocumentUtils.readObject(rscpath, Graph::class)

                    val taintsimf = IterativeGraphComparator.compareAsync(ltaint, rtaint)
                    val scsimf = IterativeGraphComparator.compareAsync(lscs, rscs)

                    sem.acquire()

                    taintsimf.thenCombine(scsimf) { taintsim, scsim ->
                        taintsim to scsim
                    }.thenAccept { (taintsim, scsim) ->
                        val minSim = (taintsim.min() + scsim.min()).div(2)
                        val maxSim = (taintsim.max() + scsim.max()).div(2)
                        val avgSim = (taintsim.avg() + scsim.avg()).div(2)

                        minCosts[lindex][rindex] = 1.0 - minSim
                        maxCosts[lindex][rindex] = 1.0 - maxSim
                        avgCosts[lindex][rindex] = 1.0 - avgSim

                        sem.release()
                    }
                }
            }
        }
    }

    do {
        println("Waiting on ${permits - sem.availablePermits()} of ${permits} permits ....")

    } while (!sem.tryAcquire(permits, 10, TimeUnit.SECONDS))

    println("Acquired ${permits} permits")

    val (bestLMinMatches, bestRMinMatches) = BestMatchFinder.bestMatches(
        ltracearr,
        rtracearr,
        minCosts
    )
    val (bestLMaxMatches, bestRMaxMatches) = BestMatchFinder.bestMatches(
        ltracearr,
        rtracearr,
        maxCosts
    )
    val (bestLAvgMatches, bestRAvgmatches) = BestMatchFinder.bestMatches(
        ltracearr,
        rtracearr,
        avgCosts
    )

    val lminsim = bestLMinMatches.map { it.third }
        .sum()
        .div(ltracearr.size)

    val lmaxsim = bestLMaxMatches.map { it.third }
        .sum()
        .div(ltracearr.size)

    val lavgsim = bestLAvgMatches.map { it.third }
        .sum()
        .div(ltracearr.size)

    val rminsim = bestRMinMatches.map { it.third }
        .sum()
        .div(rtracearr.size)

    val rmaxsim = bestRMaxMatches.map { it.third }
        .sum()
        .div(rtracearr.size)

    val ravgsim = bestRAvgmatches.map { it.third }
        .sum()
        .div(rtracearr.size)

    logg.println("TOTALSIM - ${lid} vs ${rid}")
    logg.println("LMIN:$lminsim")
    logg.println("LMAX:$lmaxsim")
    logg.println("LAVG:$lavgsim")
    logg.println("RMIN:$rminsim")
    logg.println("RMAX:$rmaxsim")
    logg.println("RAVG:$ravgsim")
    logg.println()

}

fun listProject(path: Path): ProjectModel {
    val eps = Files.list(path)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }

    val traces = eps.map { ep ->
        ep to Files.list(ep)
            .filter { Files.isRegularFile(it) && !Files.isHidden(it) }
            .use { it.toList() }
            .groupBy { it.fileName.toString().split("-")[0] }
    }.toMap()

    return ProjectModel(
        path,
        eps,
        traces
    )
}

data class ProjectModel (
    val root: Path,
    val entryPoints: List<Path>,
    val traces: Map<Path, Map<String, List<Path>>>
)