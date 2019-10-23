package jcinterpret.testconsole

import jcinterpret.comparison.iterative.IterativeGraphComparator
import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.document.DocumentUtils
import jcinterpret.testconsole.utils.TraceModel
import jcinterpret.testconsole.utils.avg
import jcinterpret.testconsole.utils.buildTraceModel
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import kotlin.streams.toList

fun main(args: Array<String>) {
    val root = Paths.get(args[0])

    val projects = Files.list(root)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }

    for (project in projects) {
        val id = project.fileName.toString()

        println("Loading $id")
        val traces = Files.list(project)
            .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser")}
            .use { it.toList() }
            .parallelStream()
            .map { DocumentUtils.readObject(it, EntryPointExecutionTraces::class) }
            .toList()
            .flatMap { it.executionTraces.toList() }
            .mapIndexed { index, executionTrace -> CompletableFuture.supplyAsync {
                buildTraceModel(
                    id,
                    index,
                    executionTrace
                )
            } }
            .map { it.get() }
            .let { condenseTraces(it) }

        System.gc()
    }
}

val T_CONDENSE_THESHOLD = 0.9

fun condenseTraces(traces: List<TraceModel>): List<TraceModel> {
    println("Condensing ${traces.size} ...")

    val reducedTraces = traces.toMutableList()

    var l = 0
    while (l < reducedTraces.size) {
        val lhs = reducedTraces[l]
        val riter = reducedTraces.iterator()

        while (riter.hasNext()) {
            val rhs = riter.next()

            if (System.identityHashCode(lhs) != System.identityHashCode(rhs)) {
                val tsim = IterativeGraphComparator.compare(lhs.ex.graph, rhs.ex.graph)
                if (tsim.avg() >= T_CONDENSE_THESHOLD)
                    riter.remove()
            }
        }

        l++
    }

    println("Removed ${traces.size-reducedTraces.size} traces, leaving ${reducedTraces.size}")
    return reducedTraces
}