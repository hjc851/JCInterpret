package jcinterpret.testconsole.automator

import jcinterpret.document.ConfigDocument
import jcinterpret.document.DocumentUtils
import jcinterpret.testconsole.pipeline.TraceGenerator
import jcinterpret.testconsole.utils.FileUtils
import jcinterpret.testconsole.utils.Forker
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Semaphore
import kotlin.streams.toList

val MAX_PARALLEL = 32

object ForkingTraceGenerator {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.count() != 1)
            error("One argument is expected listing the path to a valid config document")

        val docPath = Paths.get(args[0])

        if (!Files.exists(docPath) || !Files.isRegularFile(docPath))
            error("The passed argument is not a path to a file")

        val start = Instant.now()

        val document = DocumentUtils.readJson(docPath, ConfigDocument::class)
        val root = docPath.parent
        val projectsRoot = root.resolve(document.projectsRoot)
        val output = root.resolve(document.output)
            .resolve(projectsRoot.fileName.toString())

        if (!Files.exists(output))
            Files.createDirectories(output)

        if (!Files.exists(projectsRoot))
            throw IllegalArgumentException("Unknown projects root $projectsRoot")

        val projectPaths = Files.list(projectsRoot)
            .filter { Files.isDirectory(it) }
            .toList()
            .sorted()

        val sem = Semaphore(MAX_PARALLEL)

        for (proj in projectPaths) {
            if (Files.exists(output.resolve(proj.fileName)))
                continue //FileUtils.deleteDirectory(output.resolve(proj.fileName))

            sem.acquire()

            val tmp = Files.createTempDirectory("jcinterpret_forking")
            val newConfig = Files.copy(docPath, tmp.resolve(docPath.fileName))
            val src = Files.createDirectories(tmp.resolve(document.projectsRoot))
            FileUtils.copyDir(proj, src.resolve(proj.fileName))
            val out = Files.createDirectories(tmp.resolve(document.output))

            // Copy libs
            for (lib in document.globalLibraries) {
                val to = tmp.resolve(lib)
                if (!Files.exists(to.parent)) Files.createDirectories(to.parent)
                Files.copy(root.resolve(lib), tmp.resolve(lib))
            }

            // Copy any project libs
            val projlibs = document.projectLibraries?.get(proj.fileName.toString())
            if (projlibs != null) {
                for (lib in projlibs) {
                    val to = tmp.resolve(lib)
                    if (!Files.exists(to.parent)) Files.createDirectories(to.parent)
                    Files.copy(root.resolve(lib), tmp.resolve(lib))
                }
            }

            CompletableFuture.runAsync {
                println("Executing ${proj.fileName.toString()}")

                val res = Forker.exec(
                    TraceGenerator::class.java,
                    arrayOf(
                        newConfig.toAbsolutePath().toString()
                    ),
                    props = arrayOf(
                        "-Xmx8G"
                    )
                )

                if (res == 0) {
                    println("Finished ${proj.fileName.toString()}")
                    FileUtils.copyDir(
                        out.resolve(projectsRoot.fileName).resolve(proj.fileName),
                        output.resolve(proj.fileName)
                    )
                } else {
                    System.err.println("${proj.fileName.toString()}")
                }

            }.whenComplete { void, throwable ->
                throwable?.printStackTrace()
                sem.release()

                FileUtils.deleteDirectory(tmp)
            }
        }

        println("Awaiting finish ...")
        sem.acquire(MAX_PARALLEL)

        val finish = Instant.now()
        val elapsed = Duration.between(start, finish)

        println("Elapsed: ${elapsed.seconds}s")
        System.exit(0)
    }
}