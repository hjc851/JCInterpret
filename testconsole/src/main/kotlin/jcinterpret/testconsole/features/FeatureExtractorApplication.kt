package jcinterpret.testconsole.features

import jcinterpret.testconsole.features.extractor.ConditionalFeatureExtractor
import jcinterpret.testconsole.features.extractor.GraphFeatureExtractor
import jcinterpret.testconsole.features.extractor.TraceFeatureExtractor
import jcinterpret.testconsole.features.featureset.FeatureSet
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
        Files.createDirectory(featureOut)
    }

    val ids = Files.list(traceRoot)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .map { it.fileName.toString() }
        .toList()

    val classes = ids.map { it.split("-")[0] }
        .toSet()

    FeatureSet().use { fs ->
        // Get the features
        ConditionalFeatureExtractor.extract(traceRoot, fs)
        TraceFeatureExtractor.extract(traceRoot, fs)
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
            }
        }
    }

    println("Finished")
}

//fun traceFeatures(root: Path): Pair<Map<String, Map<String, Map<FeatureType, List<Feature<*>>>>>, Map<String, Map<String, List<Map<FeatureType, List<Feature<*>>>>>>> {
//    val branchFeatures = mutableMapOf<String, MutableMap<String, Map<FeatureType, List<Feature<*>>>>>()
//    val traceFeatures = mutableMapOf<String, MutableMap<String, List<Map<FeatureType, List<Feature<*>>>>>>()
//
//    val projects = Files.list(root)
//        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
//        .use { it.toList() }
//        .sortedBy { it.fileName.toString() }
//
//    projects.forEach { project ->
//        val id = project.fileName.toString()
//        println("\tProject $id")
//        val traceSetFiles = Files.list(project)
//            .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser")}
//            .use { it.toList() }
//
//        for (traceSetFile in traceSetFiles) {
//            val eptraces = DocumentUtils.readObject(traceSetFile, EntryPointExecutionTraces::class)
//            val epsig = eptraces.entryPoint.toString().replace("/", ".")
//                .replace("\"", ".")
//
//            println("\t\tAnalysing $epsig ...")
//            if (eptraces.executionTraces.isEmpty()) continue
//
//            println("\t\t\tExtracting Branching Features ...")
//            val records = eptraces.executionTraces.map { it.records }
//            val cgraph = ConditionalGraphBuilder.build(records)
//            val cfeatures = extractConditionalGraphFeatures(cgraph)
//            branchFeatures.getOrPut(id) { mutableMapOf() }
//                .put(epsig, cfeatures)
//
//            println("\t\t\tExtracting Trace Features ...")
//            val size = eptraces.executionTraces.size
//
//            val features = IntStream.range(0, eptraces.executionTraces.size).parallel().mapToObj { index ->
//                println("\t\t\t\t${index+1} of $size")
//                val trace = eptraces.executionTraces[index]
//                return@mapToObj extractTraceFeatures(trace)
//            }.toList()
//
//            traceFeatures.getOrPut(id) { mutableMapOf() }
//                .put(epsig, features)
//        }
//    }
//
//    return branchFeatures to traceFeatures
//}
//
//fun extractConditionalGraphFeatures(root: RootBranchGraphNode): Map<FeatureType, List<Feature<*>>> {
//    val features = mutableMapOf<FeatureType, List<Feature<*>>>()
//
//    features[FeatureType.CONDITIONAL_GRAPH] = listOf(
//        NumericFeature("CONDITION_GRAPH_TREE_HEIGHT", root.height()),
//        NumericFeature("CONDITION_GRAPH_TREE_NODE_COUNT", root.size()),
//        NumericFeature("CONDITION_GRAPH_TREE_TERMINAL_COUNT", root.terminals().count()),
//        NumericFeature("CONDITION_GRAPH_TREE_INTERNAL_COUNT", root.internals().count()),
//        NumericFeature("CONDITION_GRAPH_CONDITION_COUNT", root.uniqueConditions().count())
//    )
//
//    return features
//}
//
//fun extractTraceFeatures(trace: ExecutionTrace): Map<FeatureType, List<Feature<*>>> {
//    val heap = trace.heapArea
//    val records = trace.records
//
//    val features = mutableMapOf<FeatureType, List<Feature<*>>>()
//
//    // EntryPoint
//    val em = records.first { it is TraceRecord.EntryMethod } as TraceRecord.EntryMethod
//    val es = records.singleOrNull { it is TraceRecord.EntryScope } as TraceRecord.EntryScope?
//    val est = es?.let { heap.dereference(it.ref) }
//    val epars = records.filterIsInstance<TraceRecord.EntryParameter>();
//
//    features[FeatureType.ENTRYPOINT] = listOf(
//        StringFeature("ENTRYPOINT_QUALIFIED_NAME", em.sig.toString()),
//        StringFeature("ENTRYPOINT_SIGNATURE_NAME", em.sig.methodSignature.toString()),
//
//        EnumeratedFeature("ENTRYPOINT_QUALIFIED_${em.sig}", Bool.TRUE),
//        EnumeratedFeature("ENTRYPOINT_SIGNATURE_${em.sig.methodSignature}", Bool.TRUE),
//
//        EnumeratedFeature("ENTRYPOINT_SCOPE_PRESENT", if (es != null) Bool.TRUE else Bool.FALSE),
//        EnumeratedFeature("ENTRYPOINT_SCOPE_TYPE${est?.type ?: "NONE"}", Bool.TRUE),
//
//        NumericFeature("ENTRYPOINT_PARAMETER_COUNT", epars.count()),
//
//        RelationalFeature("ENTRYPOINT_PARAMETER_STACKTYPES",
//            {
//                val epargroups = epars.groupBy { it.ref.type }
//                StackType.values().map {
//                    NumericFeature("ENTRYPOINT_PARAMETER_STACKTYPE_${it.name}_COUNT", epargroups[it]?.count() ?: 0)
//                }
//            }().toTypedArray()
//        ),
//
//        *epars.filter { it.ref is StackReference }
//            .map { heap.dereference(it.ref as StackReference) }
//            .groupBy { it.type }
//            .map { NumericFeature("ENTRYPOINT_HEAPTYPE_${it.key}_COUNT", it.value.size) }
//            .toTypedArray()
//    )
//
//    // Heap Features
//    features[FeatureType.HEAP] = listOf(
//        // Total heap count
//        NumericFeature("HEAP_COUNT", heap.storage.count()),
//
//        // Count of each non-boxed type
//        NumericFeature("HEAP_CONCRETE_OBJECT_COUNT", heap.storage.values.count { it is ConcreteObject }),
//        NumericFeature("HEAP_SYMBOLIC_OBJECT_COUNT", heap.storage.values.count { it is SymbolicObject }),
//        NumericFeature("HEAP_ARRAY_OBJECT_COUNT", heap.storage.values.count { it is SymbolicArray }),
//        NumericFeature("HEAP_CLASS_LIT_OBJECT_COUNT", heap.storage.values.count { it is ClassObject }),
//
//        // String
//        NumericFeature("HEAP_STRING_COUNT", heap.storage.values.count { it is BoxedStringObject }),
//        NumericFeature("HEAP_CONCRETE_STRING_COUNT", heap.storage.values.count { it is BoxedStringObject && it.value is ConcreteStringValue }),
//        NumericFeature("HEAP_SYMBOLIC_STRING_COUNT", heap.storage.values.count { it is BoxedStringObject && it.value is SymbolicStringValue }),
//        NumericFeature("HEAP_STRINGIFIED_STACK_VALUE_COUNT", heap.storage.values.count { it is BoxedStringObject && it.value is StackValueStringValue  }),
//        NumericFeature("HEAP_COMPOSITE_STACK_VALUE_COUNT", heap.storage.values.count { it is BoxedStringObject && it.value is CompositeStringValue  }),
//
//        // Boxed Value
//        NumericFeature("HEAP_BOXED_COUNT", heap.storage.values.count { it is BoxedStackValueObject }),
//        *heap.storage.values.filterIsInstance<BoxedStackValueObject>()
//            .groupBy { it.value.type }
//            .map { NumericFeature("HEAP_BOXED_${it.key.name}_COUNT", it.value.size) }
//            .toTypedArray(),
//
//        // Objects of each type
//        *heap.storage.values.filterIsInstance<ConcreteObject>()
//            .map { "HEAP_CONCRETE_OBJECT_${it.type}_COUNT" to it }
//            .groupBy { it.first }
//            .map { NumericFeature(it.key, it.value.count()) }
//            .toTypedArray(),
//
//        *heap.storage.values.filterIsInstance<SymbolicObject>()
//            .map { "HEAP_SYMBOLIC_OBJECT_${it.type}_COUNT" to it }
//            .groupBy { it.first }
//            .map { NumericFeature(it.key, it.value.count()) }
//            .toTypedArray(),
//
//        *heap.storage.values.filterIsInstance<SymbolicArray>()
//            .map { "HEAP_ARRAY_OBJECT_${it.type}_COUNT" to it }
//            .groupBy { it.first }
//            .map { NumericFeature(it.key, it.value.count()) }
//            .toTypedArray()
//    )
//
//    // Execution Environment Interaction Features
//    val staticCalls = records.filterIsInstance<TraceRecord.StaticLibraryMethodCall>()
//    val instanceCalls = records.filterIsInstance<TraceRecord.InstanceLibraryMethodCall>()
//    val syntheticValues = records.filterIsInstance<TraceRecord.SynthesisedReturnValue>()
//
//    features[FeatureType.EXEC_ENV_INTERACTION] = listOf(
//        NumericFeature("EXEC_CALL_COUNT", staticCalls.count() + instanceCalls.count()),
//        NumericFeature("EXEC_STATIC_CALL_COUNT", staticCalls.count()),
//        NumericFeature("EXEC_INSTANCE_CALL_COUNT", instanceCalls.count()),
//
//        *staticCalls.groupBy { it.method.toString() }
//            .map { NumericFeature("EXEC_STATIC_CALL_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *instanceCalls.groupBy { it.method.toString() }
//            .map { NumericFeature("EXEC_INSTANCE_QUALIFIED_CALL_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *instanceCalls.groupBy { it.method.methodSignature.toString() }
//            .map { NumericFeature("EXEC_INSTANCE_SIGNATURE_CALL_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        // Synthesised return values
//        NumericFeature("EXEC_SYNTHESISED_COUNT", syntheticValues.count()),
//
//        *syntheticValues.groupBy { it.method.toString() }
//            .map { NumericFeature("EXEC_SYNTHESISED_QUALIFIED_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *syntheticValues.groupBy { it.method.methodSignature.toString() }
//            .map { NumericFeature("EXEC_SYNTHESISED_SIGNATURE_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *syntheticValues.groupBy { it.result.type }
//            .map { NumericFeature("EXEC_SYNTHESISED_STACKTYPE_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *syntheticValues.filter { it.result is StackReference }
//            .map { heap.dereference(it.result as StackReference) to it }
//            .groupBy { it.first.type.toString() }
//            .map { NumericFeature("EXEC_SYNTHESISED_HEAPTYPE_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray()
//    )
//
//    //  Fields
//    val staticFieldPut = records.filterIsInstance<TraceRecord.StaticFieldPut>()
//    val defaultStaticFieldValue = records.filterIsInstance<TraceRecord.DefaultStaticFieldValue>()
//
//    val instanceFieldPut = records.filterIsInstance<TraceRecord.ObjectFieldPut>()
//    val defaultInstanceFieldValue = records.filterIsInstance<TraceRecord.DefaultInstanceFieldValue>()
//
//    features[FeatureType.FIELDS] = listOf(
//        // Static
//        NumericFeature("FIELD_STATIC_ASSIGNMENT_COUNT", staticFieldPut.count()),
//
//        *staticFieldPut.groupBy { it.staticType }
//            .map { NumericFeature("FIELD_STATIC_ASSIGNMENT_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *staticFieldPut.groupBy { it.staticType to it.field }
//            .map { NumericFeature("FIELD_STATIC_ASSIGNMENT_${it.key.first}:${it.key.second}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *staticFieldPut.groupBy { it.field }
//            .map { NumericFeature("FIELD_STATIC_ASSIGNMENT_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *staticFieldPut.groupBy { it.newValue.type }
//            .map { NumericFeature("FIELD_STATIC_ASSIGNMENT_STACKTYPE_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *staticFieldPut.filter { it.newValue is StackReference }
//            .map { heap.dereference(it.newValue as StackReference) to it }
//            .groupBy { it.first.type }
//            .map { NumericFeature("FIELD_STATIC_ASSIGNMENT_HEAPTYPE_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *defaultStaticFieldValue.groupBy { it.type }
////            .filterNot { it.key.className.startsWith("java/") || it.key.className.startsWith("javax") }
//            .map { NumericFeature("FIELD_STATIC_SYNTHESISED_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        // Instance
//        NumericFeature("FIELD_INSTANCE_ASSIGNMENT_COUNT", instanceFieldPut.count()),
//
//        *instanceFieldPut.map { heap.dereference(it.ref) to it }
//            .toSet()
//            .groupBy { it.first.type }
//            .map { NumericFeature("FIELD_INSTANCE_ASSIGNMENT_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *instanceFieldPut.map { heap.dereference(it.ref) to it }
//            .groupBy { it.first.type }
//            .map { NumericFeature("FIELD_INSTANCE_ASSIGNMENT_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *instanceFieldPut.groupBy { it.newValue.type }
//            .map { NumericFeature("FIELD_INSTANCE_ASSIGNMENT_STACKTYPE_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *instanceFieldPut.filter { it.newValue is StackReference }
//            .map { heap.dereference(it.newValue as StackReference) to it }
//            .groupBy { it.first.type }
//            .map { NumericFeature("FIELD_INSTANCE_ASSIGNMENT_HEAPTYPE_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *defaultInstanceFieldValue.map { heap.dereference(it.ref) to it }
//            .groupBy { it.first.type }
//            .map { NumericFeature("FIELD_INSTANCE_SYNTHESISED_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray()
//    )
//
//    //  Arrays
//    val arrayPut = records.filterIsInstance<TraceRecord.ArrayMemberPut>()
//    val arrayGet = records.filterIsInstance<TraceRecord.ArrayMemberGet>()
//
//    features[FeatureType.ARRAYS] = listOf(
//        NumericFeature("ARRAY_PUT_COUNT", arrayPut.count()),
//        NumericFeature("ARRAY_GET_COUNT", arrayGet.count()),
//
//        *arrayPut.map { heap.dereference(it.ref) to it }
//            .groupBy { it.first.type }
//            .map { NumericFeature("ARRAY_PUT_ARRTYPE_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *arrayPut.groupBy { it.newValue.type }
//            .map { NumericFeature("ARRAY_PUT_STACKTYPE_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *arrayGet.map { heap.dereference(it.ref) to it }
//            .groupBy { it.first.type }
//            .map { NumericFeature("ARRAY_GET_ARRTYPE_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *arrayGet.groupBy { it.value.type }
//            .map { NumericFeature("ARRAY_GET_ARRTYPE_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray()
//    )
//
//    // Stack Transformations
//    val stackTransforms = records.filterIsInstance<TraceRecord.StackTransformation>()
//    val notTransform = records.filterIsInstance<TraceRecord.NotValueTransformation>()
//    val castTransforms = records.filterIsInstance<TraceRecord.StackCast>()
//
//    features[FeatureType.STACK_TRANSFORMS] = listOf(
//        NumericFeature("STACK_TRANSFORM_COUNT", stackTransforms.count() + notTransform.count() + castTransforms.count()),
//        NumericFeature("STACK_TRANSFORM_BINARY_COUNT", stackTransforms.count()),
//        NumericFeature("STACK_TRANSFORM_NOT_COUNT", notTransform.count()),
//        NumericFeature("STACK_TRANSFORM_CAST_COUNT", castTransforms.count()),
//
//        *stackTransforms.groupBy { it.operator }
//            .map { NumericFeature("STACK_TRANSFORM_BINARY_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *stackTransforms.groupBy { it.result.type }
//            .map { NumericFeature("STACK_TRANSFORM_BINARY_RESULT_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray()
//    )
//
//    // Stringification
//    val stringConcat = records.filterIsInstance<TraceRecord.StringConcat>()
//    val stringification = records.filterIsInstance<TraceRecord.Stringification>()
//
//    features[FeatureType.STRING_TRANSFORM] = listOf(
//        NumericFeature("STRING_TRANSFORM_CONCAT_COUNT", stringConcat.count()),
//        NumericFeature("STRING_TRANSFORM_STRINGIFICATION_COUNT", stringification.count()),
//
//        *stringConcat.groupBy { it.result.javaClass }
//            .map { NumericFeature("STRING_TRANSFORM_CONCAT_RESULT_TYPE_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *stringification.groupBy { it.value.type }
//            .map { NumericFeature("STRING_TRANSFORM_STRINGIFICATION_STACKTYPE_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *stringification.groupBy { it.value.javaClass }
//            .map { NumericFeature("STRING_TRANSFORM_STRINGIFICATION_STACKTYPE_${it.key.simpleName}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        *stringification.map { it.value }
//            .filterIsInstance<StackReference>()
//            .map { heap.dereference(it) }
//            .groupBy { it.type }
//            .map { NumericFeature("STRING_TRANSFORM_STRINGIFICATION_HEAPTYPE_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray()
//    )
//
//    // Assertions
//    val assertions = records.filterIsInstance<TraceRecord.Assertion>()
//
//    val assertionsOnSymbolic = assertions.filter { it.condition is SymbolicValue }
//    val assertionsOnComputed = assertions.filter { it.condition is ComputedValue }
//    val assertionsOnNotValue = assertions.filter { it.condition is NotValue }
//    val assertionsOnBinaryValue = assertions.filter { it.condition is BinaryOperationValue }
//
//    features[FeatureType.ASSERTIONS] = listOf(
//        NumericFeature("ASSERTIONS_COUNT", assertions.count()),
//        NumericFeature("ASSERTIONS_TRUTH_TRUE_COUNT", assertions.count { it.truth }),
//        NumericFeature("ASSERTIONS_TRUTH_FALSE_COUNT", assertions.count { !it.truth }),
//        *assertions.groupBy { it.javaClass }
//            .map { NumericFeature("ASSERTIONS_TYPE_${it.key.simpleName}_COUNT", it.value.count()) }
//            .toTypedArray(),
//
//        NumericFeature("ASSERTIONS_SYMBOLIC_COUNT", assertionsOnSymbolic.count()),
//        NumericFeature("ASSERTIONS_SYMBOLIC_TRUTH_TRUE_COUNT", assertionsOnSymbolic.count { it.truth }),
//        NumericFeature("ASSERTIONS_SYMBOLIC_TRUTH_FALSE_COUNT", assertionsOnSymbolic.count { !it.truth }),
//
//        NumericFeature("ASSERTIONS_COMPUTED_COUNT", assertionsOnComputed.count()),
//        NumericFeature("ASSERTIONS_COMPUTED_TRUTH_TRUE_COUNT", assertionsOnComputed.count { it.truth }),
//        NumericFeature("ASSERTIONS_COMPUTED_TRUTH_FALSE_COUNT", assertionsOnComputed.count { !it.truth }),
//
//        NumericFeature("ASSERTIONS_NOT_COUNT", assertionsOnNotValue.count()),
//        NumericFeature("ASSERTIONS_NOT_TRUTH_TRUE_COUNT", assertionsOnNotValue.count { it.truth }),
//        NumericFeature("ASSERTIONS_NOT_TRUTH_FALSE_COUNT", assertionsOnNotValue.count { !it.truth }),
//
//        NumericFeature("ASSERTIONS_BINARY_COUNT", assertionsOnBinaryValue.count()),
//        NumericFeature("ASSERTIONS_BINARY_TRUTH_TRUE_COUNT", assertionsOnBinaryValue.count { it.truth }),
//        NumericFeature("ASSERTIONS_BINARY_TRUTH_FALSE_COUNT", assertionsOnBinaryValue.count { !it.truth }),
//        *assertionsOnBinaryValue.groupBy { (it.condition as BinaryOperationValue).operator }
//            .map { NumericFeature("ASSERTIONS_BINARY_OPERATOR_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray()
//    )
//
//    // Halts
//    val halts = records.filterIsInstance<TraceRecord.Halt>()
//
//    features[FeatureType.HALTS] = listOf(
//        NumericFeature("HALT_COUNT", halts.count())
//    )
//
//    // Uncaught Exceptions
//    val uncaughtExceptions = records.filterIsInstance<TraceRecord.UncaughtException>()
//
//    features[FeatureType.UNCAUGHT_EXCEPTIONS] = listOf(
//        NumericFeature("UNCAUGHT_EXCEPTIONS_COUNT", uncaughtExceptions.count()),
//
//        *uncaughtExceptions.groupBy { it.type }
//            .map { NumericFeature("UNCAUGHT_EXCEPTIONS_${it.key}_COUNT", it.value.count()) }
//            .toTypedArray()
//    )
//
//    return features
//}
//
//fun graphFeatures(root: Path): Map<String, Map<String, List<Map<FeatureType, List<Feature<*>>>>>> {
//    val results = mutableMapOf<String, MutableMap<String, MutableList<Map<FeatureType, List<Feature<*>>>>>>()
//
//    val projects = Files.list(root)
//        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
//        .use { it.toList() }
//
//    for (project in projects) {
//        val id = project.fileName.toString()
//        println("\t$id")
//
//        val entryPoints = Files.list(project)
//            .filter { Files.isDirectory(it) || !Files.isHidden(it) }
//            .use { it.toList() }
//
//        for (entryPoint in entryPoints) {
//            val epsig = entryPoint.fileName.toString()
//            println("\t\tAnalysing $epsig ...")
//            val files = Files.list(entryPoint)
//                .filter { Files.isRegularFile(it) && !Files.isHidden(it) }
//                .use { it.toList() }
//
//            // Number of files - 1 (for the manifest) div by 5 (5 files per trace)
//            val traceCount = (files.count() - 1) / 5
//
//            for (i in 0 until traceCount) {
//                println("\t\t\t${i+1} of $traceCount")
//                val execgraph = DocumentUtils.readObject(entryPoint.resolve("$i-execgraph.ser"), GraphSerializationAdapter::class).toGraph()
//                val taint = DocumentUtils.readObject(entryPoint.resolve("$i-taint.ser"), GraphSerializationAdapter::class).toGraph()
//                val scs = DocumentUtils.readObject(entryPoint.resolve("$i-scs.ser"), GraphSerializationAdapter::class).toGraph()
//
//                val features = mutableMapOf<FeatureType, List<Feature<*>>>()
//                features[FeatureType.EXECUTION_GRAPH] = extractGraphAttributes(execgraph, "EXEC")
//                features[FeatureType.TAINT_GRAPH] = extractGraphAttributes(taint, "TAINT")
//                features[FeatureType.CONCERN_GRAPH] = extractGraphAttributes(scs, "SCS")
//
//                results.getOrPut(id) { mutableMapOf() }
//                    .getOrPut(epsig) { mutableListOf() }
//                    .add(features)
//            }
//        }
//    }
//
//    return results
//}
//
//private fun extractGraphAttributes(graph: Graph, prefix: String): List<Feature<*>> {
//    val nodes = graph.getNodeSet<Node>()
//    val edges = graph.getEdgeSet<Edge>()
//    val elements = nodes + edges
//
//    val byLabel = elements.mapNotNull { it.getAttribute<Any>(UILABEL) }
//        .groupBy { it }
//        .map { NumericFeature("GRAPH_${prefix}_LABEL_${it.key}_COUNT", it.value.count()) }
//
//    val byClass = nodes.groupBy { it.getAttribute<String>(UICLASS) }
//        .map { NumericFeature("GRAPH_${prefix}_CLASS_${it.key}_COUNT", it.value.count()) }
//
//    val byNodeType = nodes.groupBy { it.getAttribute<NodeType>(NODETYPE) }
//        .map { NumericFeature("GRAPH_${prefix}_NODETYPE_${it.key}_COUNT", it.value.count()) }
//
//    val byTypeSignature = nodes.filter { it.hasAttribute(TYPE) }
//        .groupBy { it.getAttribute<TypeSignature>(TYPE) }
//        .map { NumericFeature("GRAPH_${prefix}_TYPESIGNATURE_${it.key}_COUNT", it.value.count()) }
//
//    val byLiteral = NumericFeature("GRAPH_${prefix}_LITERAL_COUNT", nodes.filter { it.hasAttribute(LITERAL) }.count())
//
//    val byOperator = nodes.filter { it.hasAttribute(OPERATOR) }
//        .groupBy { it.getAttribute<Operator>(OPERATOR) }
//        .map { NumericFeature("GRAPH_${prefix}_OPERATOR_${it.key}_COUNT", it.value.count()) }
//
//    val byCastType = nodes.filter { it.hasAttribute(CASTTYPE) }
//        .groupBy { it.getAttribute<StackType>(CASTTYPE) }
//        .map { NumericFeature("GRAPH_${prefix}_CASTTYPE_${it.key}_COUNT", it.value.count()) }
//
//    val byQualifiedMethodSignature = nodes.filter { it.hasAttribute(METHODSIGNATURE) }
//        .groupBy { it.getAttribute<QualifiedMethodSignature>(METHODSIGNATURE) }
//        .map { NumericFeature("GRAPH_${prefix}_QUALIFED_SIGNATURE_${it.key}_COUNT", it.value.count()) }
//
//    val byMethodSignature = nodes.filter { it.hasAttribute(METHODSIGNATURE) }
//        .groupBy { it.getAttribute<QualifiedMethodSignature>(METHODSIGNATURE).methodSignature }
//        .map { NumericFeature("GRAPH_${prefix}_SIGNATURE_${it.key}_COUNT", it.value.count()) }
//
//    val byValue = nodes.filter { it.hasAttribute(VALUE) }
//        .groupBy { it.getAttribute<StackValue>(VALUE) }
//        .map { NumericFeature("GRAPH_${prefix}_VALUE_${it.key}_${it.key.type}_COUNT", it.value.count()) }
//
//    val byConcreteValue = nodes.filter { it.hasAttribute(VALUE) }
//        .map { it.getAttribute<StackValue>(VALUE) }
//        .filterIsInstance<ConcreteValue<*>>()
//        .groupBy { it.value }
//        .map { NumericFeature("GRAPH_${prefix}_CONCRETEVALUE_${it.key}_${it.key.javaClass}_COUNT", it.value.count()) }
//
//    val byString = nodes.filter { it.hasAttribute(STRING) }
//        .groupBy { it.getAttribute<StringValue>(STRING) }
//        .map { NumericFeature("GRAPH_${prefix}_STRING_${it.key}_COUNT", it.value.count()) }
//
//    val byConcreteString = nodes.filter { it.hasAttribute(STRING) }
//        .map { it.getAttribute<StringValue>(STRING) }
//        .filterIsInstance<ConcreteStringValue>()
//        .groupBy { it.value }
//        .map { NumericFeature("GRAPH_${prefix}_CONCRETESTRING_${it.key}", it.value.count()) }
//
//    val byRepresenting = nodes.filter { it.hasAttribute(REPRESENTING) }
//        .groupBy { it.getAttribute<ClassTypeSignature>(REPRESENTING) }
//        .map { NumericFeature("GRAPH_${prefix}_REPRESENTINGTYPE_${it.key}", it.value.count()) }
//
//    val byEdgeType = edges.groupBy { it.getAttribute<EdgeType>(EDGETYPE) }
//        .map { NumericFeature("GRAPH_${prefix}_EDGETYPE_${it.key}_COUNT", it.value.count()) }
//
//    return byLabel +
//            byClass +
//            byNodeType +
//            byTypeSignature +
//            byLiteral +
//            byLabel +
//            byOperator +
//            byCastType +
//            byQualifiedMethodSignature +
//            byMethodSignature +
//            byValue +
//            byConcreteValue +
//            byString +
//            byConcreteString +
//            byRepresenting +
//            byEdgeType
//}