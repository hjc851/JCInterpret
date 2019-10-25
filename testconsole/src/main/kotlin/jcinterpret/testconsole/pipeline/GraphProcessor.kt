package jcinterpret.testconsole.pipeline

import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.document.DocumentUtils
import jcinterpret.graph.analysis.concern.toGraph
import jcinterpret.testconsole.utils.ExecutionGraphCondenser
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
//        .filter { it.fileName.toString() == "11" }

    val condenser = ExecutionGraphCondenser(0.85)

    projects
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

            entryPointTraces
//                .drop(2)
                .forEach { eptraces ->
                    val epsig = eptraces.entryPoint.toString().replace("/", ".")
                        .replace("\"", ".")

                    val epout = pout.resolve(epsig)
                    if (Files.notExists(epout)) Files.createDirectory(epout)

                    println("\tBuilding trace models for $epsig (${eptraces.executionTraces.size} traces)")
                    val tm = (0 until eptraces.executionTraces.size).toList()
                        .parallelStream()
                        .map { index ->
                            TraceModelBuilder.build(id, index, eptraces.executionTraces[index])
                        }
                        .toList()

                    println("Condensing ...")
                    val models = tm.let { condenser.condenseTraces(it) }

                    println("\t\tWriting graphs & data")
                    (0 until models.size).toList()
                        .parallelStream()
                        .forEach { index ->
                            println("\t\t\t${index+1} of ${models.size}")
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

                    println("\t\tFinished writing data")
                }

            System.gc()
        }

    println("Finished")
    System.exit(0)
}