package jcinterpret.testconsole.features.extractor

import jcinterpret.core.memory.stack.*
import jcinterpret.core.memory.heap.*
import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.core.trace.ExecutionTrace
import jcinterpret.core.trace.TraceRecord
import jcinterpret.document.DocumentUtils
import jcinterpret.testconsole.features.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.IntStream
import kotlin.streams.toList

object TraceFeatureExtractor {
    fun extract(root: Path, outRoot: Path) {
        // List the projects
        val projects = Files.list(root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }
            .sortedBy { it.fileName.toString() }

        // Iterate through the projects + process
        projects.parallelStream()
            .forEach { project ->
                val id = project.fileName.toString()

                // Out root for this project
                val projOut = outRoot.resolve(id)
                Files.createDirectory(projOut)

                // Trace sets for this project
                val traceSetFiles = Files.list(project)
                    .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser")}
                    .use { it.toList() }

                // Process the traces
                traceSetFiles
                    .forEach { traceSetPath ->
                        val eptraces = DocumentUtils.readObject(traceSetPath, EntryPointExecutionTraces::class)
                        val epsig = eptraces.entryPoint.toString().replace("/", ".")
                            .replace("\"", ".")

                        IntStream.range(0, eptraces.executionTraces.size)
                            .parallel()
                            .forEach { index ->
                                // Extract the features
                                val trace = eptraces.executionTraces[index]
                                val features = extractTraceFeatures(trace)

                                // Save to disk
                                val fout = projOut.resolve("${epsig}-$index.ser")
                                DocumentUtils.writeObject(fout, HashMap(features))
                            }
                    }
            }
    }

