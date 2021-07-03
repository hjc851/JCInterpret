package jcinterpret.testconsole.plagiariser

import jcinterpret.testconsole.utils.FileUtils
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

object OutputGrouper {

    val BASE = Paths.get("/media/haydencheers/Data/SymbExec/src/algorithms")
    val INPUT = Paths.get("/media/haydencheers/Data/SymbExec/variant_src/algorithms")
    val OUTPUT = Paths.get("/media/haydencheers/Data/SymbExec/Algorithms")

    @JvmStatic
    fun main(args: Array<String>) {
        val bases = Files.list(INPUT)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .map { it.fileName.toString() }
            .use { it.toList() }

        for (base in bases) {
            val base_src = BASE.resolve(base)
            val t20  = INPUT.resolve(base).resolve("L5").resolve("T20").resolve("V1")
            val t40  = INPUT.resolve(base).resolve("L5").resolve("T40").resolve("V1")
            val t60  = INPUT.resolve(base).resolve("L5").resolve("T60").resolve("V1")
            val t80  = INPUT.resolve(base).resolve("L5").resolve("T80").resolve("V1")
            val t100 = INPUT.resolve(base).resolve("L5").resolve("T100").resolve("V1")

            val base_out = OUTPUT.resolve(base)
            val t20_out  = OUTPUT.resolve("${base}-T20")
            val t40_out  = OUTPUT.resolve("${base}-T40")
            val t60_out  = OUTPUT.resolve("${base}-T60")
            val t80_out  = OUTPUT.resolve("${base}-T80")
            val t100_out = OUTPUT.resolve("${base}-T100")

            FileUtils.copyDir(base_src, base_out)
            FileUtils.copyDir(t20, t20_out)
            FileUtils.copyDir(t40, t40_out)
            FileUtils.copyDir(t60, t60_out)
            FileUtils.copyDir(t80, t80_out)
            FileUtils.copyDir(t100, t100_out)
        }
    }
}