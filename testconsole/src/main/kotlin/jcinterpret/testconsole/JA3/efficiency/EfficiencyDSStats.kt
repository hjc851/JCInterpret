package jcinterpret.testconsole.JA3.efficiency

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

object EfficiencyDSStats {

    val root = Paths.get("/media/haydencheers/Data/SymbExec/src")

    @JvmStatic
    fun main(args: Array<String>) {
        val sets = Files.list(root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }
            .sorted()

        for (ds in sets) {
            val subs = Files.list(ds)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .use { it.toList() }

            var fcount = 0.0
            var lcount = 0.0
            var fcounter = 0.0

            for (sub in subs) {
                val sources = Files.walk(sub)
                    .filter { Files.isRegularFile(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".java") }
                    .use { it.toList() }

                val lineCount = sources.map {
                    Files.lines(it, Charset.forName("ISO-8859-1")).filter { it.isNotEmpty() }.count()
                }.sum().toDouble()

                val sourceCount = sources.size

                fcounter++

                lcount += lineCount
                fcount += sourceCount
            }

            val name = ds.fileName.toString()
            val size = subs.size
            val avgfcount = fcount/fcounter
            val avglcount = lcount/fcounter

            println("${name}\t${size}\t${avgfcount}\t${avglcount}")
        }
    }
}