    fun extractTraceFeatures(trace: ExecutionTrace): Map<FeatureType, List<Feature<*>>> {
        val heap = trace.heapArea
        val records = trace.records

        val features = mutableMapOf<FeatureType, List<Feature<*>>>()

        // EntryPoint
        val em = records.first { it is TraceRecord.EntryMethod } as TraceRecord.EntryMethod
        val es = records.singleOrNull { it is TraceRecord.EntryScope } as TraceRecord.EntryScope?
        val est = es?.let { heap.dereference(it.ref) }
        val epars = records.filterIsInstance<TraceRecord.EntryParameter>();

        features[FeatureType.ENTRYPOINT] = listOf(
            StringFeature("ENTRYPOINT_QUALIFIED_NAME", em.sig.toString()),
            StringFeature("ENTRYPOINT_SIGNATURE_NAME", em.sig.methodSignature.toString()),

            EnumeratedFeature("ENTRYPOINT_QUALIFIED_${em.sig}", Bool.TRUE),
            EnumeratedFeature("ENTRYPOINT_SIGNATURE_${em.sig.methodSignature}", Bool.TRUE),

            EnumeratedFeature("ENTRYPOINT_SCOPE_PRESENT", if (es != null) Bool.TRUE else Bool.FALSE),
            EnumeratedFeature("ENTRYPOINT_SCOPE_TYPE${est?.type ?: "NONE"}", Bool.TRUE),

            NumericFeature("ENTRYPOINT_PARAMETER_COUNT", epars.count()),

            RelationalFeature("ENTRYPOINT_PARAMETER_STACKTYPES",
                {
                    val epargroups = epars.groupBy { it.ref.type }
                    StackType.values().map {
                        NumericFeature("ENTRYPOINT_PARAMETER_STACKTYPE_${it.name}_COUNT", epargroups[it]?.count() ?: 0)
                    }
                }().toTypedArray()
            ),

            *epars.filter { it.ref is StackReference }
                .map { heap.dereference(it.ref as StackReference) }
                .groupBy { it.type }
                .map { NumericFeature("ENTRYPOINT_HEAPTYPE_${it.key}_COUNT", it.value.size) }
                .toTypedArray()
        )

        // Heap Features
        features[FeatureType.HEAP] = listOf(
            // Total heap count
            NumericFeature("HEAP_COUNT", heap.storage.count()),

            // Count of each non-boxed type
            NumericFeature("HEAP_CONCRETE_OBJECT_COUNT", heap.storage.values.count { it is ConcreteObject }),
            NumericFeature("HEAP_SYMBOLIC_OBJECT_COUNT", heap.storage.values.count { it is SymbolicObject }),
            NumericFeature("HEAP_ARRAY_OBJECT_COUNT", heap.storage.values.count { it is SymbolicArray }),
            NumericFeature("HEAP_CLASS_LIT_OBJECT_COUNT", heap.storage.values.count { it is ClassObject }),

            // String
            NumericFeature("HEAP_STRING_COUNT", heap.storage.values.count { it is BoxedStringObject }),
            NumericFeature("HEAP_CONCRETE_STRING_COUNT", heap.storage.values.count { it is BoxedStringObject && it.value is ConcreteStringValue }),
            NumericFeature("HEAP_SYMBOLIC_STRING_COUNT", heap.storage.values.count { it is BoxedStringObject && it.value is SymbolicStringValue }),
            NumericFeature("HEAP_STRINGIFIED_STACK_VALUE_COUNT", heap.storage.values.count { it is BoxedStringObject && it.value is StackValueStringValue  }),
            NumericFeature("HEAP_COMPOSITE_STACK_VALUE_COUNT", heap.storage.values.count { it is BoxedStringObject && it.value is CompositeStringValue  }),

            // Boxed Value
            NumericFeature("HEAP_BOXED_COUNT", heap.storage.values.count { it is BoxedStackValueObject }),
            *heap.storage.values.filterIsInstance<BoxedStackValueObject>()
                .groupBy { it.value.type }
                .map { NumericFeature("HEAP_BOXED_${it.key.name}_COUNT", it.value.size) }
                .toTypedArray(),

            // Objects of each type
            *heap.storage.values.filterIsInstance<ConcreteObject>()
                .map { "HEAP_CONCRETE_OBJECT_${it.type}_COUNT" to it }
                .groupBy { it.first }
                .map { NumericFeature(it.key, it.value.count()) }
                .toTypedArray(),

            *heap.storage.values.filterIsInstance<SymbolicObject>()
                .map { "HEAP_SYMBOLIC_OBJECT_${it.type}_COUNT" to it }
                .groupBy { it.first }
                .map { NumericFeature(it.key, it.value.count()) }
                .toTypedArray(),

            *heap.storage.values.filterIsInstance<SymbolicArray>()
                .map { "HEAP_ARRAY_OBJECT_${it.type}_COUNT" to it }
                .groupBy { it.first }
                .map { NumericFeature(it.key, it.value.count()) }
                .toTypedArray()
        )

        // Execution Environment Interaction Features
        val staticCalls = records.filterIsInstance<TraceRecord.StaticLibraryMethodCall>()
        val instanceCalls = records.filterIsInstance<TraceRecord.InstanceLibraryMethodCall>()
        val syntheticValues = records.filterIsInstance<TraceRecord.SynthesisedReturnValue>()

        features[FeatureType.EXEC_ENV_INTERACTION] = listOf(
            NumericFeature("EXEC_CALL_COUNT", staticCalls.count() + instanceCalls.count()),
            NumericFeature("EXEC_STATIC_CALL_COUNT", staticCalls.count()),
            NumericFeature("EXEC_INSTANCE_CALL_COUNT", instanceCalls.count()),

            *staticCalls.groupBy { it.method.toString() }
                .map { NumericFeature("EXEC_STATIC_CALL_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *instanceCalls.groupBy { it.method.toString() }
                .map { NumericFeature("EXEC_INSTANCE_QUALIFIED_CALL_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *instanceCalls.groupBy { it.method.methodSignature.toString() }
                .map { NumericFeature("EXEC_INSTANCE_SIGNATURE_CALL_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            // Synthesised return values
            NumericFeature("EXEC_SYNTHESISED_COUNT", syntheticValues.count()),

            *syntheticValues.groupBy { it.method.toString() }
                .map { NumericFeature("EXEC_SYNTHESISED_QUALIFIED_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *syntheticValues.groupBy { it.method.methodSignature.toString() }
                .map { NumericFeature("EXEC_SYNTHESISED_SIGNATURE_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *syntheticValues.groupBy { it.result.type }
                .map { NumericFeature("EXEC_SYNTHESISED_STACKTYPE_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *syntheticValues.filter { it.result is StackReference }
                .map { heap.dereference(it.result as StackReference) to it }
                .groupBy { it.first.type.toString() }
                .map { NumericFeature("EXEC_SYNTHESISED_HEAPTYPE_${it.key}_COUNT", it.value.count()) }
                .toTypedArray()
        )

        //  Fields
        val staticFieldPut = records.filterIsInstance<TraceRecord.StaticFieldPut>()
        val defaultStaticFieldValue = records.filterIsInstance<TraceRecord.DefaultStaticFieldValue>()

        val instanceFieldPut = records.filterIsInstance<TraceRecord.ObjectFieldPut>()
        val defaultInstanceFieldValue = records.filterIsInstance<TraceRecord.DefaultInstanceFieldValue>()

        features[FeatureType.FIELDS] = listOf(
            // Static
            NumericFeature("FIELD_STATIC_ASSIGNMENT_COUNT", staticFieldPut.count()),

            *staticFieldPut.groupBy { it.staticType }
                .map { NumericFeature("FIELD_STATIC_ASSIGNMENT_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *staticFieldPut.groupBy { it.staticType to it.field }
                .map { NumericFeature("FIELD_STATIC_ASSIGNMENT_${it.key.first}:${it.key.second}_COUNT", it.value.count()) }
                .toTypedArray(),

            *staticFieldPut.groupBy { it.field }
                .map { NumericFeature("FIELD_STATIC_ASSIGNMENT_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *staticFieldPut.groupBy { it.newValue.type }
                .map { NumericFeature("FIELD_STATIC_ASSIGNMENT_STACKTYPE_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *staticFieldPut.filter { it.newValue is StackReference }
                .map { heap.dereference(it.newValue as StackReference) to it }
                .groupBy { it.first.type }
                .map { NumericFeature("FIELD_STATIC_ASSIGNMENT_HEAPTYPE_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *defaultStaticFieldValue.groupBy { it.type }
    //            .filterNot { it.key.className.startsWith("java/") || it.key.className.startsWith("javax") }
                .map { NumericFeature("FIELD_STATIC_SYNTHESISED_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            // Instance
            NumericFeature("FIELD_INSTANCE_ASSIGNMENT_COUNT", instanceFieldPut.count()),

            *instanceFieldPut.map { heap.dereference(it.ref) to it }
                .toSet()
                .groupBy { it.first.type }
                .map { NumericFeature("FIELD_INSTANCE_ASSIGNMENT_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *instanceFieldPut.map { heap.dereference(it.ref) to it }
                .groupBy { it.first.type }
                .map { NumericFeature("FIELD_INSTANCE_ASSIGNMENT_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *instanceFieldPut.groupBy { it.newValue.type }
                .map { NumericFeature("FIELD_INSTANCE_ASSIGNMENT_STACKTYPE_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *instanceFieldPut.filter { it.newValue is StackReference }
                .map { heap.dereference(it.newValue as StackReference) to it }
                .groupBy { it.first.type }
                .map { NumericFeature("FIELD_INSTANCE_ASSIGNMENT_HEAPTYPE_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *defaultInstanceFieldValue.map { heap.dereference(it.ref) to it }
                .groupBy { it.first.type }
                .map { NumericFeature("FIELD_INSTANCE_SYNTHESISED_${it.key}_COUNT", it.value.count()) }
                .toTypedArray()
        )

        //  Arrays
        val arrayPut = records.filterIsInstance<TraceRecord.ArrayMemberPut>()
        val arrayGet = records.filterIsInstance<TraceRecord.ArrayMemberGet>()

        features[FeatureType.ARRAYS] = listOf(
            NumericFeature("ARRAY_PUT_COUNT", arrayPut.count()),
            NumericFeature("ARRAY_GET_COUNT", arrayGet.count()),

            *arrayPut.map { heap.dereference(it.ref) to it }
                .groupBy { it.first.type }
                .map { NumericFeature("ARRAY_PUT_ARRTYPE_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *arrayPut.groupBy { it.newValue.type }
                .map { NumericFeature("ARRAY_PUT_STACKTYPE_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *arrayGet.map { heap.dereference(it.ref) to it }
                .groupBy { it.first.type }
                .map { NumericFeature("ARRAY_GET_ARRTYPE_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *arrayGet.groupBy { it.value.type }
                .map { NumericFeature("ARRAY_GET_ARRTYPE_${it.key}_COUNT", it.value.count()) }
                .toTypedArray()
        )

        // Stack Transformations
        val stackTransforms = records.filterIsInstance<TraceRecord.StackTransformation>()
        val notTransform = records.filterIsInstance<TraceRecord.NotValueTransformation>()
        val castTransforms = records.filterIsInstance<TraceRecord.StackCast>()

        features[FeatureType.STACK_TRANSFORMS] = listOf(
            NumericFeature("STACK_TRANSFORM_COUNT", stackTransforms.count() + notTransform.count() + castTransforms.count()),
            NumericFeature("STACK_TRANSFORM_BINARY_COUNT", stackTransforms.count()),
            NumericFeature("STACK_TRANSFORM_NOT_COUNT", notTransform.count()),
            NumericFeature("STACK_TRANSFORM_CAST_COUNT", castTransforms.count()),

            *stackTransforms.groupBy { it.operator }
                .map { NumericFeature("STACK_TRANSFORM_BINARY_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *stackTransforms.groupBy { it.result.type }
                .map { NumericFeature("STACK_TRANSFORM_BINARY_RESULT_${it.key}_COUNT", it.value.count()) }
                .toTypedArray()
        )

        // Stringification
        val stringConcat = records.filterIsInstance<TraceRecord.StringConcat>()
        val stringification = records.filterIsInstance<TraceRecord.Stringification>()

        features[FeatureType.STRING_TRANSFORM] = listOf(
            NumericFeature("STRING_TRANSFORM_CONCAT_COUNT", stringConcat.count()),
            NumericFeature("STRING_TRANSFORM_STRINGIFICATION_COUNT", stringification.count()),

            *stringConcat.groupBy { it.result.javaClass }
                .map { NumericFeature("STRING_TRANSFORM_CONCAT_RESULT_TYPE_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *stringification.groupBy { it.value.type }
                .map { NumericFeature("STRING_TRANSFORM_STRINGIFICATION_STACKTYPE_${it.key}_COUNT", it.value.count()) }
                .toTypedArray(),

            *stringification.groupBy { it.value.javaClass }
                .map { NumericFeature("STRING_TRANSFORM_STRINGIFICATION_STACKTYPE_${it.key.simpleName}_COUNT", it.value.count()) }
                .toTypedArray(),

            *stringification.map { it.value }
                .filterIsInstance<StackReference>()
                .map { heap.dereference(it) }
                .groupBy { it.type }
                .map { NumericFeature("STRING_TRANSFORM_STRINGIFICATION_HEAPTYPE_${it.key}_COUNT", it.value.count()) }
                .toTypedArray()
        )

        // Assertions
        val assertions = records.filterIsInstance<TraceRecord.Assertion>()

        val assertionsOnSymbolic = assertions.filter { it.condition is SymbolicValue }
        val assertionsOnComputed = assertions.filter { it.condition is ComputedValue }
        val assertionsOnNotValue = assertions.filter { it.condition is NotValue }
        val assertionsOnBinaryValue = assertions.filter { it.condition is BinaryOperationValue }

        features[FeatureType.ASSERTIONS] = listOf(
            NumericFeature("ASSERTIONS_COUNT", assertions.count()),
            NumericFeature("ASSERTIONS_TRUTH_TRUE_COUNT", assertions.count { it.truth }),
            NumericFeature("ASSERTIONS_TRUTH_FALSE_COUNT", assertions.count { !it.truth }),
            *assertions.groupBy { it.javaClass }
                .map { NumericFeature("ASSERTIONS_TYPE_${it.key.simpleName}_COUNT", it.value.count()) }
                .toTypedArray(),

            NumericFeature("ASSERTIONS_SYMBOLIC_COUNT", assertionsOnSymbolic.count()),
            NumericFeature("ASSERTIONS_SYMBOLIC_TRUTH_TRUE_COUNT", assertionsOnSymbolic.count { it.truth }),
            NumericFeature("ASSERTIONS_SYMBOLIC_TRUTH_FALSE_COUNT", assertionsOnSymbolic.count { !it.truth }),

            NumericFeature("ASSERTIONS_COMPUTED_COUNT", assertionsOnComputed.count()),
            NumericFeature("ASSERTIONS_COMPUTED_TRUTH_TRUE_COUNT", assertionsOnComputed.count { it.truth }),
            NumericFeature("ASSERTIONS_COMPUTED_TRUTH_FALSE_COUNT", assertionsOnComputed.count { !it.truth }),

            NumericFeature("ASSERTIONS_NOT_COUNT", assertionsOnNotValue.count()),
            NumericFeature("ASSERTIONS_NOT_TRUTH_TRUE_COUNT", assertionsOnNotValue.count { it.truth }),
            NumericFeature("ASSERTIONS_NOT_TRUTH_FALSE_COUNT", assertionsOnNotValue.count { !it.truth }),

            NumericFeature("ASSERTIONS_BINARY_COUNT", assertionsOnBinaryValue.count()),
            NumericFeature("ASSERTIONS_BINARY_TRUTH_TRUE_COUNT", assertionsOnBinaryValue.count { it.truth }),
            NumericFeature("ASSERTIONS_BINARY_TRUTH_FALSE_COUNT", assertionsOnBinaryValue.count { !it.truth }),
            *assertionsOnBinaryValue.groupBy { (it.condition as BinaryOperationValue).operator }
                .map { NumericFeature("ASSERTIONS_BINARY_OPERATOR_${it.key}_COUNT", it.value.count()) }
                .toTypedArray()
        )

        // Halts
        val halts = records.filterIsInstance<TraceRecord.Halt>()

        features[FeatureType.HALTS] = listOf(
            NumericFeature("HALT_COUNT", halts.count())
        )

        // Uncaught Exceptions
        val uncaughtExceptions = records.filterIsInstance<TraceRecord.UncaughtException>()

        features[FeatureType.UNCAUGHT_EXCEPTIONS] = listOf(
            NumericFeature("UNCAUGHT_EXCEPTIONS_COUNT", uncaughtExceptions.count()),

            *uncaughtExceptions.groupBy { it.type }
                .map { NumericFeature("UNCAUGHT_EXCEPTIONS_${it.key}_COUNT", it.value.count()) }
                .toTypedArray()
        )

        return features
    }
}