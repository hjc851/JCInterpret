package jcinterpret.testconsole.plagiariser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jcinterpret.testconsole.utils.FileUtils
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.streams.toList

object BulkPlagiariser {
    val config = arrayOf(
        10,
        20,
        30,
        40,
        50
    )

    val exec = Paths.get("testconsole/src/main/resources/spplagiarise/bin/runner").toAbsolutePath().toString()

    val rt = Paths.get("testconsole/src/main/resources/rt.jar").toAbsolutePath()
    val servlet_api = Paths.get("testconsole/src/main/resources/servlet-api.jar").toAbsolutePath()

    val javac = System.getProperty("java.home") + "/bin/javac"

    val _work = ThreadLocal<Path>()
    var work: Path
        get() = _work.get()
        set(value) = _work.set(value)

    val mapper = ObjectMapper().registerModule(KotlinModule())

    // CONFIG
    var DS_NAME = "Collected"
    var RANDOM_WEIGHT = 20

    @JvmStatic
    fun main(args: Array<String>) {
        // CONFIG
        DS_NAME = args[0] // "Collected"
        RANDOM_WEIGHT = args[1].toInt() // 20

        println("Running with args:")
        println("DS_NAME ${DS_NAME}")
        println("RANDOM_WEIGHT ${RANDOM_WEIGHT}")
        println()

        val input = Paths.get("/media/haydencheers/Data/PhD/Data Sets/${DS_NAME}")
        val output = Paths.get("/media/haydencheers/Data/SymbExec/src/${DS_NAME}_${RANDOM_WEIGHT}")

        if (!Files.exists(output)) Files.createDirectory(output)

        val inputs = Files.list(input)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }
            .sortedBy { it.fileName.toString() }

        // copy inputs to outputs
        for (input in inputs) {
            val to = output.resolve(input.fileName)
            if (!Files.exists(to)) FileUtils.copyDir(input, to)
        }

        // Do the variant generation
        inputs//.parallelStream()
            .forEach { input ->
                println(input.toAbsolutePath())

                for (l in 1 .. 5) {
                    var errorCounter = 0

                    val count = config[l-1]

                    var created = 0
                    creator@while (created < count) {
                        // tmp working dir
                        work = Files.createTempDirectory("spplagiarise_work")
                        Files.copy(Paths.get("db.blob"), work.resolve("db.blob"))

                        // Check if this variant already created
                        val target = output.resolve(input.fileName.toString() + "-L${l}" + "-${created}")
                        if (Files.exists(target)) {
                            val count = Files.walk(target)
                                .filter { Files.isRegularFile(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".java") }
                                .count()

                            if (count > 0) {
                                created++
                                continue@creator
                            } else {
                                FileUtils.deleteDirectory(target)
                            }

                        }

                        // Config file (regen seed)
                        val config = makeConfig(l)

                        try {
                            // Make variant + handle
                            val variantRoot = makeVariant(input, config)

                            if (variantRoot != null) {
                                if (validateVariant(variantRoot)) {
                                    // Save the variant to the output
                                    val id = created++
                                    FileUtils.copyDir(variantRoot, target)

                                    println("Created variant L${l}-${id}")
                                    errorCounter = 0

                                } else {
//                                    println(work)
                                    println("Error: Variant is invalid.")
                                    errorCounter++
                                }
                            } else {
                                println("Error: No variant path returned.")
                                errorCounter++
                            }


                        } finally {
                            try {
                                synchronized(this) {
                                    Files.copy(work.resolve("db.blob"), Paths.get("db.blob"), StandardCopyOption.REPLACE_EXISTING)
                                }
                            } catch (e: Exception) { e.printStackTrace() }

                            FileUtils.deleteDirectory(work)
                            if (errorCounter >= 10) {
                                System.err.println("Error: Aborting ${input} at L${l}-${created}")
                                return@forEach
                            }
                        }
                    }
                }
            }

        println("Finished")
        System.exit(0)
    }

    // Variant must be underneath work directory
    fun makeVariant(input: Path, config: ConfigDocument): Path? {

        val inp = work.resolve("inp")
        val out = Files.createDirectory(work.resolve("out"))
        val libs = Files.createDirectory(work.resolve("libs"))
        val cfg = work.resolve("config.json").toAbsolutePath()

        // Copy the input to the spplagiarise input dir
        // + copy the rt.jar + write the config file
        FileUtils.copyDir(input, inp.resolve(inp))
        Files.copy(rt, libs.resolve("rt.jar"))
        Files.copy(servlet_api, libs.resolve("servlet_api.jar"))
        mapper.writeValue(cfg.toFile(), config)

        val variantRoot = out.resolve("0")

        if (!invokeSPPlagiarise(cfg)) return null
        if (!Files.isDirectory(variantRoot)) return null

        return variantRoot
    }

    @Synchronized
    fun validateVariant(root: Path): Boolean {
        val sources = Files.walk(root)
            .filter { Files.isRegularFile(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".java") }
            .map { it.toAbsolutePath().toString() }
            .use { it.toList() }

        if (sources.isEmpty())
            return false

        val cmd = arrayOf(BulkPlagiariser.javac, "-cp", ".:" + BulkPlagiariser.servlet_api.toAbsolutePath().toString()) + sources

        val proc = ProcessBuilder()
            .command(*cmd)
            .inheritIO()
            .start()

        proc.waitFor()

        if (proc.exitValue() != 0) {
            val err = proc.errorStream.bufferedReader()
                .readLines()

            // Dont care about already defined errors (interpretor can handle it)
            for (line in err) {
                if (line.contains("variable i is already defined"))
                    return true
            }
        }

        return proc.exitValue() == 0
    }

    fun invokeSPPlagiarise(config: Path): Boolean {
        val proc = ProcessBuilder()
            .command(
                "/bin/sh",
                exec,
                config.toAbsolutePath().toString()
            )
            .directory(work.toFile())
            .inheritIO()
            .start()

        proc.waitFor()

        if (proc.exitValue() != 0) {
            val inp = proc.inputStream
            inp.bufferedReader()
                .lines()
                .forEach(System.out::println)
            val err = proc.errorStream
            err.bufferedReader()
                .lines()
                .forEach(System.err::println)
        }

        return proc.exitValue() == 0
    }

    fun makeConfig(level: Int): ConfigDocument {
        return ConfigDocument (
            "inp",
            "out",
            "libs",
            1,
            false,
            System.currentTimeMillis(),
            level >= 1,
            level >= 2,
            level >= 3,
            level >= 4,
            level >= 5,
            RANDOM_WEIGHT
        )
    }
}



