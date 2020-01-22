package jcinterpret.feature.extractor.old

import jcinterpret.feature.extractor.old.GraphFeatureExtractor
import jcinterpret.testconsole.features.featureset.FeatureSet
import jcinterpret.testconsole.features.featureset.NumericFeature
import jcinterpret.testconsole.features.featureset.NumericFeatureDescriptor
import weka.core.converters.ArffLoader
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths
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

    val ff = 12.toChar().toString()
    val nc = '\u0000'.toString()

    FeatureSet().use { fs ->
        // Get the features
        println("GRAPH")
        GraphFeatureExtractor.extract(graphRoot, fs)

        // Save
        Files.newBufferedWriter(featureOut).use { fw ->
            val fout = PrintWriter(fw)
            val featureNames = fs.getFeatureNames().toList()

            val groups = fs.getFeatureSetIds()
                .sorted()
                .groupBy { it.split("-").dropLast(1).joinToString("-") }

            println("Writing headers ...")
            fout.println("@RELATION plagiarismmetrics")
            fout.println()
            fout.printf("@ATTRIBUTE id {%s}\n", groups.keys.joinToString(","))
            featureNames.forEach { name ->
                val desc = fs.getDescriptor(name)

                val name = name.replace("<", "LESS")
                    .replace(">", "GRTR")
                    .replace("[", "LBRACE")
                    .replace("]", "RBRACE")
                    .replace("(", "LPAR")
                    .replace(")", "RPAR")
                    .replace("{", "LBRACKET")
                    .replace("}", "RBRACKET")
                    .replace("/", "DIV")
                    .replace(".", "DOT")
                    .replace(",", "COMA")
                    .replace("#", "HASH")
                    .replace(" ", "SPACE")
                    .replace("%", "PERC")
                    .replace("@", "AT")
                    .replace("+", "PLUS")
                    .replace("-", "MINUS")
                    .replace("=", "EQUALS")
                    .replace("*", "MULTIPLY")
                    .replace("\"", "QUOTE")
                    .replace("'", "SQUOTE")
                    .replace("\n", "NL")
                    .replace("\r", "CF")
                    .replace("\t", "TAB")
                    .replace(ff, "_ff")
                    .replace(nc, "_null")
                    .replace(":", "COLON")
                    .replace(";", "SEMI")

                fout.println("@ATTRIBUTE $name ${desc.descriptor()}")
            }
            fout.printf("@ATTRIBUTE cls {%s}\n", classes.joinToString(","))
            fout.println()
            fout.println("@DATA")

            println("Writing average attributes ...")

            val writeLock = Any()

            groups.toList()
                .parallelStream()
                .forEach { (groupId, setIds) ->
                    val fsets = setIds.map { fs.getFeatureSet(it).features }

                    val buffer = StringBuffer()
                    buffer.append("{0 $groupId,")

                    for (i in 0 until featureNames.count()) {
                        val fname = featureNames[i]
                        val fdesc = fs.getDescriptor(fname)

                        if (fdesc is NumericFeatureDescriptor) {
                            val values = fsets.map { (it.get(fname) as? NumericFeature)?.value?.toDouble() }
                                .filterNotNull()

                            if (values.isNotEmpty()) {
                                val value = values.sum().div(fsets.size.toDouble())

                                buffer.append(String.format("%d %.4f,", i+1, value))
                            }
                        }
                    }

                    val cls = groupId.split("-")[0]
                    buffer.append("${featureNames.size+1} $cls}")

                    synchronized(writeLock) {
                        fout.println(buffer.toString())
                    }

                    setIds.forEach { fs.cacheFeatureSet(it) }
                    println("\t$groupId")
                }
        }

        println("Validating file format ...")
        val loader = ArffLoader()
        loader.setFile(featureOut.toFile())
        val instances = loader.dataSet
    }

    println("Finished")
    System.exit(0)
}