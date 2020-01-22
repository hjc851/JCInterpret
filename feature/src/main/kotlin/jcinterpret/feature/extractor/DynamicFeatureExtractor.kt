package jcinterpret.feature.extractor

import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.core.trace.ExecutionTrace
import jcinterpret.document.DocumentUtils
import jcinterpret.graph.condition.ConditionalGraphBuilder
import jcinterpret.graph.condition.RootBranchGraphNode
import jcinterpret.graph.serialization.GraphSerializationAdapter
import jcinterpret.graph.serialization.toGraph
import jcinterpret.testconsole.features.featureset.FeatureSet
import jcinterpret.testconsole.features.featureset.NumericFeature
import org.graphstream.graph.Graph
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.*
import java.util.function.Supplier
import java.util.stream.IntStream
import java.util.stream.Stream
import kotlin.streams.toList

class DynamicFeatureExtractor (
    val pool: ExecutorService
) {
    fun extract (
        traceRoot: Path,
        graphRoot: Path,
        fs: FeatureSet,
        extractBranching: Boolean,
        extractTrace: Boolean,
        extractGraph: Boolean
    ) {
        val waitPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()-4)

        if (extractBranching || extractTrace) {
            val traceProjects = Files.list(traceRoot)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .toList()
                .sortedBy { it.fileName.toString() }

            println("Submitting trace jobs ...")
            val traceFutures = traceProjects.map { project ->
                CompletableFuture.runAsync (Runnable {
                    val id = project.fileName.toString()
                    val traceSetFiles = Files.list(project)
                        .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser") }
                        .use { it.toList() }

                    for (traceSetPath in traceSetFiles) {
                        val eptraces = DocumentUtils.readObject(traceSetPath, EntryPointExecutionTraces::class)
                        val epsig = eptraces.entryPoint.toString().replace("/", ".")
                            .replace("\"", ".")

                        val epid = "$id-$epsig"
                        val epfs = fs.getFeatureSet(epid)

                        val branchf = if (extractBranching) {
                            CompletableFuture.runAsync(Runnable {
                                val records = eptraces.executionTraces.map { it.records }
                                val cgraph = ConditionalGraphBuilder.build(records)

                                val features = featuresForConditionalGraph(cgraph)
                                epfs.addAll(features)
                            }, pool)
                        } else {
                            null
                        }

                        val execf = if (extractTrace) {
                            CompletableFuture.runAsync(Runnable {
                                val allTraceFeatures = Stream.of(*eptraces.executionTraces).parallel()
                                    .map { featuresForTrace(it) }
                                    .toList()

                                val aggregatedFeatures = aggregateFeatures(allTraceFeatures)
                                epfs.addAll(aggregatedFeatures)
                            }, pool)
                        } else {
                            null
                        }

                        branchf?.get()
                        execf?.get()

                        fs.cacheFeatureSet(epid)
                    }

                    println("\tFinished trace for $id")
                }, waitPool)
            }

            println("Awaiting trace jobs to finish")
            traceFutures.forEach { it.get() }
        }

        if (extractGraph) {
            val graphProjects = Files.list(graphRoot)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .toList()
                .sortedBy { it.fileName.toString() }

            println("Submitting graph jobs")
            val graphFutures = graphProjects.map { project ->
                CompletableFuture.runAsync(Runnable {
                    val id = project.fileName.toString()
                    val entryPoints = Files.list(project)
                        .filter { Files.isDirectory(it) || !Files.isHidden(it) }
                        .use { it.toList() }

                    for (entryPoint in entryPoints) {
                        val epsig = entryPoint.fileName.toString()
                        val epid = "$id-$epsig"
                        val epfs = fs.getFeatureSet(epid)

                        val fileCount = Files.list(entryPoint)
                            .filter { Files.isRegularFile(it) && !Files.isHidden(it) }
                            .count()

                        val allGraphFeatures = Collections.synchronizedList(mutableListOf<List<NumericFeature>>())

                        val traceCount = (fileCount - 1) / 5
                        (0 until traceCount).map { i ->
                            CompletableFuture.supplyAsync(Supplier {
                                val graph = DocumentUtils.readObject(entryPoint.resolve("$i-taint.ser"), GraphSerializationAdapter::class).toGraph()
                                val features = featuresForGraph(graph)
                                allGraphFeatures.add(features)
                            }, waitPool)
                        }.forEach { it.get() }

                        val aggregatedGraphFeatures = aggregateFeatures(allGraphFeatures)
                        epfs.addAll(aggregatedGraphFeatures)

                        fs.cacheFeatureSet(epid)
                    }
                }, waitPool)
            }

            println("Awaiting graph futures")
            graphFutures.forEach { it.get()}
        }
    }

    private fun featuresForConditionalGraph(root: RootBranchGraphNode): List<NumericFeature> {
        return listOf(
            NumericFeature("CONDITION_GRAPH_TREE_HEIGHT", root.height()),
            NumericFeature("CONDITION_GRAPH_TREE_NODE_COUNT", root.size()),
            NumericFeature("CONDITION_GRAPH_TREE_TERMINAL_COUNT", root.terminals().count()),
            NumericFeature("CONDITION_GRAPH_TREE_INTERNAL_COUNT", root.internals().count()),
            NumericFeature("CONDITION_GRAPH_CONDITION_COUNT", root.uniqueConditions().count())
        )
    }

    private fun featuresForTrace(trace: ExecutionTrace): List<NumericFeature> {
        TODO()
    }

    private fun featuresForGraph(graph: Graph): List<NumericFeature> {
        TODO()
    }

    private fun aggregateFeatures(allFeatureSets: List<List<NumericFeature>>): List<NumericFeature> {
        val featureNames = mutableSetOf<String>()
        for (set in allFeatureSets) {
            for (feature in set) {
                featureNames.add(feature.name)
            }
        }

        val featureNameMap = featureNames.mapIndexed { index, name -> name to index } .toMap()
        val matrix = Array<DoubleArray>(featureNames.size) { DoubleArray(allFeatureSets.size) }

        IntStream.range(0, allFeatureSets.size)
            .parallel()
            .forEach { i ->
                val featureSet = allFeatureSets[i]
                val indexedFeatureSet = featureSet.map { it.name to it.value.toDouble() }
                    .toMap()

                for ((featureName, rowIndex) in featureNameMap) {
                    matrix[rowIndex][i] = indexedFeatureSet[featureName] ?: 0.0
                }
            }

        val reducedFeatures = mutableListOf<NumericFeature>()
        for ((featureName, rowIndex) in featureNameMap) {
            val scores = matrix[rowIndex]

            val min = scores.min() ?: 0.0
            val max = scores.max() ?: 0.0
            val avg = scores.average()
            val median = scores.median() ?: 0.0
            val stddev = scores.stddev() ?: 0.0
            val variance = scores.variance() ?: 0.0

            reducedFeatures.add(NumericFeature("${featureName}_MIN", min))
            reducedFeatures.add(NumericFeature("${featureName}_MAX", max))
            reducedFeatures.add(NumericFeature("${featureName}_AVG", avg))
            reducedFeatures.add(NumericFeature("${featureName}_MEDIAN", median))
            reducedFeatures.add(NumericFeature("${featureName}_STDDEV", stddev))
            reducedFeatures.add(NumericFeature("${featureName}_VARIANCE", variance))
        }

        return reducedFeatures
    }
}

fun DoubleArray.median(): Double? {
    if (this.isEmpty()) return null

    val sorted = this.sortedArray()
    return if (sorted.size % 2 == 0)
        (sorted[sorted.size/2] + sorted[(sorted.size-1)/2])/2.0
    else
        sorted[sorted.size/2]

}

@Strictfp
fun DoubleArray.stddev(): Double? {
    if (this.isEmpty()) return null

    return Math.sqrt(this.variance() ?: 0.0)
}

@Strictfp
fun DoubleArray.variance(): Double? {
    if (this.isEmpty()) return null

    val sum = this.sum()
    val sumsq = this.sumByDouble { it*it }
    val mean = sum / this.size
    return sumsq/this.size - mean*mean
}