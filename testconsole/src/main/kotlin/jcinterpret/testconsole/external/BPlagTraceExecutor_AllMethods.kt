package jcinterpret.testconsole.external

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jcinterpret.core.ctx.frame.interpreted.sub
import jcinterpret.document.ConfigDocument
import jcinterpret.testconsole.pipeline.TraceGenerator
import jcinterpret.testconsole.pipeline.TraceGenerator_AllMethods
import jcinterpret.testconsole.thesischpt11.BPlagTraceExtractor
import jcinterpret.testconsole.utils.FileUtils
import jcinterpret.testconsole.utils.Forker
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.streams.toList

object BPlagTraceExecutor_AllMethods {

    val stdlibs = Files.list(Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/bplag/lib"))
        .filter { !Files.isHidden(it) && Files.isRegularFile(it) && it.fileName.toString().endsWith(".jar") }
        .toList()

    val tracedir = Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/bplag/traces-allmethods")

    val important2050_a1_2021 = listOf("3275842", "3277545", "3302896", "3304972", "3306899", "3309266", "3339952", "3345202", "3350434")
    val important2050_external_a1_2021 = listOf("SENG2050-Assignment-1", "SENG2050_assignment1", "SENG2050")

    val important2050_a2_2021 = listOf("3168517", "3204936", "3331609", "3356085", "3309266")
    val important2050_external_a2_2021 = listOf("Java_JSP_Game")

    val sourcedirs = listOf(
//        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/src/SENG2050_A1_2017"),
//        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/src/SENG2050_A1_2018"),
//        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/src/SENG2050_A1_2019"),
//        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/src/SENG2050_A1_2020"),
        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/src/SENG2050_A1_2021"),
//        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/src/SENG2050_A2_2017"),
//        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/src/SENG2050_A2_2018"),
//        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/src/SENG2050_A2_2019"),
//        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/src/SENG2050_A2_2020"),
        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/src/SENG2050_A2_2021"),

        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/github/SENG2050_A1_GITHUB"),
        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/github/SENG2050_A2_GITHUB")

//        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/src/SENG1110_A1_2017"),
//        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/src/SENG1110_A1_2020"),
//        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/src/SENG1110_A1_2021"),
//
//        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/src/SENG1110_A2_2017"),
//        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/src/SENG1110_A2_2020"),
//        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/src/SENG1110_A2_2021"),
//
//        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/github/SENG1110_A1_GITHUB"),
//        Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/github/SENG1110_A2_GITHUB")
    )

    val MAX_PARALLEL = 32
    val sem = Semaphore(MAX_PARALLEL)
    val pool = Executors.newFixedThreadPool(MAX_PARALLEL)

    val MX_HEAP = "-Xmx4G"
    val CONFIG_FILENAME = "config.json"

    val mapper = ObjectMapper().registerModule(KotlinModule())

    @JvmStatic
    fun main(args: Array<String>) {

        for (dir in sourcedirs) {
            println(dir.fileName)

            val isWeb = dir.fileName.toString().contains("SENG2050")

            val outDir = tracedir.resolve(dir.fileName)
            if (!Files.exists(outDir))
                Files.createDirectories(outDir)

            Files.list(dir)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .forEach {
                    makeTrace(it, isWeb, outDir)
                }

            println()
        }

        sem.acquire(MAX_PARALLEL)

        println()

        for (dir in sourcedirs) {
            val outDir = tracedir.resolve(dir.fileName)

            val createdTraces = Files.list(outDir).count()
            val totalSubmissions = Files.list(dir).count()

            println("${dir.fileName}\t- analysed ${createdTraces} of ${totalSubmissions} submissions (${String.format("%.2f", (100.0 * createdTraces / totalSubmissions))})")
        }

        for (dir in sourcedirs) {
            val outDir = tracedir.resolve(dir.fileName)
            val createdTraces = Files.list(outDir)
                .map { it.fileName.toString() }
                .toList()

            if (dir.fileName.toString().contains("SENG2050_A1_2021")) {
                println()
                println("SENG2050_A1_2021")
                println()

                for (imp in important2050_a1_2021) {
                    if (!createdTraces.filter { it.contains(imp) }.any()) {
                        println("failed to find ${imp}")
                    }
                }

            } else if (dir.fileName.toString().contains("SENG2050_A1_GITHUB"))  {
                println()
                println("SENG2050_A1_GITHUB")
                println()

                for (imp in important2050_external_a1_2021) {
                    if (!createdTraces.filter { it.contains(imp) }.any()) {
                        println("failed to find ${imp}")
                    }
                }

            } else if (dir.fileName.toString().contains("SENG2050_A2_2021")) {
                println()
                println("SENG2050_A2_2021")
                println()

                val outDir = tracedir.resolve(dir.fileName)
                val createdTraces = Files.list(outDir)
                    .map { it.fileName.toString() }
                    .toList()

                for (imp in important2050_a2_2021) {
                    if (!createdTraces.filter { it.contains(imp) }.any()) {
                        println("failed to find ${imp}")
                    }
                }
            }  else if (dir.fileName.toString().contains("SENG2050_A2_GITHUB")) {
                println()
                println("SENG2050_A2_GITHUB")
                println()

                for (imp in important2050_external_a2_2021) {
                    if (!createdTraces.filter { it.contains(imp) }.any()) {
                        println("failed to find ${imp}")
                    }
                }
            }
        }

        println("Finished")
        System.exit(0)
    }

    private fun makeTrace(subRoot: Path, isWeb: Boolean, outputDir: Path) {
        val submissionName = subRoot.fileName.toString()
        val traceSaveDir = outputDir.resolve(submissionName)

        if (Files.exists(traceSaveDir))
            return

        val (tmp, config) = if (isWeb) setupWebProject(subRoot)
        else setupConsoleProject(subRoot)

        sem.acquire()

        try {
            CompletableFuture.runAsync(Runnable {
                println("Analysing ${submissionName}")
                val cfg = tmp.resolve(CONFIG_FILENAME).toAbsolutePath().toString()

                val res = Forker.exec(
                    TraceGenerator_AllMethods::class.java,
                    arrayOf(
                        cfg
                    ),
                    props = arrayOf(
                        MX_HEAP
                    ),
                    waitFor = 1L
                )

                if (res == 0) {

                    // Copy over the trace
                    val tmpp = tmp.toAbsolutePath().toString()
                    val trace_out = tmp.resolve("out/src").resolve(submissionName)

                    if (!Files.exists(trace_out)) {
                        System.err.println("No trace for ${submissionName}")
                    } else {
                        println("Finished ${submissionName}")
                        FileUtils.copyDir(trace_out, traceSaveDir)
                    }

                } else {
                    System.err.println("Failed ${submissionName}")
                }

            }, pool).whenComplete { void, throwable ->
                throwable?.printStackTrace()

                sem.release()

                Files.walk(tmp)
                    .sorted(Comparator.reverseOrder())
                    .forEach(Files::delete)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            sem.release()
        }
    }

    data class BPlagConfigBean (
        val tmp: Path,
        val config: ConfigDocument
    )

    private fun setupWebProject(subRoot: Path): BPlagConfigBean {
        val webInfDir = Files.walk(subRoot)
            .filter { it.fileName.toString() == "WEB-INF" }
            .toList()
            .singleOrNull()

        if (webInfDir == null) {
            System.err.println("Cannot find WEB-INF for web project ${subRoot.fileName}")
            return setupConsoleProject(subRoot)
        }

        val tmp = Files.createTempDirectory("bplag_sub")
        val cfg = tmp.resolve("config.json")
        val src = Files.createDirectory(tmp.resolve("src"))
        val libs = Files.createDirectory(tmp.resolve("lib"))
        val out = Files.createDirectory(tmp.resolve("out"))

        val libNames = mutableListOf<String>()
        libNames.addAll(stdlibs.map { "lib/" + it.fileName.toString() })
        stdlibs.forEach { Files.copy(it, libs.resolve(it.fileName)) }

        val classesDirectory = webInfDir.resolve("classes")
        val libDirectory = webInfDir.resolve("lib")

        val subSrc = src.resolve(subRoot.fileName)

        if (Files.exists(classesDirectory)) {
            Files.walk(classesDirectory)
                .filter { Files.isRegularFile(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".java") }
                .forEachOrdered { item ->
                    val target = subSrc.resolve(classesDirectory.relativize(item)).toAbsolutePath()

                    if (!Files.exists(target.parent))
                        Files.createDirectories(target)

                    Files.copy(item, target, StandardCopyOption.REPLACE_EXISTING)
                }
        }

        if (Files.exists(libDirectory)) {
            Files.list(libDirectory)
                .filter { !Files.isHidden(it) && Files.isRegularFile(it) && it.fileName.toString().endsWith(".jar") }
                .forEach {
                    if (!Files.exists(libs.resolve(it.fileName))) {
                        libNames.add("lib/" + it.fileName)
                        Files.copy(it, libs.resolve(it.fileName))
                    }
                }
        }

        val config = ConfigDocument(
            "tmp",
            "src",
            "out",
            libNames,
            null,
            null,
            false,
            2,
            3
        )

        mapper.writeValue(cfg.toFile(), config)

        return BPlagConfigBean(tmp, config)
    }

    private fun setupConsoleProject(subRoot: Path): BPlagConfigBean {
        val tmp = Files.createTempDirectory("bplag_sub")
        val cfg = tmp.resolve("config.json")
        val src = Files.createDirectory(tmp.resolve("src"))
        val libs = Files.createDirectory(tmp.resolve("lib"))
        val out = Files.createDirectory(tmp.resolve("out"))

        val libNames = mutableListOf<String>()
        libNames.addAll(stdlibs.map { "lib/" + it.fileName.toString() })
        stdlibs.forEach { Files.copy(it, libs.resolve(it.fileName)) }

        val subSrc = src.resolve(subRoot.fileName)

        Files.walk(subRoot)
            .filter { Files.isRegularFile(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".java") }
            .forEach { item ->
                val target = subSrc.resolve(item.fileName).toAbsolutePath()

                if (!Files.exists(target.parent))
                    Files.createDirectories(target)

                Files.copy(item, target, StandardCopyOption.REPLACE_EXISTING)
            }

        val config = ConfigDocument(
            "tmp",
            "src",
            "out",
            libNames,
            null,
            null,
            false,
            2,
            3
        )

        mapper.writeValue(cfg.toFile(), config)

        return BPlagConfigBean(tmp, config)
    }
}