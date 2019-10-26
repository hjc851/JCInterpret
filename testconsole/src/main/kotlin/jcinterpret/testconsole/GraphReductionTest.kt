package jcinterpret.testconsole

import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.document.DocumentUtils
import jcinterpret.testconsole.utils.ExecutionGraphCondenser
import jcinterpret.testconsole.utils.TraceModelBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import kotlin.streams.toList

fun main(args: Array<String>) {
    val root = Paths.get(args[0])

    val projects = Files.list(root)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }

    val condenser = ExecutionGraphCondenser(0.9)

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
                TraceModelBuilder.build(
                    id,
                    index,
                    executionTrace
                )
            } }
            .map { it.get() }
            .let { condenser.condenseTraces(it) }

        System.gc()
    }
}