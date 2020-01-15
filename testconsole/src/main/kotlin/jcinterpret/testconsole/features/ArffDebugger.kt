package jcinterpret.testconsole.features

import weka.core.converters.ArffLoader
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val file = Paths.get(args[0])
    val reader = Files.newBufferedReader(file)

    val out = Files.createTempFile("scrubber", ".arff")
    val writer = Files.newBufferedWriter(out)

    var i = 1
    var line = reader.readLine()
    while (line != null) {
        writer.appendln(line.replace(" ", "").replace("_", ""))

//        print(i)
//        print(": ")
//        print(line)
//        readLine()
//
//        i++
//        line = reader.readLine()
    }

    writer.close()
    reader.close()

    println("Validating file format ...")
    val loader = ArffLoader()
    loader.setFile(out.toFile())
    val instances = loader.dataSet

    println("Copying ...")
    Files.delete(file)
    Files.copy(out, file)
}