package jcinterpret.testconsole

import com.opencsv.CSVReader
import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    val input = Paths.get("/Users/haydencheers/Desktop/instructions.csv")
    Files.newBufferedReader(input).use {
        val lines = CSVReader(it).readAll()

        lines.forEach { line ->
            val name = line[0]
            val hexcode = line[1]

            println("\"${hexcode}\" to ::${name},")
        }

//        lines.forEach { line ->
//            val name = line[0]
//            val opcode = line[1]
//            val bytes = line[3]
//            val stack = line[4]
//            val description = line[5]
//
//            println("""
//                // ${description}
//                // ${stack}
//                fun ${name}(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
//                    TODO("Not implemented")
//                }
//
//            """.trimIndent())
//        }
    }
}