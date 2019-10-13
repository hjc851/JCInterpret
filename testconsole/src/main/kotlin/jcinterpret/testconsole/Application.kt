package jcinterpret.testconsole

import jcinterpret.config.document.ConfigDocumentUtils
import jcinterpret.core.JavaConcolicInterpreterFactory
import jcinterpret.core.descriptors.DescriptorLibraryFactory
import jcinterpret.core.descriptors.qualifiedSignature
import jcinterpret.core.source.SourceLibraryFactory
import jcinterpret.entry.EntryPointFinder
import jcinterpret.parser.Parser
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

fun main(args: Array<String>) {
    if (args.count() != 1)
        error("One argument is expected listing the path to a valid config document")

    val docPath = Paths.get(args[0])

    if (!Files.exists(docPath) || !Files.isRegularFile(docPath))
        error("The passed argument is not a path to a file")

    val document = ConfigDocumentUtils.read(docPath)

    val root = docPath.parent
    val projectsRoot = root.resolve(document.projectsRoot)

    val globalLibraries = document.globalLibraries.map { root.resolve(it) }
    val projectLibraries = document.projectLibraries?.map { it.key to it.value.map { root.resolve(it) } }?.toMap()
        ?: emptyMap()

    val projectPaths = Files.list(projectsRoot)
        .filter { Files.isDirectory(it) }
        .toList()

    //  Pre-Processing: Parse the projects
    println("Pre-Processing: Parsing the projects")
    val projects = projectPaths.map { path ->
        val id = path.fileName.toString()
        val sources = FileUtils.listFiles(path, ".java")
        val directories = FileUtils.listDirectories(path)
        val libraries = if (projectLibraries.containsKey(id)) {
            globalLibraries + projectLibraries[id]!!
        } else {
            globalLibraries
        }

        val compilationUnits = Parser.parse(sources, libraries, directories)
        val descriptorLibrary = DescriptorLibraryFactory.build(compilationUnits, libraries)
        val sourceLibrary = SourceLibraryFactory.build(compilationUnits)

        val entries = EntryPointFinder.find(compilationUnits)

        return@map Project(id, path, compilationUnits, descriptorLibrary, sourceLibrary, entries)
    }.toList()

    println("Generating Execution Traces")
    val executionTraces = projects.map { project ->
        project to project.entries.map { entry ->
            val sig = entry.binding.qualifiedSignature()
            val interpreter = JavaConcolicInterpreterFactory.build(sig, project.descriptorLibrary, project.sourceLibrary)
            val traces = interpreter.execute()
            return@map entry to traces
        }.toList().toMap()
    }.toList().toMap()


}