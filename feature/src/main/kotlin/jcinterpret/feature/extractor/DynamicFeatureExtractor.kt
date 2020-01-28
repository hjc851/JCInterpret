package jcinterpret.feature.extractor

import jcinterpret.core.ctx.meta.HeapArea
import jcinterpret.core.memory.heap.*
import jcinterpret.core.memory.stack.Operator
import jcinterpret.core.memory.stack.StackReference
import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.core.trace.ExecutionTrace
import jcinterpret.core.trace.TraceRecord
import jcinterpret.document.DocumentUtils
import jcinterpret.graph.*
import jcinterpret.graph.condition.ConditionalGraphBuilder
import jcinterpret.graph.condition.RootBranchGraphNode
import jcinterpret.graph.execution.EdgeType
import jcinterpret.graph.execution.NodeAttributeKeys
import jcinterpret.graph.execution.NodeType
import jcinterpret.graph.serialization.GraphSerializationAdapter
import jcinterpret.graph.serialization.toGraph
import jcinterpret.signature.*
import jcinterpret.testconsole.features.featureset.FeatureSet
import jcinterpret.testconsole.features.featureset.NumericFeature
import jcinterpret.testconsole.pipeline.GraphManifest
import org.graphstream.graph.Edge
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
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
            .filter { it !is TraceRecord.DefaultStaticFieldValue }

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

//        features.add(NumericFeature("TRACE_TRIGRAM_ALL_COUNT", ngramSum))
//        features.add(NumericFeature("TRACE_TRIGRAM_TYPES_COUNT", ngrams.count()))

//        features.add(NumericFeature("TRACE_TRIGRAM_ALL_LOGCOUNT", ln(ngramSum)))
//        features.add(NumericFeature("TRACE_TRIGRAM_TYPES_LOGCOUNT", ln(ngrams.count().toDouble())))

        for ((ngram, count) in ngrams) {
            val perc = count / ngramSum

//            features.add(NumericFeature("TRACE_TRIGRAM_${ngram}_COUNT", count))
            features.add(NumericFeature("TRACE_TRIGRAM_${ngram}_PERC", perc))

//            features.add(NumericFeature("TRACE_TRIGRAM_${ngram}_LOGCOUNT", ln(count.toDouble())))
//            features.add(NumericFeature("TRACE_TRIGRAM_${ngram}_LOGPERC", ln(perc)))
        }

        //
        // Record Counts
        //

        val recordCount = records.count().toDouble()
        val recordGroups = records.groupBy { it.javaClass }

//        features.add(NumericFeature("TRACE_RECORDS_ALL_COUNT", recordCount))
//        features.add(NumericFeature("TRACE_RECORDS_GROUPS_COUNT", recordGroups.size))

//        features.add(NumericFeature("TRACE_RECORDS_ALL_LOGCOUNT", ln(recordCount)))
//        features.add(NumericFeature("TRACE_RECORDS_GROUPS_LOGCOUNT", ln(recordGroups.size.toDouble())))

        for ((type, records) in recordGroups) {
            val count = records.size.toDouble()
            val perc = count / recordCount
            val name = type.simpleName

//            features.add(NumericFeature("TRACE_RECORDS_${name}_COUNT", count))
            features.add(NumericFeature("TRACE_RECORDS_${name}_PERC", perc))

//            features.add(NumericFeature("TRACE_RECORDS_${name}_LOGCOUNT", ln(count)))
//            features.add(NumericFeature("TRACE_RECORDS_${name}_LOGPERC", ln(perc)))
        }

        //
        //  Library Method Calls
        //

        val instanceLibraryMethodCalls = records.filterIsInstance<TraceRecord.InstanceLibraryMethodCall>()
            .filter { it.method.declaringClassSignature.className.startsWith("java/") }
        val staticLibraryMethodCalls = records.filterIsInstance<TraceRecord.StaticLibraryMethodCall>()
            .filter { it.method.declaringClassSignature.className.startsWith("java/") }

        val instanceByQSig = instanceLibraryMethodCalls.groupBy { it.method }
        val instanceBySig = instanceLibraryMethodCalls.groupBy { it.method.methodSignature }

        val staticByQSig = staticLibraryMethodCalls.groupBy { it.method }
        val staticBySig = staticLibraryMethodCalls.groupBy { it.method.methodSignature }

        val instanceMethodCallCount = instanceLibraryMethodCalls.size.toDouble()
        val staticMethodCallCount = staticLibraryMethodCalls.size.toDouble()
        val totalMethodCallCount = instanceMethodCallCount + staticMethodCallCount

