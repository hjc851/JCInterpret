package jcinterpret.feature

import jcinterpret.feature.extractor.ConditionalFeatureExtractor
import jcinterpret.feature.extractor.DynamicFeatureExtractor
import jcinterpret.feature.featureset.FeatureSetWriter
import jcinterpret.testconsole.features.featureset.FeatureSet
import weka.core.converters.ArffLoader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors

fun main(args: Array<String>) {
    val traceRoot = Paths.get(args[0])
    val graphRoot = Paths.get(args[1])

    val featureOut = Paths.get(args[2])

    Files.deleteIfExists(featureOut)
    Files.createFile(featureOut)

    val workpool = Executors.newFixedThreadPool(28)
    val waitpool = Executors.newFixedThreadPool(12)

    try {
        FeatureSet().use { fs ->
            val extractor = DynamicFeatureExtractor(workpool, waitpool)
            extractor.extract(traceRoot, graphRoot, fs, extractBranching = false, extractTrace = false, extractGraph = true)

            println("Writing feature set ...")
            Files.newBufferedWriter(featureOut).use { out ->
                FeatureSetWriter.write(out, fs) { id -> id.split("-")[0] }
            }

            println("Validating feature set ...")
            val loader = ArffLoader()
            loader.setFile(featureOut.toFile())
            loader.dataSet
        }
    } catch (e: Exception) {
        println("Aborting due to error ...")
        e.printStackTrace()
    }

    workpool.shutdownNow()
    waitpool.shutdownNow()

    println("Finished")
    System.exit(0)
}