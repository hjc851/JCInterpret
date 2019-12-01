package jcinterpret.testconsole.features.extractor

import jcinterpret.core.memory.stack.*
import jcinterpret.core.memory.heap.*
import jcinterpret.document.DocumentUtils
import jcinterpret.graph.execution.EdgeType
import jcinterpret.graph.execution.NodeAttributeKeys.NODETYPE
import jcinterpret.graph.execution.NodeAttributeKeys.EDGETYPE
import jcinterpret.graph.execution.NodeAttributeKeys.UILABEL
import jcinterpret.graph.execution.NodeAttributeKeys.UICLASS
import jcinterpret.graph.execution.NodeAttributeKeys.STRING
import jcinterpret.graph.execution.NodeAttributeKeys.VALUE
import jcinterpret.graph.execution.NodeAttributeKeys.METHODSIGNATURE
import jcinterpret.graph.execution.NodeAttributeKeys.CASTTYPE
import jcinterpret.graph.execution.NodeAttributeKeys.OPERATOR
import jcinterpret.graph.execution.NodeAttributeKeys.REPRESENTING
import jcinterpret.graph.execution.NodeAttributeKeys.TYPE
import jcinterpret.graph.execution.NodeAttributeKeys.LITERAL
import jcinterpret.graph.execution.NodeType
import jcinterpret.graph.serialization.GraphSerializationAdapter
import jcinterpret.graph.serialization.toGraph
import jcinterpret.signature.*
import jcinterpret.testconsole.features.*
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
import org.graphstream.graph.Edge
import java.nio.file.Files
import java.nio.file.Path
import java.util.HashMap
import java.util.stream.IntStream
import kotlin.streams.toList

object GraphFeatureExtractor {
    fun extract(root: Path, outRoot: Path) {
        // List the projects
        val projects = Files.list(root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }
            .sortedBy { it.fileName.toString() }

        // Iterate through the projects + process
        projects
            .forEach { project ->
                val id = project.fileName.toString()

                // Out root for this project
                val projOut = outRoot.resolve(id)
                Files.createDirectory(projOut)

                // List the entry points
                val entryPoints = Files.list(project)
                    .filter { Files.isDirectory(it) || !Files.isHidden(it) }
                    .use { it.toList() }

                // Iterate through the entry points + process
                entryPoints
                    .forEach { entryPoint ->
                    val epsig = entryPoint.fileName.toString()

                    val files = Files.list(entryPoint)
                        .filter { Files.isRegularFile(it) && !Files.isHidden(it) }
                        .use { it.toList() }

                    // Number of files - 1 (for the manifest) div by 5 (5 files per trace)
                    val traceCount = (files.count() - 1) / 5

                    IntStream.range(0, traceCount)
                        .parallel()
                        .forEach { i ->
                            // Extract the features
                            val features = mutableMapOf<FeatureType, List<Feature<*>>>()

                            val execgraph = DocumentUtils.readObject(entryPoint.resolve("$i-execgraph.ser"), GraphSerializationAdapter::class).toGraph()
                            features[FeatureType.EXECUTION_GRAPH] = extractGraphAttributes(execgraph, "EXEC")

                            val taint = DocumentUtils.readObject(entryPoint.resolve("$i-taint.ser"), GraphSerializationAdapter::class).toGraph()
                            features[FeatureType.TAINT_GRAPH] = extractGraphAttributes(taint, "TAINT")

                            val scs = DocumentUtils.readObject(entryPoint.resolve("$i-scs.ser"), GraphSerializationAdapter::class).toGraph()
                            features[FeatureType.CONCERN_GRAPH] = extractGraphAttributes(scs, "SCS")

                            // Save to disk
                            val fout = projOut.resolve("${epsig}-$i.ser")
                            DocumentUtils.writeObject(fout, HashMap(features))
                        }
                }
            }
    }

