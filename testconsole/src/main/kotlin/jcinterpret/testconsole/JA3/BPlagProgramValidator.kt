package jcinterpret.testconsole.JA3

import jcinterpret.core.ExecutionConfig
import jcinterpret.core.JavaConcolicInterpreterFactory
import jcinterpret.core.descriptors.DescriptorLibraryFactory
import jcinterpret.core.descriptors.qualifiedSignature
import jcinterpret.core.source.SourceLibraryFactory
import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.document.ConfigDocument
import jcinterpret.document.DocumentUtils
import jcinterpret.entry.EntryPointFinder
import jcinterpret.parser.Parser
import jcinterpret.testconsole.utils.FileUtils
import jcinterpret.testconsole.utils.Project
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.streams.toList

object BPlagProgramValidator {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.count() != 1)
            error("One argument is expected listing the path to a valid config document.")

        val docPath = Paths.get(args[0])

        if (!Files.exists(docPath) || !Files.isRegularFile(docPath))
            error("The passed argument is not a path to a file")

        val start = Instant.now()

        val document = DocumentUtils.readJson(docPath, ConfigDocument::class)
        ExecutionConfig.loggingEnabled = document.loggingEnabled
        ExecutionConfig.maxLoopExecutions = document.maxLoopExecutions
        ExecutionConfig.maxRecursiveCalls = document.maxRecursiveCalls

        val root = docPath.parent
        val projectsRoot = root.resolve(document.projectsRoot)

        if (!Files.exists(projectsRoot))
            throw IllegalArgumentException("Unknown projects root $projectsRoot")

        val globalLibraries = document.globalLibraries.map { root.resolve(it) }
        val projectLibraries = document.projectLibraries?.map { it.key to it.value.map { root.resolve(it) } }?.toMap()
            ?: emptyMap()

        val projectPaths = Files.list(projectsRoot)
            .filter { Files.isDirectory(it) }
            .toList()
            .sorted()

        val dir = Files.createTempDirectory("JCInterpret-deleter")

        val toDelete = mutableListOf<Path>()

        //  Pre-Processing: Parse the projects
        val projects = projectPaths.parallelStream()
            .map { path ->
                try {
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
                        System.err.println("Ignoring $id")
                        msg.forEach { println("\t${it.startPosition}:${it.length} ${it.message}") }
                        toDelete.add(path)
                        return@map null
                    }

                    val descriptorLibrary = DescriptorLibraryFactory.build(compilationUnits, libraries)
                    val sourceLibrary = SourceLibraryFactory.build(compilationUnits)
                    val entries = EntryPointFinder.find(compilationUnits, eps)

                    return@map Project(
                        id,
                        path,
                        compilationUnits,
                        descriptorLibrary,
                        sourceLibrary,
                        entries
                    )
                } catch (e: Exception) {
                    System.err.println("Error processing ${path.fileName}")
                    toDelete.add(path)
                    return@map null
                }
            }.toList()
            .filterNotNull()

        println("Executing ${projects.size} projects")

        println("Generating Execution Traces")
        projects
            .parallelStream()
            .forEach { project ->
                try {
                    val projDir = dir.resolve(project.id)
//                    if (Files.exists(projDir))
//                        return@forEach

                    println("Executing ${project.id}")
                    val result = CompletableFuture.supplyAsync {
                        project.entries//.parallelStream()
                            .map { entry ->
                                val sig = entry.binding.qualifiedSignature()
                                val interpreter = JavaConcolicInterpreterFactory.build(sig, project.descriptorLibrary, project.sourceLibrary)
                                val traces = interpreter.execute()
                                return@map entry to traces
                            }.toList()
                            .toMap()
                    }.get(1, TimeUnit.MINUTES)

                    Files.createDirectory(projDir)

                    val traceCount = result.values.map { it.size }.sum()
                    if (traceCount == 0) {
                        toDelete.add(project.path)
                        System.err.println("WARNING: NO TRACES FOR ${project.id}")
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

                } catch (e: Exception) {
                    System.err.println("${project.id} failed exceptionally")
                    toDelete.add(project.path)
                }

//                catch (e: UnsupportedLanguageFeature) {
//                    System.err.println("Removing ${project.id} due to: ${e.msg}")
//                    toDelete.add(project.path)
//                } catch (e: TooManyContextsException) {
//                    System.err.println("Too many contexts in ${project.id}")
//                    toDelete.add(project.path)
//                } catch (e: UnresolvableDescriptorException) {
//                    System.err.println("Cannot resolve descriptor for type ${e.sig}")
//                    toDelete.add(project.path)
//                } catch (e: TimeoutException) {
//                    System.err.println("${project.id} timeout waiting for traces ...")
//                    toDelete.add(project.path)
//                }
            }

        println()
        for (project in toDelete) {
            println("Deleting ${project.fileName}")
            Files.walk(project)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        }

        // Cleanup
        Files.walk(dir)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::delete)

        println("Finished")
        System.exit(0)
    }
}