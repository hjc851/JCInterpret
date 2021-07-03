package jcinterpret.testconsole.pipeline

import jcinterpret.core.ExecutionConfig
import jcinterpret.core.JavaConcolicInterpreterFactory
import jcinterpret.core.TooManyContextsException
import jcinterpret.core.bytecode.BytecodeLibraryFactory
import jcinterpret.core.control.UnsupportedLanguageFeature
import jcinterpret.core.descriptors.*
import jcinterpret.core.source.SourceLibraryFactory
import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.document.ConfigDocument
import jcinterpret.document.DocumentUtils
import jcinterpret.entry.EntryPointFinder
import jcinterpret.parser.Parser
import jcinterpret.testconsole.utils.FileUtils
import jcinterpret.testconsole.utils.Project
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.streams.toList

object TraceGenerator_AllMethods {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            run(args)
        } catch (e: Throwable) {
            e.printStackTrace(System.err)
        } finally {
            System.exit(0)
        }
    }

    private fun run(args: Array<String>) {

        if (args.count() != 1)
            error("One argument is expected listing the path to a valid config document.")

        val docPath = Paths.get(args[0])

        if (!Files.exists(docPath) || !Files.isRegularFile(docPath))
            error("The passed argument ${docPath} is not a path to a file")

        val start = Instant.now()

        val document = DocumentUtils.readJson(docPath, ConfigDocument::class)
        ExecutionConfig.loggingEnabled = document.loggingEnabled
        ExecutionConfig.maxLoopExecutions = document.maxLoopExecutions
        ExecutionConfig.maxRecursiveCalls = document.maxRecursiveCalls
        ExecutionConfig.allMethodCoverage = true

        val root = docPath.parent
        val projectsRoot = root.resolve(document.projectsRoot)
        val output = root.resolve(document.output)
            .resolve(projectsRoot.fileName.toString())

        if (!Files.exists(output))
            Files.createDirectories(output)

        if (!Files.exists(projectsRoot))
            throw IllegalArgumentException("Unknown projects root $projectsRoot")

        val globalLibraries = document.globalLibraries.map { root.resolve(it) }
        val projectLibraries = document.projectLibraries?.map { it.key to it.value.map { root.resolve(it) } }?.toMap()
            ?: emptyMap()

        val projectPaths = Files.list(projectsRoot)
            .filter { Files.isDirectory(it) }
            .toList()
            .sorted()

        val dir = output

        //  Pre-Processing: Parse the projects
        println("Starting ${Date()}")
        println("Pre-Processing: Parsing the projects")
        val projects = projectPaths.map { path ->
                val id = path.fileName.toString()

                val projDir = dir.resolve(id)
                if (Files.exists(projDir)) {
                    println("${id} already exists")
                    return@map null
                }

                val sources = FileUtils.listFiles(path, ".java")
                val directories = FileUtils.listDirectories(path)
                val eps = document.entryPoints?.get(id)?.toList() ?: emptyList()
                val libraries = if (projectLibraries.containsKey(id)) {
                    globalLibraries + projectLibraries[id]!!
                } else {
                    globalLibraries
                }

                val compilationUnits = Parser.parse(sources, libraries, directories)
                val msg = compilationUnits.flatMap { it.messages.toList() }
                    .filter { it.message.contains("Syntax error") || it.message.contains("cannot be resolved") }

                if (msg.isNotEmpty()) {
                    println("Ignoring $id")
                    msg.forEach { println("\t${it.startPosition}:${it.length} ${it.message}") }

                    println("Found sources:")
                    sources.forEach(::println)

                    println("Using libraries")
                    libraries.forEach(::println)

                    return@map null
                }

                val bytecodeLibrary = BytecodeLibraryFactory.build(path)
                val sourceLibrary = SourceLibraryFactory.build(compilationUnits)
                val descriptorLibrary = DescriptorLibrary(
                    listOf(
                        BytecodeLibraryDescriptorResolver(bytecodeLibrary),
                        BindingDescriptorResolver(compilationUnits),
                        ClassFileDescriptorResolver(libraries)
                    )
                )

                val entries = EntryPointFinder.findAllMethods(compilationUnits)

                if (entries.isEmpty()) {
                    println("No entry points for ${id}")
                    return@map null
                }

                return@map Project(
                    id,
                    path,
                    compilationUnits,
                    descriptorLibrary,
                    sourceLibrary,
                    bytecodeLibrary,
                    entries
                )
            }.toList()
            .filterNotNull()

        if (projects.isEmpty()) {
            error("No viable projects to analyse")
        }

        println("Executing ${projects.size} projects")

        var createdTraces = 0

        println("Generating Execution Traces")
        projects.forEach { project ->
                try {
                    val projDir = dir.resolve(project.id)
                    if (Files.exists(projDir))
                        return@forEach

                    println("Executing ${project.id}")
                    val result = project.entries.parallelStream()
                        .map { entry ->
                            val sig = entry.binding.qualifiedSignature()
                            val interpreter = JavaConcolicInterpreterFactory.build(
                                JavaConcolicInterpreterFactory.ExecutionMode.SOURCECODE,
                                sig,
                                project.descriptorLibrary,
                                project.sourceLibrary,
                                project.bytecodeLibrary
                            )
                            val traces = interpreter.execute()
                            return@map entry to traces
                        }.toList()
                        .toMap()

                    Files.createDirectory(projDir)

                    val traceCount = result.values.map { it.size }.sum()
                    createdTraces += traceCount
                    if (traceCount == 0) {
                        System.err.println("WARNING: NO TRACES FOR ${project.id}")
                        return@forEach
                    }

                    for ((entry, traces) in result) {
                        val msig = entry.binding.qualifiedSignature()
                        val fout = projDir.resolve(msig.toString().replace("/", ".") + ".ser")
                        Files.createFile(fout)

                        val document = EntryPointExecutionTraces (
                            msig,
                            traces.toTypedArray()
                        )

                        DocumentUtils.writeObject(fout, document)
                    }

                } catch (e: UnsupportedLanguageFeature) {
                    System.err.println("Removing ${project.id} due to: ${e.msg}")
                } catch (e: TooManyContextsException) {
                    System.err.println("Too many contexts in ${project.id}")
                } catch (e: UnresolvableDescriptorException) {
                    System.err.println("Cannot resolve descriptor for type ${e.sig}")
                }
            }

        if (createdTraces == 0) {
            error("No traces extracted")
        }

        val finish = Instant.now()
        val elapsed = Duration.between(start, finish)

        println("Elapsed: ${elapsed.seconds}s")

        println("Finished")
    }
}