    private fun extractGraphAttributes(graph: Graph, prefix: String): List<Feature<*>> {
        val nodes = graph.getNodeSet<Node>()
        val edges = graph.getEdgeSet<Edge>()
        val elements = nodes + edges

        val byLabel = elements.mapNotNull { it.getAttribute<Any>(UILABEL) }
            .groupBy { it }
            .map { NumericFeature("GRAPH_${prefix}_LABEL_${it.key}_COUNT", it.value.count()) }

        val byClass = nodes.groupBy { it.getAttribute<String>(UICLASS) }
            .map { NumericFeature("GRAPH_${prefix}_CLASS_${it.key}_COUNT", it.value.count()) }

        val byNodeType = nodes.groupBy { it.getAttribute<NodeType>(NODETYPE) }
            .map { NumericFeature("GRAPH_${prefix}_NODETYPE_${it.key}_COUNT", it.value.count()) }

        val byTypeSignature = nodes.filter { it.hasAttribute(TYPE) }
            .groupBy { it.getAttribute<TypeSignature>(TYPE) }
            .map { NumericFeature("GRAPH_${prefix}_TYPESIGNATURE_${it.key}_COUNT", it.value.count()) }

        val byLiteral = NumericFeature("GRAPH_${prefix}_LITERAL_COUNT", nodes.filter { it.hasAttribute(LITERAL) }.count())

        val byOperator = nodes.filter { it.hasAttribute(OPERATOR) }
            .groupBy { it.getAttribute<Operator>(OPERATOR) }
            .map { NumericFeature("GRAPH_${prefix}_OPERATOR_${it.key}_COUNT", it.value.count()) }

        val byCastType = nodes.filter { it.hasAttribute(CASTTYPE) }
            .groupBy { it.getAttribute<StackType>(CASTTYPE) }
            .map { NumericFeature("GRAPH_${prefix}_CASTTYPE_${it.key}_COUNT", it.value.count()) }

        val byQualifiedMethodSignature = nodes.filter { it.hasAttribute(METHODSIGNATURE) }
            .groupBy { it.getAttribute<QualifiedMethodSignature>(METHODSIGNATURE) }
            .map { NumericFeature("GRAPH_${prefix}_QUALIFED_SIGNATURE_${it.key}_COUNT", it.value.count()) }

        val byMethodSignature = nodes.filter { it.hasAttribute(METHODSIGNATURE) }
            .groupBy { it.getAttribute<QualifiedMethodSignature>(METHODSIGNATURE).methodSignature }
            .map { NumericFeature("GRAPH_${prefix}_SIGNATURE_${it.key}_COUNT", it.value.count()) }

        val byValue = nodes.filter { it.hasAttribute(VALUE) }
            .groupBy { it.getAttribute<StackValue>(VALUE) }
            .map { NumericFeature("GRAPH_${prefix}_VALUE_${it.key}_${it.key.type}_COUNT", it.value.count()) }

        val byConcreteValue = nodes.filter { it.hasAttribute(VALUE) }
            .map { it.getAttribute<StackValue>(VALUE) }
            .filterIsInstance<ConcreteValue<*>>()
            .groupBy { it.value }
            .map { NumericFeature("GRAPH_${prefix}_CONCRETEVALUE_${it.key}_${it.key.javaClass}_COUNT", it.value.count()) }

        val byString = nodes.filter { it.hasAttribute(STRING) }
            .groupBy { it.getAttribute<StringValue>(STRING) }
            .map { NumericFeature("GRAPH_${prefix}_STRING_${it.key}_COUNT", it.value.count()) }

        val byConcreteString = nodes.filter { it.hasAttribute(STRING) }
            .map { it.getAttribute<StringValue>(STRING) }
            .filterIsInstance<ConcreteStringValue>()
            .groupBy { it.value }
            .map { NumericFeature("GRAPH_${prefix}_CONCRETESTRING_${it.key}", it.value.count()) }

        val byRepresenting = nodes.filter { it.hasAttribute(REPRESENTING) }
            .groupBy { it.getAttribute<ClassTypeSignature>(REPRESENTING) }
            .map { NumericFeature("GRAPH_${prefix}_REPRESENTINGTYPE_${it.key}", it.value.count()) }

        val byEdgeType = edges.groupBy { it.getAttribute<EdgeType>(EDGETYPE) }
            .map { NumericFeature("GRAPH_${prefix}_EDGETYPE_${it.key}_COUNT", it.value.count()) }

        val res = ArrayList<Feature<*>>()
        res.addAll(byLabel)
        res.addAll(byClass)
        res.addAll(byNodeType)
        res.addAll(byTypeSignature)
        res.add(byLiteral)
        res.addAll(byLabel)
        res.addAll(byOperator)
        res.addAll(byCastType)
        res.addAll(byQualifiedMethodSignature)
        res.addAll(byMethodSignature)
        res.addAll(byValue)
        res.addAll(byConcreteValue)
        res.addAll(byString)
        res.addAll(byConcreteString)
        res.addAll(byRepresenting)
        res.addAll(byEdgeType)
        return res
    }
}