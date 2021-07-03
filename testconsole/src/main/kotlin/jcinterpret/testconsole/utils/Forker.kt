package jcinterpret.testconsole.utils

import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object Forker {
    fun exec(
        cls: Class<*>,
        args: Array<String> = emptyArray(),
        props: Array<String> = emptyArray(),
        env: Map<String, String> = emptyMap(),
        waitFor: Long = 4
    ): Int {
        val javaHome = System.getProperty("java.home")
        val javaBin = javaHome + File.separator + "bin" + File.separator + "java"
        val classpath = System.getProperty("java.class.path")
        val className = cls.name

        val command = mutableListOf<String>()
        command.add(javaBin)
        command.addAll(props)
        command.add("-cp")
        command.add(classpath)
        command.add(className)
        command.addAll(args)

        val builder = ProcessBuilder(command)
        env.toMap(builder.environment())
        val process = builder
//            .inheritIO()
            .start()

        if (process.waitFor(waitFor, TimeUnit.MINUTES)) {
            if (process.exitValue() != 0) {
                val inp = process.inputStream
                inp.bufferedReader()
                    .lines()
                    .forEach(System.out::println)

                val err = process.errorStream
                err.bufferedReader()
                    .lines()
                    .forEach(System.err::println)
            }
        } else {
            process.destroyForcibly()
            process.waitFor()

            throw TimeoutException("Exceeded wait period")
        }

        return process.exitValue()
    }

    fun exec(
        cls: Class<*>,
        args: Array<String> = emptyArray(),
        props: Array<String> = emptyArray(),
        env: Map<String, String> = emptyMap(),
        workingDir: Path,
        out: Path,
        err: Path
    ): Process {
        val javaHome = System.getProperty("java.home")
        val javaBin = javaHome + File.separator + "bin" + File.separator + "java"
        val classpath = System.getProperty("java.class.path")
        val className = cls.name

        val command = mutableListOf<String>()
        command.add(javaBin)
        command.addAll(props)
        command.add("-cp")
        command.add(classpath)
        command.add(className)
        command.addAll(args)

        val builder = ProcessBuilder(command)
        env.toMap(builder.environment())

        val process = builder
            .directory(workingDir.toFile())
            .redirectError(err.toFile())
            .redirectOutput(out.toFile())
            .start()

        return process
    }
}