//        features.add(NumericFeature("TRACE_CALL_INSTANCE_COUNT", instanceMethodCallCount))
        //        features.add(NumericFeature("TRACE_CALL_STATIC_COUNT", staticMethodCallCount))

        features.add(NumericFeature("TRACE_CALL_INSTANCE_PERC", instanceMethodCallCount / totalMethodCallCount.toDouble()))
        features.add(NumericFeature("TRACE_CALL_STATIC_PERC", staticMethodCallCount / totalMethodCallCount.toDouble()))

//        features.add(NumericFeature("TRACE_CALL_INSTANCE_LOGCOUNT", ln(instanceMethodCallCount)))
//        features.add(NumericFeature("TRACE_CALL_INSTANCE_LOGPERC", ln(instanceMethodCallCount / totalMethodCallCount.toDouble())))
//        features.add(NumericFeature("TRACE_CALL_STATIC_LOGCOUNT", ln(staticMethodCallCount)))
//        features.add(NumericFeature("TRACE_CALL_STATIC_LOGPERC", ln(staticMethodCallCount / totalMethodCallCount.toDouble())))

        for ((sig, calls) in instanceByQSig) {
            val perc = calls.size.toDouble() / instanceMethodCallCount
            val percTotal = calls.size.toDouble() / totalMethodCallCount

//            features.add(NumericFeature("TRACE_CALL_INSTANCE_QUALIFIED_${sig}_COUNT", calls.size))
            features.add(NumericFeature("TRACE_CALL_INSTANCE_QUALIFIED_${sig}_PERCINSTANCE", perc))
            features.add(NumericFeature("TRACE_CALL_INSTANCE_QUALIFIED_${sig}_PERCTOTAL", percTotal))

//            features.add(NumericFeature("TRACE_CALL_INSTANCE_QUALIFIED_${sig}_LOGCOUNT", ln(calls.size.toDouble())))
//            features.add(NumericFeature("TRACE_CALL_INSTANCE_QUALIFIED_${sig}_LOGPERCINSTANCE", ln(perc)))
//            features.add(NumericFeature("TRACE_CALL_INSTANCE_QUALIFIED_${sig}_LOGPERCTOTAL", ln(percTotal)))
        }

        for ((sig, calls) in instanceBySig) {
            val perc = calls.size.toDouble() / instanceMethodCallCount
            val percTotal = calls.size.toDouble() / totalMethodCallCount

//            features.add(NumericFeature("TRACE_CALL_INSTANCE_${sig}_COUNT", calls.size))
            features.add(NumericFeature("TRACE_CALL_INSTANCE_${sig}_PERCINSTANCE", perc))
            features.add(NumericFeature("TRACE_CALL_INSTANCE_${sig}_PERCTOTAL", percTotal))

//            features.add(NumericFeature("TRACE_CALL_INSTANCE_${sig}_LOGCOUNT", ln(calls.size.toDouble())))
//            features.add(NumericFeature("TRACE_CALL_INSTANCE_${sig}_LOGPERCINSTANCE", ln(perc)))
//            features.add(NumericFeature("TRACE_CALL_INSTANCE_${sig}_LOGPERCTOTAL", ln(percTotal)))
        }

        for ((sig, calls) in staticByQSig) {
            val perc = calls.size.toDouble() / staticMethodCallCount
            val percTotal = calls.size.toDouble() / totalMethodCallCount

//            features.add(NumericFeature("TRACE_CALL_STATIC_QUALIFIED_${sig}_COUNT", calls.size))
            features.add(NumericFeature("TRACE_CALL_STATIC_QUALIFIED_${sig}_PERCINSTANCE", perc))
            features.add(NumericFeature("TRACE_CALL_STATIC_QUALIFIED_${sig}_PERCTOTAL", percTotal))

//            features.add(NumericFeature("TRACE_CALL_STATIC_QUALIFIED_${sig}_LOGCOUNT", ln(calls.size.toDouble())))
//            features.add(NumericFeature("TRACE_CALL_STATIC_QUALIFIED_${sig}_LOGPERCINSTANCE", ln(perc)))
//            features.add(NumericFeature("TRACE_CALL_STATIC_QUALIFIED_${sig}_LOGPERCTOTAL", ln(percTotal)))
        }

        for ((sig, calls) in staticBySig) {
            val perc = calls.size.toDouble() / staticMethodCallCount
            val percTotal = calls.size.toDouble() / totalMethodCallCount

//            features.add(NumericFeature("TRACE_CALL_STATIC_${sig}_COUNT", calls.size))
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
        val heapSize = heap.storage.size.toDouble()
        val defaultStaticValues = records.filterIsInstance<TraceRecord.DefaultStaticFieldValue>()
            .map { it.value }
            .toSet()

        // Total heap count
//        features.add(NumericFeature("HEAP_COUNT", heap.storage.count()))

        // Count of each non-boxed type
//        features.addAll (
//            NumericFeature("HEAP_CONCRETE_OBJECT_COUNT", heap.storage.values.count { it is ConcreteObject }),
//            NumericFeature("HEAP_SYMBOLIC_OBJECT_COUNT", heap.storage.values.count { it is SymbolicObject }),
//            NumericFeature("HEAP_ARRAY_OBJECT_COUNT", heap.storage.values.count { it is SymbolicArray }),
//            NumericFeature("HEAP_CLASS_LIT_OBJECT_COUNT", heap.storage.values.count { it is ClassObject })
//        )

        features.addAll (
            NumericFeature("HEAP_CONCRETE_OBJECT_PERC", heap.storage.values.count { it is ConcreteObject } / heapSize),
            NumericFeature("HEAP_SYMBOLIC_OBJECT_PERC", heap.storage.values.count { it is SymbolicObject } / heapSize),
            NumericFeature("HEAP_ARRAY_OBJECT_PERC", heap.storage.values.count { it is SymbolicArray } / heapSize),
            NumericFeature("HEAP_CLASS_LIT_OBJECT_PERC", heap.storage.values.count { it is ClassObject } / heapSize)
        )

        // String
//        features.addAll (
//            NumericFeature("HEAP_STRING_COUNT", heap.storage.values.count { it is BoxedStringObject }),
//            NumericFeature("HEAP_CONCRETE_STRING_COUNT", heap.storage.values.count { it is BoxedStringObject && it.value is ConcreteStringValue }),
//            NumericFeature("HEAP_SYMBOLIC_STRING_COUNT", heap.storage.values.count { it is BoxedStringObject && it.value is SymbolicStringValue }),
//            NumericFeature("HEAP_STRINGIFIED_STACK_VALUE_COUNT", heap.storage.values.count { it is BoxedStringObject && it.value is StackValueStringValue }),
//            NumericFeature("HEAP_COMPOSITE_STACK_VALUE_COUNT", heap.storage.values.count { it is BoxedStringObject && it.value is CompositeStringValue })
//        )

        features.addAll (
            NumericFeature("HEAP_STRING_PERC", heap.storage.values.count { it is BoxedStringObject }),
            NumericFeature("HEAP_CONCRETE_STRING_PERC", heap.storage.values.count { it is BoxedStringObject && it.value is ConcreteStringValue } / heapSize),
            NumericFeature("HEAP_SYMBOLIC_STRING_PERC", heap.storage.values.count { it is BoxedStringObject && it.value is SymbolicStringValue } / heapSize),
            NumericFeature("HEAP_STRINGIFIED_STACK_VALUE_PERC", heap.storage.values.count { it is BoxedStringObject && it.value is StackValueStringValue } / heapSize),
            NumericFeature("HEAP_COMPOSITE_STACK_VALUE_PERC", heap.storage.values.count { it is BoxedStringObject && it.value is CompositeStringValue } / heapSize)
        )


        // Boxed Value
//        features.add(NumericFeature("HEAP_BOXED_COUNT", heap.storage.values.count { it is BoxedStackValueObject }))
//        features.addAll (
//            heap.storage.values.filterIsInstance<BoxedStackValueObject>()
//                .groupBy { it.value.type }
//                .map { NumericFeature("HEAP_BOXED_${it.key.name}_COUNT", it.value.size) }
//        )

        features.add(NumericFeature("HEAP_BOXED_PERC", heap.storage.values.count { it is BoxedStackValueObject } / heapSize))
        features.addAll (
            heap.storage.values.filterIsInstance<BoxedStackValueObject>()
                .groupBy { it.value.type }
                .map { NumericFeature("HEAP_BOXED_${it.key.name}_PERC", it.value.size / heapSize) }
        )


        // Objects of each type
//        features.addAll (
//            heap.storage.values.filterIsInstance<ConcreteObject>()
//                .map { if (it.type.toString().contains("Ljava")) it.type else "_ReferenceType_" }
//                .map { "HEAP_CONCRETE_OBJECT_${it}_COUNT" to it }
//                .groupBy { it.first }
//                .map { NumericFeature(it.key, it.value.count()) }
//        )

//        features.addAll (
//            heap.storage.values.filterIsInstance<SymbolicObject>()
//                .filter { !defaultStaticValues.contains(it.ref()) }
//                .map { if (it.type.toString().contains("Ljava")) it.type else "_ReferenceType_" }
//                .map { "HEAP_SYMBOLIC_OBJECT_${it}_COUNT" to it }
//                .groupBy { it.first }
//                .map { NumericFeature(it.key, it.value.count()) }
//        )

//        features.addAll (
//            heap.storage.values.filterIsInstance<SymbolicArray>()
//                .filter { !defaultStaticValues.contains(it.ref()) }
//                .map { if (it.type.toString().contains("Ljava")) it.type else "_ReferenceType_" }
//                .map { "HEAP_ARRAY_OBJECT_${it}_COUNT" to it }
//                .groupBy { it.first }
//                .map { NumericFeature(it.key, it.value.count()) }
//        )

        features.addAll (
            heap.storage.values.filterIsInstance<ConcreteObject>()
                .map { if (it.type.toString().contains("Ljava")) it.type else "_ReferenceType_" }
                .map { "HEAP_CONCRETE_OBJECT_${it}_PERC" to it }
                .groupBy { it.first }
                .map { NumericFeature(it.key, it.value.count() / heapSize) }
        )

        features.addAll (
            heap.storage.values.filterIsInstance<SymbolicObject>()
                .filter { !defaultStaticValues.contains(it.ref()) }
                .map { if (it.type.toString().contains("Ljava")) it.type else "_ReferenceType_" }
                .map { "HEAP_SYMBOLIC_OBJECT_${it}_PERC" to it }
                .groupBy { it.first }
                .map { NumericFeature(it.key, it.value.count() / heapSize) }
        )

        features.addAll (
            heap.storage.values.filterIsInstance<SymbolicArray>()
                .filter { !defaultStaticValues.contains(it.ref()) }
                .map { if (it.type.toString().contains("Ljava")) it.type else "_ReferenceType_" }
                .map { "HEAP_ARRAY_OBJECT_${it}_PERC" to it }
                .groupBy { it.first }
                .map { NumericFeature(it.key, it.value.count() / heapSize) }
        )

        return features
    }

    /*
    % data
    % operators
    % ...
    % ratios of key types

    Neighborhoods around operators + calls

    Spans
     */

    private fun featuresForGraph(graph: Graph): List<NumericFeature> {
        val features = mutableListOf<NumericFeature>()

        //
        // Node Stats
        //
        val nodes = graph.getNodeSet<Node>()
        val nodeCount = nodes.count().toDouble()

        val operators = nodes.filter { it.isOperator() }
        val methodCalls = nodes.filter { it.isMethodCall() }
        val data = nodes.filter { it.isData() }
        val datacount = data.count().toDouble()

        val objects = nodes.filter { it.isObject() }
        val values = nodes.filter { it.isValue() }
        val strings = nodes.filter { it.isString() }
        val objectcount = objects.count().toDouble()
        val valuecount = values.count().toDouble()
        val stringcount = strings.count().toDouble()

        val symbolic = nodes.filter { it.isSymbolic() }
        val synthetic = nodes.filter { it.isSynthetic() }
        val concrete = nodes.filter { it.isConcrete() }
        val symboliccount = symbolic.count().toDouble()
        val syntheticcount = synthetic.count().toDouble()
        val concretecount = concrete.count().toDouble()

        // Raw Counts
//        features.addAll(
//            NumericFeature("GRAPH_NODE_TOTAL_COUNT", nodeCount),
//
//            NumericFeature("GRAPH_NODE_OPERATOR_COUNT", operators.count()),
//            NumericFeature("GRAPH_NODE_METHODCALL_COUNT", methodCalls.count()),
//            NumericFeature("GRAPH_NODE_DATA_COUNT", data.count()),
//
//            NumericFeature("GRAPH_NODE_DATA_OBJECTS_COUNT", objects.count()),
//            NumericFeature("GRAPH_NODE_DATA_VALUES_COUNT", values.count()),
//            NumericFeature("GRAPH_NODE_DATA_STRING_COUNT", strings.count()),
//
//            NumericFeature("GRAPH_NODE_DATA_SYMBOLIC_COUNT", symbolic.count()),
//            NumericFeature("GRAPH_NODE_DATA_SYNTHETIC_COUNT", synthetic.count()),
//            NumericFeature("GRAPH_NODE_DATA_CONCRETE_COUNT", concrete.count())
//        )

        // Percentages
        features.addAll(
            NumericFeature("GRAPH_NODE_OPERATORS_PERC", operators.count() / nodeCount),
            NumericFeature("GRAPH_NODE_METHODCALL_PERC", methodCalls.count() / nodeCount),
            NumericFeature("GRAPH_NODE_DATA_PERC", data.count() / nodeCount),

            NumericFeature("GRAPH_NODE_DATA_OBJECT_PERC", objects.count() / datacount),
            NumericFeature("GRAPH_NODE_DATA_VALUE_PERC", values.count() / datacount),
            NumericFeature("GRAPH_NODE_DATA_STRING_PERC", strings.count() / datacount),

            NumericFeature("GRAPH_NODE_DATA_SYMBOLIC_PERC", symbolic.count() / datacount),
            NumericFeature("GRAPH_NODE_DATA_SYNTHETIC_PERC", synthetic.count() / datacount),
            NumericFeature("GRAPH_NODE_DATA_CONCRETE_PERC", concrete.count() / datacount)
        )

        // Ratios
        features.addAll(
            NumericFeature("GRAPH_NODE_DATA_RATIO_OBJECT_VALUE", objectcount / valuecount),
            NumericFeature("GRAPH_NODE_DATA_RATIO_OBJECT_STRING", objectcount / stringcount),
            NumericFeature("GRAPH_NODE_DATA_RATIO_VALUE_OBJECT", valuecount / objectcount),
            NumericFeature("GRAPH_NODE_DATA_RATIO_VALUE_STRING", valuecount / stringcount),
            NumericFeature("GRAPH_NODE_DATA_RATIO_STRING_OBJECT", stringcount / objectcount),
            NumericFeature("GRAPH_NODE_DATA_RATIO_STRING_VALUE", stringcount / valuecount),

            NumericFeature("GRAPH_NODE_DATA_RATIO_SYMBOLIC_SYNTHETIC", symboliccount / syntheticcount),
            NumericFeature("GRAPH_NODE_DATA_RATIO_SYMBOLIC_CONCRETE", symboliccount / concretecount),
            NumericFeature("GRAPH_NODE_DATA_RATIO_SYNTHETIC_SYMBOLIC", syntheticcount / symboliccount),
            NumericFeature("GRAPH_NODE_DATA_RATIO_SYNTHETIC_CONCRETE", syntheticcount / concretecount),
            NumericFeature("GRAPH_NODE_DATA_RATIO_CONCRETE_SYMBOLIC", concretecount / symboliccount),
            NumericFeature("GRAPH_NODE_DATA_RATIO_CONCRETE_SYNTHETIC", concretecount / syntheticcount)
        )

        //
        // Edge Stats
        //
        val edges = graph.getEdgeSet<Edge>()
        val edgeCount = edges.count()

        val transformations = edges.filter { it.isTransformation() }
        val aggregations = edges.filter { it.isAggregation() }
        val parameters = edges.filter { it.isParameter() }
        val scope = edges.filter { it.isScope() }
        val supplies = edges.filter { it.isSupplies() }

        val transformationcount = transformations.count().toDouble()
        val aggregationcount = aggregations.count().toDouble()
        val parametercount = parameters.count().toDouble()
        val scopecount = scope.count().toDouble()
        val suppliescount = supplies.count().toDouble()

        // raw counts
//        features.addAll(
//            NumericFeature("GRAPH_EDGE_TOTAL_COUNT", edgeCount),
//
//            NumericFeature("GRAPH_EDGE_TRANSFORMATION_COUNT", transformationcount),
//            NumericFeature("GRAPH_EDGE_AGGREGATION_COUNT", aggregationcount),
//            NumericFeature("GRAPH_EDGE_PARAMETER_COUNT", parametercount),
//            NumericFeature("GRAPH_EDGE_SCOPE_COUNT", scopecount),
//            NumericFeature("GRAPH_EDGE_SUPPLIES_COUNT", suppliescount)
//        )

        // percentages
        features.addAll(
            NumericFeature("GRAPH_EDGE_TRANSFORMATION_PERC", transformationcount / edgeCount),
            NumericFeature("GRAPH_EDGE_AGGREGATION_PERC", aggregationcount / edgeCount),
            NumericFeature("GRAPH_EDGE_PARAMETER_PERC", parametercount / edgeCount),
            NumericFeature("GRAPH_EDGE_SCOPE_PERC", scopecount / edgeCount),
            NumericFeature("GRAPH_EDGE_SUPPLIES_PERC", suppliescount / edgeCount)
        )

        // ratios
        features.addAll(
            NumericFeature("GRAPH_EDGE_RATIO_TRANSFORMATION_AGGREGATION", transformationcount / aggregationcount),
            NumericFeature("GRAPH_EDGE_RATIO_TRANSFORMATION_PARAMETER", transformationcount / parametercount),
            NumericFeature("GRAPH_EDGE_RATIO_TRANSFORMATION_SCOPE", transformationcount / scopecount),
            NumericFeature("GRAPH_EDGE_RATIO_TRANSFORMATION_SUPPLIES", transformationcount / suppliescount),

            NumericFeature("GRAPH_EDGE_RATIO_AGGREGATION_TRANSFORMATION", aggregationcount / transformationcount),
            NumericFeature("GRAPH_EDGE_RATIO_AGGREGATION_PARAMETER", aggregationcount / parametercount),
            NumericFeature("GRAPH_EDGE_RATIO_AGGREGATION_SCOPE", aggregationcount / scopecount),
            NumericFeature("GRAPH_EDGE_RATIO_AGGREGATION_SUPPLIES", aggregationcount / suppliescount),

            NumericFeature("GRAPH_EDGE_RATIO_PARAMETER_TRANSFORMATION", parametercount / transformationcount),
            NumericFeature("GRAPH_EDGE_RATIO_PARAMETER_AGGREGATION", parametercount / aggregationcount),
            NumericFeature("GRAPH_EDGE_RATIO_PARAMETER_SCOPE", parametercount / scopecount),
            NumericFeature("GRAPH_EDGE_RATIO_PARAMETER_SUPPLIES", parametercount / suppliescount),

            NumericFeature("GRAPH_EDGE_RATIO_SCOPE_TRANSFORMATION", scopecount / transformationcount),
            NumericFeature("GRAPH_EDGE_RATIO_SCOPE_AGGREGATION", scopecount / aggregationcount),
            NumericFeature("GRAPH_EDGE_RATIO_SCOPE_PARAMETER", scopecount / parametercount),
            NumericFeature("GRAPH_EDGE_RATIO_SCOPE_SUPPLIES", scopecount / suppliescount),

            NumericFeature("GRAPH_EDGE_RATIO_SUPPLIES_TRANSFORMATION", suppliescount / transformationcount),
            NumericFeature("GRAPH_EDGE_RATIO_SUPPLIES_AGGREGATION", suppliescount / aggregationcount),
            NumericFeature("GRAPH_EDGE_RATIO_SUPPLIES_PARAMETER", suppliescount / parametercount),
            NumericFeature("GRAPH_EDGE_RATIO_SUPPLIES_SCOPE", suppliescount / scopecount)
        )

        //
        //  Communities
        //

        fun Node.key(): String {
            val type = this.getAttribute<NodeType>(NodeAttributeKeys.NODETYPE)
            return when (type) {
                NodeType.ENTRYPOINT -> "E"

                NodeType.VALUE -> {
                    "V" + this.getAttribute<TypeSignature>(NodeAttributeKeys.TYPE)
                }

                NodeType.OBJECT -> {
                    val type = this.getAttribute<TypeSignature>(NodeAttributeKeys.TYPE)
                    val sig = if (type is ReferenceTypeSignature)
                        if (type.toString().contains("Ljava")) type.toString() else "_REFERENCETYPE"
                    else type.toString()

                    "O" + sig
                }

                NodeType.OPERATOR -> {
                    "Op" + this.getAttribute<Operator>(NodeAttributeKeys.OPERATOR)
                }

                NodeType.METHODCALL -> {
                    "M" + this.getAttribute<QualifiedMethodSignature>(NodeAttributeKeys.METHODSIGNATURE).methodSignature
                }
            }
        }

        fun Edge.key(): String {
            val type = this.getAttribute<EdgeType>(NodeAttributeKeys.EDGETYPE)
            return when (type) {
                EdgeType.SCOPE -> "Sc"
                EdgeType.SUPPLIES -> "Su"
                EdgeType.PARAMETER -> "P"
                EdgeType.AGGREGATION -> "A"
                EdgeType.TRANSFORMATION -> "T"
            }
        }

        val communities = mutableMapOf<String, Int>()

        fun makeCommunity(node: Node) {
            val neighbours = node.getEdgeSet<Edge>()
                .map { it to it.getOpposite<Node>(node) }
                .sortedBy { it.second.getAttribute<NodeType>(NodeAttributeKeys.NODETYPE) }

            val id = StringBuffer()

            id.append("${node.key()}_")
            for (neighbour in neighbours) {
                id.append(neighbour.first.key())
                id.append(neighbour.second.key())
            }

            val community = id.toString()
            communities[community] = communities.getOrDefault(community, 0) + 1
        }

        val ep = nodes.firstOrNull { it.isEntryPoint() }
        ep?.let { makeCommunity(it) }

        val eparams = nodes.filter { it.isEntryParameter() }
        eparams.forEach { makeCommunity(it) }
        operators.forEach { makeCommunity(it) }
        methodCalls.forEach { makeCommunity(it) }

        val communitySize = communities.map { it.value }.sum().toDouble()
        communities.forEach {
//            features.add(NumericFeature("GRAPH_COMMUNITIES_${it.key}_COUNT", it.value))
            features.add(NumericFeature("GRAPH_COMMUNITIES_${it.key}_PERC", it.value / communitySize))
        }

        //
        //  Validation - get rid of NaN or Infinity
        //

        for (feature in features)
            if (feature.value == Double.NaN || feature.value == Double.NEGATIVE_INFINITY || feature.value == Double.POSITIVE_INFINITY)
                feature.value = 0.0

        return features
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

//            reducedFeatures.add(NumericFeature("${featureName}_MIN", min))
//            reducedFeatures.add(NumericFeature("${featureName}_MAX", max))
            reducedFeatures.add(NumericFeature("${featureName}_AVG", avg))
//            reducedFeatures.add(NumericFeature("${featureName}_MEDIAN", median))
//            reducedFeatures.add(NumericFeature("${featureName}_STDDEV", stddev))
//            reducedFeatures.add(NumericFeature("${featureName}_VARIANCE", variance))
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
        arg.push("Ep${record.ref.type}")
    }

    override fun visit(record: TraceRecord.StaticLibraryMethodCall, arg: Stack<String>) {
        arg.push("Sm${record.method}")
    }

    override fun visit(record: TraceRecord.InstanceLibraryMethodCall, arg: Stack<String>) {
        arg.push("Im${record.method.methodSignature}")
    }

    override fun visit(record: TraceRecord.SynthesisedReturnValue, arg: Stack<String>) {
        arg.push("Xr${record.result.type}")
    }

    override fun visit(record: TraceRecord.StaticFieldPut, arg: Stack<String>)
    {
        val type = if (record.staticType.toString().contains("Ljava")) record.staticType.toString() else "_REFERENCETYPE"
        arg.push("Sp$type")
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
        arg.push("Ds${record.type.className}")
    }

    override fun visit(record: TraceRecord.StackTransformation, arg: Stack<String>) {
        arg.push("St${record.lhs.type}${record.rhs.type}${record.operator}${record.result.type}")
    }

    override fun visit(record: TraceRecord.NotValueTransformation, arg: Stack<String>) {
        arg.push("N")
    }

    override fun visit(record: TraceRecord.StackCast, arg: Stack<String>) {
        arg.push("Sc${record.lhs.type}${record.rhs.type}")
    }

    override fun visit(record: TraceRecord.StringConcat, arg: Stack<String>) {
        arg.push("Stc")
    }

    override fun visit(record: TraceRecord.Stringification, arg: Stack<String>) {
        arg.push("Stf${record.value.type}")
    }

    override fun visit(record: TraceRecord.Assertion, arg: Stack<String>) {
        arg.push("A")
    }

    override fun visit(record: TraceRecord.Halt, arg: Stack<String>) {
        arg.push("H")
    }

    override fun visit(record: TraceRecord.UncaughtException, arg: Stack<String>) {
        arg.push("Ue${record.type.keySig()}")
    }
}

fun ClassTypeSignature.keySig(): String {
    if (this.className.contains("Ljava")) return this.toString()
    else return "_REFERENCETYPE"
}

fun <T> MutableList<T>.addAll(vararg items: T) = this.addAll(items)
