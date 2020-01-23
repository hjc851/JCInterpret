package jcinterpret.feature.extractor

import jcinterpret.core.ctx.meta.HeapArea
import jcinterpret.core.memory.heap.*
import jcinterpret.core.memory.stack.StackReference
import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.core.trace.ExecutionTrace
import jcinterpret.core.trace.TraceRecord
import jcinterpret.document.DocumentUtils
import jcinterpret.graph.condition.ConditionalGraphBuilder
import jcinterpret.graph.condition.RootBranchGraphNode
import jcinterpret.graph.serialization.GraphSerializationAdapter
import jcinterpret.graph.serialization.toGraph
import jcinterpret.testconsole.features.featureset.FeatureSet
import jcinterpret.testconsole.features.featureset.NumericFeature
import jcinterpret.testconsole.pipeline.GraphManifest
import org.graphstream.graph.Graph
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.*
import java.util.function.Supplier
import java.util.stream.IntStream
import java.util.stream.Stream
import kotlin.math.ln
import kotlin.math.log
import kotlin.streams.toList

class DynamicFeatureExtractor (
    val workPool: ExecutorService,
    val waitPool: ExecutorService
) {
    fun extract (
        traceRoot: Path,
        graphRoot: Path,
        fs: FeatureSet,
        extractBranching: Boolean,
        extractTrace: Boolean,
        extractGraph: Boolean
    ) {
        if (extractBranching || extractTrace) {
            val traceProjects = Files.list(traceRoot)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .toList()
                .sortedBy { it.fileName.toString() }
                .take(10)

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
                            }, workPool)
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
                            }, workPool)
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

        if (extractBranching || extractTrace) {
            println("Running GC")
            System.gc()
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

                        val manifest = DocumentUtils.readObject(entryPoint.resolve("manifest.ser"), GraphManifest::class)

                        val allGraphFeatures = Collections.synchronizedList(mutableListOf<List<NumericFeature>>())
                        val traceCount = (fileCount - 1) / 5
                        (0 until traceCount).map { i ->
                            CompletableFuture.supplyAsync(Supplier {
                                val graph = DocumentUtils.readObject(entryPoint.resolve("$i-taint.ser"), GraphSerializationAdapter::class).toGraph()
                                val factor = (manifest.graphWeights["$i"] ?: 1).toDouble()
                                val features = featuresForGraph(graph)
                                features.forEach { it.scale(factor) }
                                allGraphFeatures.add(features)
                            }, workPool)
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
        return listOf (
            NumericFeature("CONDITION_GRAPH_TREE_HEIGHT", root.height()),
            NumericFeature("CONDITION_GRAPH_TREE_NODE_COUNT", root.size()),
            NumericFeature("CONDITION_GRAPH_TREE_TERMINAL_COUNT", root.terminals().count()),
            NumericFeature("CONDITION_GRAPH_TREE_INTERNAL_COUNT", root.internals().count()),
            NumericFeature("CONDITION_GRAPH_CONDITION_COUNT", root.uniqueConditions().count())
        )
    }

    private fun featuresForTrace(trace: ExecutionTrace): List<NumericFeature> {
        val features = mutableListOf<NumericFeature>()

        val records = trace.records

        //
        //  NGrams
        //

        val ngrams = mutableMapOf<String, Int>()
        val accumulator = StringBuffer(10)
        val codeStack = Stack<String>()

        for (i in 0 until records.size-3) {
            val a = records[i]
            val b = records[i+1]
            val c = records[i+2]

            c.accept(TraceRecordCodeVisitor, codeStack)
            b.accept(TraceRecordCodeVisitor, codeStack)
            a.accept(TraceRecordCodeVisitor, codeStack)

            accumulator.append(codeStack.pop())
            accumulator.append(codeStack.pop())
            accumulator.append(codeStack.pop())

            val code = accumulator.toString()
            accumulator.delete(0, accumulator.length)

            ngrams[code] = 1 + (ngrams[code] ?: 0)
        }

        var ngramSum = 0.0
        for ((ngram, count) in ngrams) ngramSum += count.toDouble()

        features.add(NumericFeature("TRACE_TRIGRAM_ALL_COUNT", ngramSum))
        features.add(NumericFeature("TRACE_TRIGRAM_TYPES_COUNT", ngrams.count()))

//        features.add(NumericFeature("TRACE_TRIGRAM_ALL_LOGCOUNT", ln(ngramSum)))
//        features.add(NumericFeature("TRACE_TRIGRAM_TYPES_LOGCOUNT", ln(ngrams.count().toDouble())))

        for ((ngram, count) in ngrams) {
            val perc = count / ngramSum

            features.add(NumericFeature("TRACE_TRIGRAM_${ngram}_COUNT", count))
            features.add(NumericFeature("TRACE_TRIGRAM_${ngram}_PERC", perc))

//            features.add(NumericFeature("TRACE_TRIGRAM_${ngram}_LOGCOUNT", ln(count.toDouble())))
//            features.add(NumericFeature("TRACE_TRIGRAM_${ngram}_LOGPERC", ln(perc)))
        }

        //
        // Record Counts
        //

        val recordCount = records.count().toDouble()
        val recordGroups = records.groupBy { it.javaClass }

        features.add(NumericFeature("TRACE_RECORDS_ALL_COUNT", recordCount))
        features.add(NumericFeature("TRACE_RECORDS_GROUPS_COUNT", recordGroups.size))

//        features.add(NumericFeature("TRACE_RECORDS_ALL_LOGCOUNT", ln(recordCount)))
//        features.add(NumericFeature("TRACE_RECORDS_GROUPS_LOGCOUNT", ln(recordGroups.size.toDouble())))

        for ((type, records) in recordGroups) {
            val count = records.size.toDouble()
            val perc = count / recordCount
            val name = type.simpleName

            features.add(NumericFeature("TRACE_RECORDS_${name}_COUNT", count))
            features.add(NumericFeature("TRACE_RECORDS_${name}_PERC", perc))

//            features.add(NumericFeature("TRACE_RECORDS_${name}_LOGCOUNT", ln(count)))
//            features.add(NumericFeature("TRACE_RECORDS_${name}_LOGPERC", ln(perc)))
        }

        //
        //  Library Method Calls
        //

        val instanceLibraryMethodCalls = records.filterIsInstance<TraceRecord.InstanceLibraryMethodCall>()
        val staticLibraryMethodCalls = records.filterIsInstance<TraceRecord.StaticLibraryMethodCall>()

        val instanceByQSig = instanceLibraryMethodCalls.groupBy { it.method }
        val instanceBySig = instanceLibraryMethodCalls.groupBy { it.method.methodSignature }

        val staticByQSig = staticLibraryMethodCalls.groupBy { it.method }
        val staticBySig = staticLibraryMethodCalls.groupBy { it.method.methodSignature }

        val instanceMethodCallCount = instanceLibraryMethodCalls.size.toDouble()
        val staticMethodCallCount = staticLibraryMethodCalls.size.toDouble()
        val totalMethodCallCount = instanceMethodCallCount + staticMethodCallCount

        features.add(NumericFeature("TRACE_CALL_INSTANCE_COUNT", instanceMethodCallCount))
        features.add(NumericFeature("TRACE_CALL_INSTANCE_PERC", instanceMethodCallCount / totalMethodCallCount.toDouble()))
        features.add(NumericFeature("TRACE_CALL_STATIC_COUNT", staticMethodCallCount))
        features.add(NumericFeature("TRACE_CALL_STATIC_PERC", staticMethodCallCount / totalMethodCallCount.toDouble()))

//        features.add(NumericFeature("TRACE_CALL_INSTANCE_LOGCOUNT", ln(instanceMethodCallCount)))
//        features.add(NumericFeature("TRACE_CALL_INSTANCE_LOGPERC", ln(instanceMethodCallCount / totalMethodCallCount.toDouble())))
//        features.add(NumericFeature("TRACE_CALL_STATIC_LOGCOUNT", ln(staticMethodCallCount)))
//        features.add(NumericFeature("TRACE_CALL_STATIC_LOGPERC", ln(staticMethodCallCount / totalMethodCallCount.toDouble())))

        for ((sig, calls) in instanceByQSig) {
            val perc = calls.size.toDouble() / instanceMethodCallCount
            val percTotal = calls.size.toDouble() / totalMethodCallCount

            features.add(NumericFeature("TRACE_CALL_INSTANCE_QUALIFIED_${sig}_COUNT", calls.size))
            features.add(NumericFeature("TRACE_CALL_INSTANCE_QUALIFIED_${sig}_PERCINSTANCE", perc))
            features.add(NumericFeature("TRACE_CALL_INSTANCE_QUALIFIED_${sig}_PERCTOTAL", percTotal))

//            features.add(NumericFeature("TRACE_CALL_INSTANCE_QUALIFIED_${sig}_LOGCOUNT", ln(calls.size.toDouble())))
//            features.add(NumericFeature("TRACE_CALL_INSTANCE_QUALIFIED_${sig}_LOGPERCINSTANCE", ln(perc)))
//            features.add(NumericFeature("TRACE_CALL_INSTANCE_QUALIFIED_${sig}_LOGPERCTOTAL", ln(percTotal)))
        }

        for ((sig, calls) in instanceBySig) {
            val perc = calls.size.toDouble() / instanceMethodCallCount
            val percTotal = calls.size.toDouble() / totalMethodCallCount

            features.add(NumericFeature("TRACE_CALL_INSTANCE_${sig}_COUNT", calls.size))
            features.add(NumericFeature("TRACE_CALL_INSTANCE_${sig}_PERCINSTANCE", perc))
            features.add(NumericFeature("TRACE_CALL_INSTANCE_${sig}_PERCTOTAL", percTotal))

//            features.add(NumericFeature("TRACE_CALL_INSTANCE_${sig}_LOGCOUNT", ln(calls.size.toDouble())))
//            features.add(NumericFeature("TRACE_CALL_INSTANCE_${sig}_LOGPERCINSTANCE", ln(perc)))
//            features.add(NumericFeature("TRACE_CALL_INSTANCE_${sig}_LOGPERCTOTAL", ln(percTotal)))
        }

        for ((sig, calls) in staticByQSig) {
            val perc = calls.size.toDouble() / staticMethodCallCount
            val percTotal = calls.size.toDouble() / totalMethodCallCount

            features.add(NumericFeature("TRACE_CALL_STATIC_QUALIFIED_${sig}_COUNT", calls.size))
            features.add(NumericFeature("TRACE_CALL_STATIC_QUALIFIED_${sig}_PERCINSTANCE", perc))
            features.add(NumericFeature("TRACE_CALL_STATIC_QUALIFIED_${sig}_PERCTOTAL", percTotal))

//            features.add(NumericFeature("TRACE_CALL_STATIC_QUALIFIED_${sig}_LOGCOUNT", ln(calls.size.toDouble())))
//            features.add(NumericFeature("TRACE_CALL_STATIC_QUALIFIED_${sig}_LOGPERCINSTANCE", ln(perc)))
//            features.add(NumericFeature("TRACE_CALL_STATIC_QUALIFIED_${sig}_LOGPERCTOTAL", ln(percTotal)))
        }

        for ((sig, calls) in staticBySig) {
            val perc = calls.size.toDouble() / staticMethodCallCount
            val percTotal = calls.size.toDouble() / totalMethodCallCount

            features.add(NumericFeature("TRACE_CALL_STATIC_${sig}_COUNT", calls.size))
            features.add(NumericFeature("TRACE_CALL_STATIC_${sig}_PERCINSTANCE", perc))
            features.add(NumericFeature("TRACE_CALL_STATIC_${sig}_PERCTOTAL", percTotal))

//            features.add(NumericFeature("TRACE_CALL_STATIC_${sig}_LOGCOUNT", ln(calls.size.toDouble())))
//            features.add(NumericFeature("TRACE_CALL_STATIC_${sig}_LOGPERCINSTANCE", ln(perc)))
//            features.add(NumericFeature("TRACE_CALL_STATIC_${sig}_LOGPERCTOTAL", ln(percTotal)))
        }

        //
        //  Heap Object/Array Allocations
        //

        val heap = trace.heapArea
        val defaultStaticValues = records.filterIsInstance<TraceRecord.DefaultStaticFieldValue>()
            .map { it.value }
            .toSet()

        // Total heap count
        features.add(NumericFeature("HEAP_COUNT", heap.storage.count()))

        // Count of each non-boxed type
        features.addAll (
            NumericFeature("HEAP_CONCRETE_OBJECT_COUNT", heap.storage.values.count { it is ConcreteObject }),
            NumericFeature("HEAP_SYMBOLIC_OBJECT_COUNT", heap.storage.values.count { it is SymbolicObject }),
            NumericFeature("HEAP_ARRAY_OBJECT_COUNT", heap.storage.values.count { it is SymbolicArray }),
            NumericFeature("HEAP_CLASS_LIT_OBJECT_COUNT", heap.storage.values.count { it is ClassObject })
        )

        // String
        features.addAll (
            NumericFeature("HEAP_STRING_COUNT", heap.storage.values.count { it is BoxedStringObject }),
            NumericFeature("HEAP_CONCRETE_STRING_COUNT", heap.storage.values.count { it is BoxedStringObject && it.value is ConcreteStringValue }),
            NumericFeature("HEAP_SYMBOLIC_STRING_COUNT", heap.storage.values.count { it is BoxedStringObject && it.value is SymbolicStringValue }),
            NumericFeature("HEAP_STRINGIFIED_STACK_VALUE_COUNT", heap.storage.values.count { it is BoxedStringObject && it.value is StackValueStringValue }),
            NumericFeature("HEAP_COMPOSITE_STACK_VALUE_COUNT", heap.storage.values.count { it is BoxedStringObject && it.value is CompositeStringValue })
        )

        // Boxed Value
        features.add(NumericFeature("HEAP_BOXED_COUNT", heap.storage.values.count { it is BoxedStackValueObject }))
        features.addAll (
            heap.storage.values.filterIsInstance<BoxedStackValueObject>()
                .groupBy { it.value.type }
                .map { NumericFeature("HEAP_BOXED_${it.key.name}_COUNT", it.value.size) }
        )

        // Objects of each type
        features.addAll (
            heap.storage.values.filterIsInstance<ConcreteObject>()
                .map { if (it.type.toString().contains("Ljava")) it.type else "_ReferenceType_" }
                .map { "HEAP_CONCRETE_OBJECT_${it}_COUNT" to it }
                .groupBy { it.first }
                .map { NumericFeature(it.key, it.value.count()) }
        )

        features.addAll (
            heap.storage.values.filterIsInstance<SymbolicObject>()
                .filter { !defaultStaticValues.contains(it.ref()) }
                .map { if (it.type.toString().contains("Ljava")) it.type else "_ReferenceType_" }
                .map { "HEAP_SYMBOLIC_OBJECT_${it}_COUNT" to it }
                .groupBy { it.first }
                .map { NumericFeature(it.key, it.value.count()) }
        )

        features.addAll (
            heap.storage.values.filterIsInstance<SymbolicArray>()
                .filter { !defaultStaticValues.contains(it.ref()) }
                .map { if (it.type.toString().contains("Ljava")) it.type else "_ReferenceType_" }
                .map { "HEAP_ARRAY_OBJECT_${it}_COUNT" to it }
                .groupBy { it.first }
                .map { NumericFeature(it.key, it.value.count()) }
        )

        return features
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

object TraceRecordCodeVisitor: TraceRecord.Visitor<Stack<String>>() {
    override fun visit(record: TraceRecord.EntryMethod, arg: Stack<String>) {
        arg.push("EM")
    }

    override fun visit(record: TraceRecord.EntryScope, arg: Stack<String>) {
        arg.push("Es")
    }

    override fun visit(record: TraceRecord.EntryParameter, arg: Stack<String>) {
        arg.push("Ep")
    }

    override fun visit(record: TraceRecord.StaticLibraryMethodCall, arg: Stack<String>) {
        arg.push("Sm")
    }

    override fun visit(record: TraceRecord.InstanceLibraryMethodCall, arg: Stack<String>) {
        arg.push("Im")
    }

    override fun visit(record: TraceRecord.SynthesisedReturnValue, arg: Stack<String>) {
        arg.push("Xr")
    }

    override fun visit(record: TraceRecord.StaticFieldPut, arg: Stack<String>) {
        arg.push("Sp")
    }

    override fun visit(record: TraceRecord.ObjectFieldPut, arg: Stack<String>) {
        arg.push("Op")
    }

    override fun visit(record: TraceRecord.ArrayMemberPut, arg: Stack<String>) {
        arg.push("Ap")
    }

    override fun visit(record: TraceRecord.ArrayMemberGet, arg: Stack<String>) {
        arg.push("Ag")
    }

    override fun visit(record: TraceRecord.DefaultInstanceFieldValue, arg: Stack<String>) {
        arg.push("Di")
    }

    override fun visit(record: TraceRecord.DefaultStaticFieldValue, arg: Stack<String>) {
        arg.push("Ds")
    }

    override fun visit(record: TraceRecord.StackTransformation, arg: Stack<String>) {
        arg.push("St")
    }

    override fun visit(record: TraceRecord.NotValueTransformation, arg: Stack<String>) {
        arg.push("N")
    }

    override fun visit(record: TraceRecord.StackCast, arg: Stack<String>) {
        arg.push("Sc")
    }

    override fun visit(record: TraceRecord.StringConcat, arg: Stack<String>) {
        arg.push("Stc")
    }

    override fun visit(record: TraceRecord.Stringification, arg: Stack<String>) {
        arg.push("Stf")
    }

    override fun visit(record: TraceRecord.Assertion, arg: Stack<String>) {
        arg.push("A")
    }

    override fun visit(record: TraceRecord.Halt, arg: Stack<String>) {
        arg.push("H")
    }

    override fun visit(record: TraceRecord.UncaughtException, arg: Stack<String>) {
        arg.push("Ue")
    }
}

fun <T> MutableList<T>.addAll(vararg items: T) = this.addAll(items)
