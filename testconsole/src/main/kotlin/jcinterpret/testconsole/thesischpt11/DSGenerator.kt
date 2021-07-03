package jcinterpret.testconsole.thesischpt11

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jcinterpret.testconsole.plagiariser.BulkPlagiariser
import jcinterpret.testconsole.plagiariser.ConfigDocument
import jcinterpret.testconsole.utils.FileUtils
import java.lang.Exception
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.streams.toList

object DSGenerator {

    val exec = Paths.get("testconsole/src/main/resources/spplagiarise/bin/runner").toAbsolutePath().toString()

    val src_root = Paths.get("/media/haydencheers/Data/ThesisChpt11/SENG2050_A1_2018")
    val variant_root = Paths.get("/media/haydencheers/Data/ThesisChpt11/variants")
    val lib_root = Paths.get("/media/haydencheers/Data/ThesisChpt11/lib")

    val trns_chances = 10 .. 100 step 10
    val trns_levels = 1 .. 5

    val mapper = ObjectMapper().registerModule(KotlinModule())

    val MAX_PARALLEL = 32
    val pool = Executors.newFixedThreadPool(MAX_PARALLEL)
    val sem = Semaphore(MAX_PARALLEL)

    @JvmStatic
    fun main(args: Array<String>) {
        val bases = Files.list(src_root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }
            .sorted()

        for (base in bases) {
            val bout = variant_root.resolve(base.fileName)

            for (trns_chance in trns_chances) {
                val cout = bout.resolve("C${trns_chance}")
                if (!Files.exists(cout)) Files.createDirectories(cout)

                for (trns_level in trns_levels) {
                    val lout = cout.resolve("L${trns_level}")
                    if (!Files.exists(lout)) Files.createDirectories(lout)

                    val copies = (trns_level * 10).div(2).toInt()
                    for (copy in 1 .. copies) {
                        val copy_out = lout.resolve("V${copy}")
                        if (Files.exists(copy_out)) continue

                        sem.acquire(1)

                        CompletableFuture.runAsync(Runnable {
                            if (makeVariant(copy_out, trns_level, trns_chance, base)) {
                                println("Made ${base.fileName} C${trns_chance} L${trns_level} X${copy} ")
                            } else {
                                System.err.println("Fail ${base.fileName} C${trns_chance} L${trns_level} X${copy} ")
                            }
                        }, pool).whenComplete { void, throwable ->
                            throwable?.printStackTrace()
                            sem.release()
                        }
                    }
                }
            }
        }

        sem.acquire(MAX_PARALLEL)
        System.exit(0)
    }

    fun makeVariant(copy_out: Path, trns_level: Int, trns_chance: Int, base: Path): Boolean {

        val config = ConfigDocument (
            "inp",
            "out",
            "libs",
            1,
            false,
            System.currentTimeMillis(),
            trns_level >= 1,
            trns_level >= 2,
            trns_level >= 3,
            trns_level >= 4,
            trns_level >= 5,
            trns_chance
        )

        val tmp = Files.createTempDirectory("_sp")
        val inp = (tmp.resolve("inp"))
        val libs = (tmp.resolve("libs"))
        val out = (tmp.resolve("out"))
        val cfg = tmp.resolve("config.json").toAbsolutePath()

        synchronized(this) {
            Files.copy(Paths.get("db.blob"), tmp.resolve("db.blob"))
        }

        FileUtils.copyDir(base, inp)
        FileUtils.copyDir(lib_root, libs)

        mapper.writeValue(cfg.toFile(), config)

        for (attempt in 1 .. 2) {
            val success = invokeSPPlagiarise(cfg)

            if (!success) continue

            val variant_path = out.resolve("0")
            if (!Files.exists(variant_path)) continue
            if (!validateVariant(variant_path)) continue

            // Success - save it
            FileUtils.copyDir(variant_path, copy_out)

            // Delete
            synchronized(this) {
                Files.copy(tmp.resolve("db.blob"), Paths.get("db.blob"), StandardCopyOption.REPLACE_EXISTING)
            }

            Files.walk(tmp)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)

            return true
        }

        Files.walk(tmp)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::delete)

        return false
    }

    fun invokeSPPlagiarise(config: Path): Boolean {
        val proc = ProcessBuilder()
            .command(
                "/bin/sh",
                BulkPlagiariser.exec,
                config.toAbsolutePath().toString()
            )
            .directory(config.parent.toFile())
//            .inheritIO()
            .start()

        proc.waitFor()

//        if (proc.exitValue() != 0) {
//            val inp = proc.inputStream
//            inp.bufferedReader()
//                .lines()
//                .forEach(System.out::println)
//            val err = proc.errorStream
//            err.bufferedReader()
//                .lines()
//                .forEach(System.err::println)
//        }

        return proc.exitValue() == 0
    }

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
//            .inheritIO()
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
}