package jcinterpret.feature

import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

fun main(args: Array<String>) {
    val file = Paths.get(args[0])

    val lines = Files.lines(file)
        .skip(1370104-2)
        .limit(4)
        .collect(Collectors.toList())

    lines.forEach(System.out::println)

//    val reader = Files.newBufferedReader(file)
//
//    val out = Files.createTempFile("scrubber", ".arff")
//    val writer = Files.newBufferedWriter(out)
//
//    val ff = 12.toChar()
//
//    println("Scrubbing file ...")
//    var i = 1
//    var line = reader.readLine()
//    while (line != null) {
//
//        line = line.replace(ff, '_')
//
//        writer.appendln(line)
//        println(i++)
//
////        print(i)
////        print(": ")
////        print(line)
////        readLine()
////
////        i++
////        line = reader.readLine()
//    }
//
//    writer.close()
//    reader.close()
//
//    println("Validating file format ...")
//    val loader = ArffLoader()
//    loader.setFile(out.toFile())
//    val instances = loader.dataSet
//
//    println("Copying ...")
//    Files.delete(file)
//    Files.copy(out, file)
}