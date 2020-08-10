package jcinterpret.testconsole.plagiariser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

object SPPlagiariseWrapperApplication {

    @JvmStatic
    fun main(args: Array<String>) {
        val input = Paths.get(args[0])
        val output = Paths.get(args[1])

        if (Files.exists(output)) {
            Files.walk(output)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        }

        Files.createDirectory(output)

        val exec = Paths.get("testconsole/src/main/resources/spplagiarise/bin/runner").toAbsolutePath().toString()
        val rt = Paths.get("testconsole/src/main/resources/rt.jar").toAbsolutePath()

        val dirs = Files.list(input)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
//            .filter { !completed.contains(it.fileName.toString()) }
            .toList()
            .sortedBy { it.fileName }

        for (dir in dirs) {
            for (i in 1 .. 5) {
                val config = ConfigDocument (
                    "inp",
                    "out",
                    "libs",
                    1,
                    true,
                    System.currentTimeMillis(),
                    i >= 1,
                    i >= 2,
                    i >= 3,
                    i >= 4,
                    i >= 5,
                    50
                )

                val tmp = Files.createTempDirectory("_sp")
                println(tmp.toAbsolutePath())

                val inp = Files.createDirectory(tmp.resolve("inp"))
                val out = Files.createDirectory(tmp.resolve("out"))
                val libs = Files.createDirectory(tmp.resolve("libs"))
                val cfg = tmp.resolve("config.json").toAbsolutePath()

                Files.walk(dir)
                    .forEach { path ->
                        if (path == dir) return@forEach

                        if (Files.isDirectory(path)) {
                            val dirPth = inp.resolve(dir.relativize(path))
                            Files.createDirectories(dirPth)
                        } else {
                            val fPth = inp.resolve(dir.relativize(path))
                            Files.copy(path, fPth)
                        }
                    }

                Files.copy(rt, libs.resolve("rt.jar"))
                mapper.writeValue(cfg.toFile(), config)

                val proc = ProcessBuilder()
                    .command(
                        "/bin/sh",
                        exec,
                        cfg.toString()
                    )
                    .inheritIO()
                    .start()

                val exitCode = proc.waitFor()
                if (exitCode != 0) {
                    val out = proc.inputStream.bufferedReader().readLines()
                    System.err.println(out.joinToString("\n"))
                    throw IllegalStateException()
                }

                // Copy files over
                val copyDir = output.resolve(dir.fileName.toString() + "-L" + i)
                Files.createDirectory(copyDir)

                val variantRoot = out.resolve("0")
                Files.walk(variantRoot)
                    .forEach { path ->
                        if (variantRoot == path) return@forEach

                        if (Files.isDirectory(path)) {
                            val dirPth = copyDir.resolve(variantRoot.relativize(path))
                            Files.createDirectories(dir)
                        } else {
                            var fPth = copyDir.resolve(variantRoot.relativize(path))
                            Files.copy(path, fPth)
                        }
                    }

                // Delete
                Files.walk(tmp)
                    .sorted(Comparator.reverseOrder())
                    .forEach(Files::delete)
            }
        }
    }
}

val mapper = ObjectMapper().registerModule(KotlinModule())

class ConfigDocument (
    val input: String,
    val output: String,
    val libs: String,
    val copies: Int,
    val extreme: Boolean,
    val seed: Long?,
    val l1: Boolean,
    val l2: Boolean,
    val l3: Boolean,
    val l4: Boolean,
    val l5: Boolean,
    val randomWeight: Int
)