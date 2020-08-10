package jcinterpret.testconsole.plagiariser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jcinterpret.testconsole.utils.FileUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.streams.toList

object AccuracyPlagiariser {
    val ds_src = Paths.get("/media/haydencheers/Data/SymbExec/src")
    val variant_out = Paths.get("/media/haydencheers/Data/SymbExec/variant_src")

    val ds_names = listOf(
        "COMP2230_A1_2018",
        "COMP2240_A1_2018",
        "COMP2240_A2_2018",
        "COMP2240_A3_2018",
        "SENG1110_A1_2017",
        "SENG1110_A2_2017",
        "SENG2050_A1_2017",
        "SENG2050_A2_2017",
        "SENG2050_A1_2018",
        "SENG2050_A2_2018",
        "SENG2050_A1_2019",
        "SENG2050_A2_2019"
    )

    val trns_levels = listOf(20, 40, 60, 80, 100)
    val variant_count = 10

    val global_libs = Files.list(Paths.get("/media/haydencheers/Data/SymbExec/lib"))
        .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".jar") }
        .use { it.toList() }
        .sortedBy { it.fileName.toString() }

    val mapper = ObjectMapper().registerModule(KotlinModule())

    @JvmStatic
    fun main(args: Array<String>) {
        ds_names
            .parallelStream()
            .forEach { ds ->
            val ds_root = ds_src.resolve(ds)
            val variant_root = variant_out.resolve(ds)
            if (!Files.exists(variant_root)) Files.createDirectories(variant_root)

            // Identify the existing variants
            val existing_variants = Files.list(variant_root)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .map { ds_root.resolve(it.fileName.toString()) }
                .use { it.toList().toMutableList() }

            // Get the list of base programs (existing + random selection)
            val base_programs = if (existing_variants.size != 5) {
                // Get a list of additional variants
                existing_variants + Files.list(ds_root)
                    .parallel()
                    .filter { Files.isDirectory(it) && !Files.isHidden(it) && !existing_variants.contains(it) }
                    .filter { VariantValidator.validateVariant(it, global_libs) }
                    .use { it.toList() }
                    .sortedBy { it.fileName.toString() }
                    .shuffled()
                    .take(5 - existing_variants.size)
            } else {
                existing_variants
            }

            // Iterate through base programs
            baseLoop@for (base in base_programs) {
                val base_out = variant_root.resolve(base.fileName.toString())
                if (!Files.exists(base_out))
                    Files.createDirectories(base_out)

                // For the 5 levels of transformation
                levelLoop@for (level in 1 .. 5) {
                    val level_out = base_out.resolve("L${level}")

                    // For the variant probability levels
                    for (trns_level in trns_levels) {
                        val trns_out = level_out.resolve("T${trns_level}")

                        // Create the 10 variants
                        for (i in 1 .. variant_count) {
                            val variant_out = trns_out.resolve("V${i}")

                            // Continue if the variant already exists
                            if (Files.exists(variant_out)) continue

                            var error_counter = 0

                            creationLoop@do {
                                // Create working directory + copy over synonym db
                                val work = Files.createTempDirectory("SPPlagiarise_work")
                                synchronized(this) {
                                    Files.copy(Paths.get("db.blob"), work.resolve("db.blob"))
                                }

                                try {
                                    // Make the variant
                                    val config = makeConfig(level, trns_level)
                                    val variant = makeVariant(work, base, global_libs, config)

                                    if (variant != null) {  // A path was returned
                                        if (VariantValidator.validateVariant(variant, global_libs)) {  // Success

                                            if (!Files.exists(base_out)) {
                                                error_counter = Int.MAX_VALUE
                                                System.err.println("Base output is removed")
                                                continue@baseLoop
                                            }

                                            // Copy over the created variant
                                            if (!Files.exists(variant_out.parent)) Files.createDirectories(variant_out.parent)
                                            FileUtils.copyDir(variant, variant_out)

                                            // Success
                                            println("Created ${base} L${level} ${trns_level}% $i")
                                            break@creationLoop

                                        } else {    // Invalid variant
                                            throw Exception("Variant is invalid")
                                        }
                                    } else {
                                        throw Exception("No variant returned")
                                    }

                                } catch (e: Exception) {
                                    // Something bad happened - try again
                                    System.err.println("Failed ${base} L${level} ${trns_level}% $i")
                                    System.err.println(e.message)

                                    error_counter++
                                    continue@creationLoop

                                } finally {
                                    // Copy back the synonym db
                                    synchronized(this) {
                                        Files.copy(work.resolve("db.blob"), Paths.get("db.blob"), StandardCopyOption.REPLACE_EXISTING)
                                    }

                                    // Delete the work directory
                                    FileUtils.deleteDirectory(work)
                                }
                            } while (error_counter < 10)

                            if (error_counter >= 10) {
                                System.err.println("ERROR::Terminating ${base} L${level} ${trns_level}% $i")

                                Files.walk(base_out)
                                    .sorted(Comparator.reverseOrder())
                                    .forEach(Files::delete)

                                continue@levelLoop
                            }
                        }
                    }
                }
            }
        }
    }

    private fun makeVariant(work: Path, base: Path, libs: List<Path>, config: ConfigDocument): Path? {
        // SPPlagiarise required directories
        val inp = work.resolve("inp")
        val out = Files.createDirectory(work.resolve("out"))
        val libs = Files.createDirectory(work.resolve("libs"))
        val cfg = work.resolve("config.json").toAbsolutePath()

        // Copy inputs
        FileUtils.copyDir(base, inp)
        for (lib in global_libs)
            Files.copy(lib, libs.resolve(lib.fileName.toString()))

        // Save the config file
        mapper.writeValue(cfg.toFile(), config)

        // Expected location for variant
        val variantRoot = out.resolve("0")

        if (!SPPlagiarise.invoke(cfg, work)) return null
        if (!Files.isDirectory(variantRoot)) return null

        // We have a variant
        return variantRoot
    }

    fun makeConfig(level: Int, transformationChance: Int): ConfigDocument {
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
            transformationChance
        )
    }
}

