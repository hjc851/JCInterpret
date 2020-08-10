package jcinterpret.testconsole.pipeline

import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.document.DocumentUtils
import jcinterpret.graph.analysis.concern.toGraph
import jcinterpret.graph.serialization.toSerializable
import jcinterpret.testconsole.utils.ExecutionGraphCondenser
import jcinterpret.testconsole.utils.TraceModelBuilder
import java.io.Serializable
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import kotlin.streams.toList

val condenser = ExecutionGraphCondenser(0.9)

data class GraphManifest (
    val projectId: String,
    val entryPoint: String,
    val graphWeights: Map<String, Int>
): Serializable

object GraphProcessor {
    fun main(args: Array<String>) {
        //  Get the args
        //  root: The root of the produced execution traces
        //  out: the path to store the decomposed graphs in
        val root = Paths.get(args[0])
        val out = Paths.get(args[1])

        val start = Instant.now()

        //  List all the projects in the root directory
        val projects = Files.list(root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }
            .sortedBy { it.fileName.toString() }

        //  Iterate through the projects and process one by one (parallel uses too much memory)
        for (project in projects) {
            processProject(project, out.resolve(project.fileName.toString()))

            // Force clear the models from the heap
            System.gc()
        }

        val finish = Instant.now()
        val elapsed = Duration.between(start, finish)

        println("Elapsed: ${elapsed.seconds}s")

        println("Finished")
        System.exit(0)
    }

    fun processProject(project: Path, out: Path) {
        val id = project.fileName.toString()

        //  The output directory will be ${root}/${id}
        val pout = out
        if (Files.notExists(pout)) Files.createDirectories(pout)

        //  List all of the execution traces for the project
        println("Loading $id")
        val entryPointTraceFiles = Files.list(project)
            .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser")}
            .use { it.toList() }

        if (entryPointTraceFiles.isEmpty()) return

        for (entryPointFile in entryPointTraceFiles) {
            //  Load the trace
            val eptraces = DocumentUtils.readObject(entryPointFile, EntryPointExecutionTraces::class)
            val epsig = eptraces.entryPoint.toString().replace("/", ".")
                .replace("\"", ".")

            // No traces means no graph
            if (eptraces.executionTraces.isEmpty()) continue

            // This trace will be stored in ${root}/${id}/${ep}
            val epout = pout.resolve(epsig)
            if (Files.notExists(epout)) Files.createDirectory(epout)

            //  Build the entry point traces (i.e. construct the graphs + process)
            println("\tBuilding trace models for $epsig (${eptraces.executionTraces.size} traces)")
            val traceModels = (0 until eptraces.executionTraces.size).toList()
                .parallelStream()
                .map { index ->
                    TraceModelBuilder.build(id, index, eptraces.executionTraces[index])
                }
                .use { it.toList() }

            // Condense the traces by similarity
            println("\t\tCondensing models")
            val models = traceModels.let { condenser.condenseTraces(it) }

            //  Exit if we have no models
            if (models.isEmpty()) continue

            // Save the manifest of graph weightings
            DocumentUtils.writeObject(
                epout.resolve("manifest.ser"),
                GraphManifest (
                    id,
                    eptraces.entryPoint.toString(),
                    models.mapIndexed { index, model -> index.toString() to model.weight }.toMap()
                )
            )

            // Write the traces to disk
            println("\tWriting graphs & data")
            (0 until models.size).toList()
                .parallelStream()
                .forEach { index ->
                    println("\t\t${index+1} of ${models.size}")
                    val weightedModel = models[index]
                    val traceModel = weightedModel.model

                    val eg = traceModel.ex
                    val taint = traceModel.taint
                    val scs = traceModel.secondaryConcerns.toGraph()

                    val executionGraph = eg.graph
                    val heap = eg.heap
                    val assertions = eg.assertions

                    val egadapter = executionGraph.toSerializable()
                    val tgadapter = taint.graph.toSerializable()
                    val scadapter = scs.toSerializable()
                    val assertionArray = assertions.toTypedArray()

                    DocumentUtils.writeObject(epout.resolve("$index-execgraph.ser"), egadapter)
                    DocumentUtils.writeObject(epout.resolve("$index-taint.ser"), tgadapter)
                    DocumentUtils.writeObject(epout.resolve("$index-scs.ser"), scadapter)
                    DocumentUtils.writeObject(epout.resolve("$index-heap.ser"), heap)
                    DocumentUtils.writeObject(epout.resolve("$index-assertions.ser"), assertionArray)
                }

            println("\t\tFinished writing data")
        }
    }
}

object SingleGraphProcessor {
    @JvmStatic
    fun main(args: Array<String>) {
        val proj = Paths.get(args[0])
        val out = Paths.get(args[1])
        GraphProcessor.processProject(proj, out)
        System.exit(0)
    }
}