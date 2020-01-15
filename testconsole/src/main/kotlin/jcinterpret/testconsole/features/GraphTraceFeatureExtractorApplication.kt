package jcinterpret.testconsole.features

import jcinterpret.testconsole.features.extractor.ConditionalFeatureExtractor
import jcinterpret.testconsole.features.extractor.GraphFeatureExtractor
import jcinterpret.testconsole.features.extractor.TraceFeatureExtractor
import jcinterpret.testconsole.features.featureset.FeatureSet
import weka.core.converters.ArffLoader
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.streams.toList

fun main(args: Array<String>) {
    val traceRoot = Paths.get(args[0])
    val graphRoot = Paths.get(args[1])
    val featureOut = Paths.get(args[2])

    if (Files.exists(featureOut)) {
        Files.delete(featureOut)
        Files.createFile(featureOut)
    }

    val ids = Files.list(traceRoot)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .map { it.fileName.toString() }
        .toList()

    val classes = ids.map { it.split("-")[0] }
        .toSet()

    FeatureSet().use { fs ->
//        println("TRACE")
//        TraceFeatureExtractor.extract(traceRoot, fs)

        println("GRAPH")
        GraphFeatureExtractor.extract(graphRoot, fs)

        // Save
        Files.newBufferedWriter(featureOut).use { fw ->
            val fout = PrintWriter(fw)
            val featureNames = fs.getFeatureNames().toList()

            println("Writing headers ...")
            fout.println("@RELATION plagiarismmetrics")
            fout.println()
            fout.printf("@ATTRIBUTE id {%s}\n", fs.getFeatureSetIds().joinToString(","))
            featureNames.forEach { name ->
                val desc = fs.getDescriptor(name)

                val name = name.replace("<", "LESS")
                    .replace(">", "GRTR")
                    .replace("[", "LBRACE")
                    .replace("]", "RBRACE")
                    .replace("(", "LPAR")
                    .replace(")", "RPAR")
                    .replace("/", "DIV")
                    .replace(".", "DOT")
                    .replace(",", "COMA").replace("#", "HASH")
                    .replace(" ", "SPACE")
                    .replace("%", "PERC")
                    .replace("@", "AT")
                    .replace("+", "PLUS")
                    .replace("-", "MINUS")
                    .replace("=", "EQUALS")
                    .replace("*", "MULTIPLY")

                fout.println("@ATTRIBUTE $name ${desc.descriptor()}")
            }
            fout.printf("@ATTRIBUTE cls {%s}\n", classes.joinToString(","))
            fout.println()
            fout.println("@DATA")

            println("Writing attributes ...")
            fs.getFeatureSetIds().forEachIndexed { index, id ->
                println("\t$id - ${index+1} of ${fs.getFeatureSetIds().count()}")
                val pfs = fs.getFeatureSet(id)
                val features = pfs.features()

                fout.print(id)

                if (featureNames.isNotEmpty()) {
                    fout.print(',')

                    for (i in 0 until featureNames.count()) {
                        val name = featureNames[i]

                        val desc = fs.getDescriptor(name)
                        val feature = features[name]

                        if (feature != null) fout.print(feature.value)
                        else fout.print(desc.defaultValue())

                        if (i < featureNames.count()-1) fout.print(',')
                    }
                }

                val cls = id.split("-")[0]
                fout.print(",$cls")

                fout.println()
                fs.cacheFeatureSet(id)
                System.gc()
            }
        }

        println("Validating file format ...")
        val loader = ArffLoader()
        loader.setFile(featureOut.toFile())
        val instances = loader.dataSet
    }

    println("Finished")
}