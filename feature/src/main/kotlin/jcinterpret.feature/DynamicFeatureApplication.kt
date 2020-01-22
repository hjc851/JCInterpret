package jcinterpret.feature

import jcinterpret.feature.extractor.ConditionalFeatureExtractor
import jcinterpret.feature.extractor.DynamicFeatureExtractor
import jcinterpret.feature.featureset.FeatureSetWriter
import jcinterpret.testconsole.features.featureset.FeatureSet
import weka.core.converters.ArffLoader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.streams.toList

fun main(args: Array<String>) {
    val traceRoot = Paths.get(args[0])
    val graphRoot = Paths.get(args[1])

    val featureOut = Paths.get(args[2])

    Files.deleteIfExists(featureOut)
    Files.createFile(featureOut)

    val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()-4)

    FeatureSet().use { fs ->
        val extractor = DynamicFeatureExtractor(pool)
        extractor.extract(traceRoot, graphRoot, fs, extractBranching = true, extractTrace = true, extractGraph = true)

        println("Writing feature set ...")
        Files.newBufferedWriter(featureOut).use { out ->
            FeatureSetWriter.write(out, fs)
        }

        println("Validating feature set ...")
        val loader = ArffLoader()
        loader.setFile(featureOut.toFile())
        loader.dataSet
    }

    println("Finished")
    System.exit(0)
}