object SPPlagiarise {
    val exec = Paths.get("testconsole/src/main/resources/spplagiarise/bin/runner").toAbsolutePath().toString()

    fun invoke(config: Path, workingDir: Path): Boolean {
        val proc = ProcessBuilder()
            .command(
                "/bin/sh",
                exec,
                config.toAbsolutePath().toString()
            )
            .directory(workingDir.toFile())
//            .inheritIO()
            .start()

        proc.waitFor()

//        if (proc.exitValue() != 0) {
//            val inp = proc.inputStream
//            val inpl = inp.bufferedReader()
//                .lines()
//                .forEach(System.out::println)
//
//            val err = proc.errorStream
//            val errl = err.bufferedReader()
//                .lines()
////                .forEach(System.err::println)
//
//            Unit
//        }

        return proc.exitValue() == 0
    }
}

object VariantValidator {
    val javac = System.getProperty("java.home") + "/bin/javac"

    fun validateVariant(root: Path, libs: List<Path>): Boolean {
        val sources = Files.walk(root)
            .filter { Files.isRegularFile(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".java") }
            .map { it.toAbsolutePath().toString() }
            .use { it.toList() }

        if (sources.isEmpty())
            return false

        val cp = if (libs.isEmpty()) { emptyArray<String>() }
        else {
            arrayOf(
                "-cp",
                ".:" + libs.joinToString(":") { it.toAbsolutePath().toString() }
            )
        }

        val cmd = arrayOf(javac, *cp) + sources
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

        Files.walk(root)
            .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".class") }
            .forEach {
                Files.delete(it)
            }

        return proc.exitValue() == 0
    }
}