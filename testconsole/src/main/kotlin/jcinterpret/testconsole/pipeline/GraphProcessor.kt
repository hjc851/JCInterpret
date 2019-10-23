package jcinterpret.testconsole.pipeline

import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.document.DocumentUtils
import jcinterpret.graph.analysis.concern.toGraph
import jcinterpret.testconsole.utils.ExecutionTraceCondenser
import jcinterpret.testconsole.utils.TraceModelBuilder
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

fun main(args: Array<String>) {
    val root = Paths.get(args[0])
    val out = Paths.get(args[1])

    val projects = Files.list(root)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }

    val condenser = ExecutionTraceCondenser(0.85)

    projects.parallelStream()
        .forEach { project ->
            val id = project.fileName.toString()

            val pout = out.resolve(id)
            if (Files.notExists(pout)) Files.createDirectories(pout)

            println("Loading $id")
            val entryPointTraces = Files.list(project)
                .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser")}
                .use { it.toList() }
                .parallelStream()
                .map { DocumentUtils.readObject(it, EntryPointExecutionTraces::class) }
                .toList()

            entryPointTraces.parallelStream()
                .forEach { eptraces ->
                    val epsig = eptraces.entryPoint.toString().replace("/", ".")
                        .replace("\"", ".")

                    val epout = pout.resolve(epsig)
                    if (Files.notExists(epout) )Files.createDirectory(epout)

                    println("Building trace models for $epsig")
                    val models = eptraces.executionTraces
                        .mapIndexed { index, executionTrace ->
                            TraceModelBuilder.build(id, index, executionTrace)
                        }
                        .let { condenser.condenseTraces(it) }

                    println("Writing graphs & data")
                    (0 until models.size).toList()
                        .parallelStream()
                        .forEach { index ->
                            val traceModel = models[index]

                            val eg = traceModel.ex
                            val taint = traceModel.taint
                            val scs = traceModel.secondaryConcerns.toGraph()

                            val executionGraph = eg.graph
                            val heap = eg.heap
                            val assertions = eg.assertions

                            DocumentUtils.writeObject(epout.resolve("$index-execgraph.ser"), executionGraph)
                            DocumentUtils.writeObject(epout.resolve("$index-taint.ser"), taint.graph)
                            DocumentUtils.writeObject(epout.resolve("$index-scs.ser"), scs)
                            DocumentUtils.writeObject(epout.resolve("$index-heap.ser"), heap)
                            DocumentUtils.writeObject(epout.resolve("$index-assertions.ser"), assertions)
                        }
                }
        }

    println("Finished")
    System.exit(0)
}