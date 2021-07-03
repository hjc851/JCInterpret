package jcinterpret.testconsole

import jcinterpret.core.ExecutionConfig
import jcinterpret.core.JavaConcolicInterpreterFactory
import jcinterpret.core.TooManyContextsException
import jcinterpret.core.bytecode.BytecodeLibraryFactory
import jcinterpret.core.control.UnsupportedLanguageFeature
import jcinterpret.core.descriptors.*
import jcinterpret.core.source.SourceLibraryFactory
import jcinterpret.document.ConfigDocument
import jcinterpret.document.DocumentUtils
import jcinterpret.entry.EntryPointFinder
import jcinterpret.parser.Parser
import jcinterpret.testconsole.utils.FileUtils
import jcinterpret.testconsole.utils.Project
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.IntStream
import kotlin.streams.toList

fun main(args: Array<String>) {
    if (args.count() != 1)
        error("One argument is expected listing the path to a valid config document")

    val docPath = Paths.get(args[0])

    if (!Files.exists(docPath) || !Files.isRegularFile(docPath))
        error("The passed argument is not a path to a file")

    val document = DocumentUtils.readJson(docPath, ConfigDocument::class)
    ExecutionConfig.loggingEnabled = document.loggingEnabled
    ExecutionConfig.maxLoopExecutions = document.maxLoopExecutions
    ExecutionConfig.maxRecursiveCalls = document.maxRecursiveCalls

    val root = docPath.parent
    val projectsRoot = root.resolve(document.projectsRoot)
    val output = root.resolve(document.output)

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

    //  Pre-Processing: Parse the projects
    println("Pre-Processing: Parsing the projects")
    val projects = projectPaths.mapNotNull { path ->
        val id = path.fileName.toString()
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
            return@mapNotNull null
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

        val entries = EntryPointFinder.find(compilationUnits, eps)

        return@mapNotNull Project(
            id,
            path,
            compilationUnits,
            descriptorLibrary,
            sourceLibrary,
            bytecodeLibrary,
            entries
        )
    }.toList()

    println("Using ${projects.size} of ${projectPaths.size}")
    println()

    println("Generating Execution Traces")
    val et = projects.map { project ->
        val traces = project.entries.map { entry ->
            val sig = BindingUtils.qualifiedSignature(entry.binding)
            val interpreter = JavaConcolicInterpreterFactory.build(
                JavaConcolicInterpreterFactory.ExecutionMode.PROJECT_BYTECODE,
                sig,
                project.descriptorLibrary,
                project.sourceLibrary,
                project.bytecodeLibrary
            )
            val traces = interpreter.execute()
            return@map entry to traces
        }

        return@map project to traces
    }.toMap()

    val count = et.values.sumBy { it.size }

    println("Generated ${count} execution trace(s)")

    println("Finished")
}