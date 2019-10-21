package jcinterpret.graph.execution

import jcinterpret.core.ctx.meta.HeapArea
import jcinterpret.core.memory.heap.*
import jcinterpret.core.memory.stack.*
import jcinterpret.core.trace.ExecutionTrace
import jcinterpret.core.trace.TraceRecord
import jcinterpret.graph.execution.NodeAttributeKeys.CASTTYPE
import jcinterpret.graph.execution.NodeAttributeKeys.CLASS_AGGREGATION
import jcinterpret.graph.execution.NodeAttributeKeys.CLASS_ENTRYPOINT
import jcinterpret.graph.execution.NodeAttributeKeys.CLASS_METHODCALL
import jcinterpret.graph.execution.NodeAttributeKeys.CLASS_OBJECT
import jcinterpret.graph.execution.NodeAttributeKeys.CLASS_OPERATOR
import jcinterpret.graph.execution.NodeAttributeKeys.CLASS_PARAMETER
import jcinterpret.graph.execution.NodeAttributeKeys.CLASS_SCOPE
import jcinterpret.graph.execution.NodeAttributeKeys.CLASS_SUPPLIES
import jcinterpret.graph.execution.NodeAttributeKeys.CLASS_TRANSFORMATION
import jcinterpret.graph.execution.NodeAttributeKeys.CLASS_VALUE
import jcinterpret.graph.execution.NodeAttributeKeys.CONCRETE
import jcinterpret.graph.execution.NodeAttributeKeys.EDGETYPE
import jcinterpret.graph.execution.NodeAttributeKeys.ENTRYPARAMETER
import jcinterpret.graph.execution.NodeAttributeKeys.ENTRYPOINT
import jcinterpret.graph.execution.NodeAttributeKeys.LITERAL
import jcinterpret.graph.execution.NodeAttributeKeys.METHODSIGNATURE
import jcinterpret.graph.execution.NodeAttributeKeys.NODETYPE
import jcinterpret.graph.execution.NodeAttributeKeys.OPERATOR
import jcinterpret.graph.execution.NodeAttributeKeys.REPRESENTING
import jcinterpret.graph.execution.NodeAttributeKeys.STATIC
import jcinterpret.graph.execution.NodeAttributeKeys.STATICFIELD
import jcinterpret.graph.execution.NodeAttributeKeys.STRING
import jcinterpret.graph.execution.NodeAttributeKeys.SYMBOLIC
import jcinterpret.graph.execution.NodeAttributeKeys.SYNTHETIC
import jcinterpret.graph.execution.NodeAttributeKeys.TYPE
import jcinterpret.graph.execution.NodeAttributeKeys.UICLASS
import jcinterpret.graph.execution.NodeAttributeKeys.UILABEL
import jcinterpret.graph.execution.NodeAttributeKeys.VALUE
import jcinterpret.signature.QualifiedMethodSignature
import org.graphstream.graph.Edge
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.MultiGraph
import java.util.*
import kotlin.collections.HashMap

object ExecutionGraphBuilder {
    fun build(title: String, trace: ExecutionTrace): ExecutionGraph {
        val visitor = Visitor(title, trace.heapArea)
        trace.records.forEach { it.accept(visitor, Unit) }
        val ex = ExecutionGraph(visitor.graph, trace.heapArea, trace.records.filterIsInstance<TraceRecord.Assertion>())
        return ex
    }
    
    internal class Visitor(title: String, val heap: HeapArea): TraceRecord.Visitor<Unit>() {

        val graph = MultiGraph(title)

        //
        //  Construction
        //

        private var idCounter = 0
        private fun nextId(): String {
            return idCounter++.toString()
        }

        private val stackValueIdentityCache = IdentityHashMap<ConcreteValue<*>, Node>()
        private val stackValueEqualityCache = HashMap<ComputedValue, Node>()

        private fun nodeFor(value: StackValue): Node {
            when (value) {
                is ConcreteValue<*> -> {
                    var node = stackValueIdentityCache[value]
                    if (node == null) {
                        node = graph.addNode(nextId())!!
                        stackValueIdentityCache[value] = node

                        node.apply {
                            setAttribute(UICLASS, CLASS_VALUE)
                            setAttribute(UILABEL, value.label())

                            setAttribute(VALUE, value)
                            setAttribute(LITERAL, true)
                            setAttribute(CONCRETE, true)
                            setAttribute(NODETYPE, NodeType.VALUE)
                        }
                    }
                    return node
                }

                is SymbolicValue -> {
                    val id = "#${value.symbol}"
                    var node = graph.getNode<Node>(id)
                    if (node == null) {
                        node = graph.addNode<Node>(id).apply {
                            setAttribute(UICLASS, CLASS_VALUE)
                            setAttribute(UILABEL, value.label())

                            setAttribute(VALUE, value)
                            setAttribute(SYMBOLIC, true)
                            setAttribute(NODETYPE, NodeType.VALUE)
                        }
                    }
                    return node
                }

                is ReferenceValue -> return nodeFor(value)

                is ComputedValue -> {
                    var node = stackValueEqualityCache[value]
                    if (node == null) {
                        node = graph.addNode(nextId())!!
                        stackValueEqualityCache[value] = node

                        node.apply {
                            setAttribute(UICLASS, CLASS_VALUE)
                            setAttribute(UILABEL, value.label())

                            setAttribute(VALUE, value)
                            setAttribute(NODETYPE, NodeType.VALUE)
                        }
                    }
                    return node
                }

                else -> throw IllegalArgumentException("Unknown StackValue ${value.javaClass}")
            }
        }

        private val stringValueCache = HashMap<String, Node>()
        private val equalityStringValueCache = HashMap<CompositeStringValue, Node>()
        private val stackValueStringValueCache = IdentityHashMap<StackValue, Node>()

        private fun nodeFor(str: StringValue): Node {
            when (str) {
                is ConcreteStringValue -> {
                    var node = stringValueCache[str.value]
                    if (node == null) {
                        node = graph.addNode<Node>(nextId()).apply {
                            setAttribute(UICLASS, CLASS_VALUE)
                            setAttribute(UILABEL, str.label())

                            setAttribute(STRING, str)
                            setAttribute(LITERAL, true)
                            setAttribute(CONCRETE, true)
                            setAttribute(NODETYPE, NodeType.VALUE)
                        }!!
                        stringValueCache[str.value] = node
                    }
                    return node
                }

                is SymbolicStringValue -> {
                    val id = "#${str.value}"
                    var node = graph.getNode<Node>(id)
                    if (node == null) {
                        node = graph.addNode<Node>(id).apply {
                            setAttribute(UICLASS, CLASS_VALUE)
                            setAttribute(UILABEL, str.label())

                            setAttribute(STRING, str)
                            setAttribute(SYMBOLIC, true)
                            setAttribute(NODETYPE, NodeType.VALUE)
                        }
                    }
                    return node
                }

                is StackValueStringValue -> {
                    var node = stackValueStringValueCache[str.value]
                    if (node == null) {
                        node = graph.addNode<Node>(nextId()).apply {
                            setAttribute(UICLASS, CLASS_VALUE)
                            setAttribute(UILABEL, str.value)

                            setAttribute(STRING, str)
                            setAttribute(CONCRETE, true)
                            setAttribute(NODETYPE, NodeType.VALUE)
                        }!!
                        stackValueStringValueCache[str.value] = node
                    }
                    return node
                }

                is CompositeStringValue -> {
                    var node = equalityStringValueCache[str]
                    if (node == null) {
                        node = graph.addNode<Node>(nextId()).apply {
                            setAttribute(UICLASS, CLASS_VALUE)
                            setAttribute(UILABEL, str.label())

                            setAttribute(STRING, str)
                            setAttribute(CONCRETE, true)
                            setAttribute(NODETYPE, NodeType.VALUE)
                        }!!
                        equalityStringValueCache[str] = node
                    }
                    return node
                }

                else -> throw IllegalArgumentException("Unknown StringValue ${str.javaClass}")
            }
        }

        private fun nodeFor(ref: ReferenceValue): Node {
            if (ref.id == 0 || ref is StackNil || ref == StackNil) {
                var node = graph.getNode<Node>("null")
                if (node == null) {
                    node = graph.addNode<Node>("null")

                    node.apply {
                        setAttribute(UICLASS, CLASS_VALUE)
                        setAttribute(UILABEL, "null")

                        setAttribute(CONCRETE, true)
                        setAttribute(NODETYPE, NodeType.VALUE)

                    }
                }
                return node
            }

            val obj = heap.dereference(ref)

            when (obj) {
                is ConcreteObject -> {
                    val id = "${ref.id}@"
                    var node = graph.getNode<Node>(id)
                    if (node == null) {
                        node = graph.addNode<Node>(id)
                        val label = "$id${obj.type}"
                        node.apply {
                            setAttribute(UICLASS, CLASS_OBJECT)
                            setAttribute(UILABEL, label)

                            setAttribute(TYPE, obj.type)
                            setAttribute(CONCRETE, true)
                            setAttribute(NODETYPE, NodeType.OBJECT)
                        }
                    }
                    return node
                }

                is SymbolicObject -> {
                    val id = "${ref.id}@"
                    var node = graph.getNode<Node>(id)
                    if (node == null) {
                        node = graph.addNode<Node>(id)
                        val label = "$id${obj.type}"
                        node.apply {
                            setAttribute(UICLASS, CLASS_OBJECT)
                            setAttribute(UILABEL, label)

                            setAttribute(TYPE, obj.type)
                            setAttribute(SYMBOLIC, true)
                            setAttribute(NODETYPE, NodeType.OBJECT)
                        }
                    }
                    return node
                }

                is SymbolicArray -> {
                    val id = "${ref.id}@"
                    var node = graph.getNode<Node>(id)
                    if (node == null) {
                        node = graph.addNode<Node>(id)
                        val label = "$id${obj.type}"
                        node.apply {
                            setAttribute(UICLASS, CLASS_OBJECT)
                            setAttribute(UILABEL, label)

                            setAttribute(TYPE, obj.type)
                            setAttribute(SYMBOLIC, true)
                            setAttribute(NODETYPE, NodeType.OBJECT)
                        }

                        val sizeNode = nodeFor(obj.length())

                        edgeBetween(sizeNode, node) {
                            setAttribute(UICLASS, CLASS_SCOPE)
                            setAttribute(UILABEL, "length")

                            setAttribute(EDGETYPE, EdgeType.AGGREGATION)
                        }
                    }
                    return node
                }

                is BoxedStringObject -> return nodeFor(obj.value)

                is BoxedStackValueObject -> return nodeFor(obj.value)

                is ClassObject -> {
                    val id = "${ref.id}@"
                    var node = graph.getNode<Node>(id)
                    if (node == null) {
                        node = graph.addNode<Node>(id)
                        val label = "$id${obj.type}"
                        node.apply {
                            setAttribute(UICLASS, CLASS_OBJECT)
                            setAttribute(UILABEL, label)

                            setAttribute(TYPE, obj.type)
                            setAttribute(REPRESENTING, obj.value)
                            setAttribute(CONCRETE, true)
                            setAttribute(NODETYPE, NodeType.OBJECT)
                        }
                    }
                    return node
                }

                else -> throw IllegalArgumentException("Unknown HeapValue ${obj.javaClass}")
            }
        }

        private fun nodeFor(sig: QualifiedMethodSignature): Node {
            val node = graph.addNode<Node>(nextId())

            node.setAttribute(UICLASS, CLASS_METHODCALL)
            node.setAttribute(UILABEL, sig.methodSignature)
            node.setAttribute(NODETYPE, NodeType.METHODCALL)
            node.setAttribute(METHODSIGNATURE, sig)

            return node
        }

        private fun nodeFor(op: BinaryOperator): Node {
            val node = graph.addNode<Node>(nextId()).apply {
                setAttribute(UICLASS, CLASS_OPERATOR)
                setAttribute(UILABEL, op.op)

                setAttribute(NODETYPE, NodeType.OPERATOR)
                setAttribute(OPERATOR, op)
            }
            return node
        }

        private fun notNode(): Node {
            val node = graph.addNode<Node>(nextId()).apply {
                setAttribute(UICLASS, CLASS_OPERATOR)
                setAttribute(UILABEL, "!")

                setAttribute(NODETYPE, NodeType.OPERATOR)
                setAttribute(OPERATOR, NotOperator)
            }
            return node
        }

        private fun concatNode(): Node {
            val node = graph.addNode<Node>(nextId()).apply {
                setAttribute(UICLASS, CLASS_OPERATOR)
                setAttribute(UILABEL, "CONCAT")

                setAttribute(NODETYPE, NodeType.OPERATOR)
                setAttribute(OPERATOR, ConcatOperator)
            }
            return node
        }

        fun stringifyNode(): Node {
            val node = graph.addNode<Node>(nextId()).apply {
                setAttribute(UICLASS, CLASS_OPERATOR)
                setAttribute(UILABEL, "toString")

                setAttribute(NODETYPE, NodeType.OPERATOR)
                setAttribute(OPERATOR, StringifyOperator)
            }
            return node
        }

        fun castNode(type: StackType): Node {
            val node = graph.addNode<Node>(nextId()).apply {
                setAttribute(UICLASS, CLASS_OPERATOR)
                setAttribute(UILABEL, "(${type.name})")

                setAttribute(NODETYPE, NodeType.OPERATOR)
                setAttribute(OPERATOR, CastOperator)
                setAttribute(CASTTYPE, type)
            }
            return node
        }

        private fun edgeBetween(start: Node, finish: Node, handle: (Edge.() -> Unit)? = null) {
            val edge = graph.addEdge<Edge>(nextId(), start, finish, true)
            handle?.invoke(edge)
        }

        //
        //  Visitors
        //

        override fun visit(record: TraceRecord.EntryMethod, arg: Unit) {
            val node = graph.addNode<Node>(NodeType.ENTRYPOINT.name)

            node.setAttribute(ENTRYPOINT, true)
            node.setAttribute(METHODSIGNATURE, record.sig)
            node.setAttribute(NODETYPE, NodeType.ENTRYPOINT)

            node.setAttribute(UICLASS, CLASS_ENTRYPOINT)
            node.setAttribute(UILABEL, "ENTRY ${record.sig}")
        }

        override fun visit(record: TraceRecord.EntryScope, arg: Unit) {
            val node = nodeFor(record.ref)
            val entry = graph.getNode<Node>(NodeType.ENTRYPOINT.name)

            edgeBetween(node, entry) {
                setAttribute(EDGETYPE, EdgeType.SCOPE)
                setAttribute(UICLASS, CLASS_SCOPE)
            }
        }

        override fun visit(record: TraceRecord.EntryParameter, arg: Unit) {
            val node = nodeFor(record.ref)
            val entry = graph.getNode<Node>(NodeType.ENTRYPOINT.name)

            node.setAttribute(ENTRYPARAMETER, true)

            edgeBetween(node, entry) {
                setAttribute(UICLASS, CLASS_SUPPLIES)
                setAttribute(EDGETYPE, EdgeType.SUPPLIES)
            }
        }

        //  Method Calls

        override fun visit(record: TraceRecord.StaticLibraryMethodCall, arg: Unit) {
            val method = nodeFor(record.method)
            method.setAttribute(STATIC, true)

            record.params.forEachIndexed { index, param ->
                val pnode = nodeFor(param)
                edgeBetween(pnode, method) {
                    setAttribute(UICLASS, CLASS_PARAMETER)
                    setAttribute(UILABEL, "\$$index")
                    setAttribute(EDGETYPE, EdgeType.PARAMETER)
                }
            }

            if (record.result != null) {
                val result = nodeFor(record.result!!)
                edgeBetween(method, result) {
                    setAttribute(UICLASS, CLASS_SUPPLIES)
                    setAttribute(EDGETYPE, EdgeType.SUPPLIES)
                }
            }

            checkAggregation(record)
            checkTransformation(record)
        }

        override fun visit(record: TraceRecord.InstanceLibraryMethodCall, arg: Unit) {
            val method = nodeFor(record.method)

            val scope = nodeFor(record.scope)
            edgeBetween(scope, method) {
                setAttribute(UICLASS, CLASS_SCOPE)
                setAttribute(EDGETYPE, EdgeType.SCOPE)
            }

            record.params.forEachIndexed { index, param ->
                val pnode = nodeFor(param)
                edgeBetween(pnode, method) {
                    setAttribute(UICLASS, CLASS_PARAMETER)
                    setAttribute(UILABEL, "\$$index")
                    setAttribute(EDGETYPE, EdgeType.PARAMETER)
                }
            }

            if (record.result != null) {
                val result = nodeFor(record.result!!)
                edgeBetween(method, result) {
                    setAttribute(UICLASS, CLASS_SUPPLIES)
                    setAttribute(EDGETYPE, EdgeType.SUPPLIES)
                }
            }

            checkAggregation(record)
            checkTransformation(record)
        }

        override fun visit(record: TraceRecord.SynthesisedReturnValue, arg: Unit) {
            val node = nodeFor(record.result)
            node.setAttribute(SYNTHETIC, true)
        }

        //  Members

        override fun visit(record: TraceRecord.StaticFieldPut, arg: Unit) {
            val node = nodeFor(record.newValue)
            node.setAttribute(STATICFIELD, true)
        }

        override fun visit(record: TraceRecord.ObjectFieldPut, arg: Unit) {
            val self = nodeFor(record.ref)
            val value = nodeFor(record.newValue)

            edgeBetween(value, self) {
                setAttribute(UICLASS, CLASS_AGGREGATION)
                setAttribute(UILABEL, record.field)
                setAttribute(EDGETYPE, EdgeType.AGGREGATION)
            }
        }

        override fun visit(record: TraceRecord.ArrayMemberPut, arg: Unit) {
            val self = nodeFor(record.ref)
            val value = nodeFor(record.newValue)

            edgeBetween(value, self) {
                setAttribute(UICLASS, CLASS_AGGREGATION)
                setAttribute(UILABEL, "$" + record.index.label())
                setAttribute(EDGETYPE, EdgeType.AGGREGATION)
            }
        }

        override fun visit(record: TraceRecord.ArrayMemberGet, arg: Unit) {
            val idx = nodeFor(record.index)
            val value = nodeFor(record.value)

            edgeBetween(idx, value) {
                setAttribute(UICLASS, CLASS_TRANSFORMATION)
                setAttribute(EDGETYPE, EdgeType.TRANSFORMATION)
            }
        }

        override fun visit(record: TraceRecord.DefaultInstanceFieldValue, arg: Unit) {
            val self = nodeFor(record.ref)
            val value = nodeFor(record.value)

            value.setAttribute(SYNTHETIC, true)

            edgeBetween(value, self) {
                setAttribute(UICLASS, CLASS_AGGREGATION)
                setAttribute(UICLASS, record.field)
                setAttribute(EDGETYPE, EdgeType.AGGREGATION)
            }
        }

        override fun visit(record: TraceRecord.DefaultStaticFieldValue, arg: Unit) {
//            val value = nodeFor(record.value)
//
//            value.setAttribute(SYNTHETIC, true)
//            value.setAttribute(STATIC, true)
        }

        //  Transformation
        
        override fun visit(record: TraceRecord.StackTransformation, arg: Unit) {

            val operator = nodeFor(record.operator)

            val lhs = nodeFor(record.lhs)
            val rhs = nodeFor(record.rhs)
            val result = nodeFor(record.result)

            edgeBetween(lhs, operator) {
                setAttribute(UICLASS, CLASS_TRANSFORMATION)
                setAttribute(EDGETYPE, EdgeType.TRANSFORMATION)
            }

            edgeBetween(rhs, operator) {
                setAttribute(UICLASS, CLASS_TRANSFORMATION)
                setAttribute(EDGETYPE, EdgeType.TRANSFORMATION)
            }

            edgeBetween(operator, result) {
                setAttribute(UICLASS, CLASS_TRANSFORMATION)
                setAttribute(EDGETYPE, EdgeType.TRANSFORMATION)
            }
        }

        override fun visit(record: TraceRecord.NotValueTransformation, arg: Unit) {

            val op = notNode()

            val input = nodeFor(record.input)
            val output = nodeFor(record.output)

            edgeBetween(input, op) {
                setAttribute(UICLASS, CLASS_TRANSFORMATION)
                setAttribute(EDGETYPE, EdgeType.TRANSFORMATION)
            }

            edgeBetween(op, output) {
                setAttribute(UICLASS, CLASS_TRANSFORMATION)
                setAttribute(EDGETYPE, EdgeType.TRANSFORMATION)
            }
        }

        override fun visit(record: TraceRecord.StackCast, arg: Unit) {

            val lhs = nodeFor(record.lhs)
            val rhs = nodeFor(record.rhs)
            val op = castNode(record.rhs.type)

            edgeBetween(lhs, op) {
                setAttribute(UICLASS, CLASS_TRANSFORMATION)
                setAttribute(EDGETYPE, EdgeType.TRANSFORMATION)
            }

            edgeBetween(op, rhs) {
                setAttribute(UICLASS, CLASS_TRANSFORMATION)
                setAttribute(EDGETYPE, EdgeType.TRANSFORMATION)
            }
        }

        override fun visit(record: TraceRecord.StringConcat, arg: Unit) {

            val operator = concatNode()

            val lhs = nodeFor(record.lhs)
            val rhs = nodeFor(record.rhs)
            val result = nodeFor(record.result)

            edgeBetween(lhs, operator) {
                setAttribute(UICLASS, CLASS_TRANSFORMATION)
                setAttribute(EDGETYPE, EdgeType.TRANSFORMATION)
            }

            edgeBetween(rhs, operator) {
                setAttribute(UICLASS, CLASS_TRANSFORMATION)
                setAttribute(EDGETYPE, EdgeType.TRANSFORMATION)
            }

            edgeBetween(operator, result) {
                setAttribute(UICLASS, CLASS_TRANSFORMATION)
                setAttribute(EDGETYPE, EdgeType.TRANSFORMATION)
            }
        }

        override fun visit(record: TraceRecord.Stringification, arg: Unit) {

            val lhs = nodeFor(record.value)
            val rhs = nodeFor(record.result)
            val op = stringifyNode()

            edgeBetween(lhs, op) {
                setAttribute(UICLASS, CLASS_TRANSFORMATION)
                setAttribute(EDGETYPE, EdgeType.TRANSFORMATION)
            }

            edgeBetween(op, rhs) {
                setAttribute(UICLASS, CLASS_TRANSFORMATION)
                setAttribute(EDGETYPE, EdgeType.TRANSFORMATION)
            }
        }

        //  Conditions
        
        override fun visit(record: TraceRecord.Assertion, arg: Unit) { }
        
        //  Errors

        override fun visit(record: TraceRecord.Halt, arg: Unit) { }

        override fun visit(record: TraceRecord.UncaughtException, arg: Unit) { }

        //
        //  Transformation & Aggregation Handling
        //

        private fun checkTransformation(record: TraceRecord.StaticLibraryMethodCall) {

        }

        private fun checkTransformation(record: TraceRecord.InstanceLibraryMethodCall) {

        }

        private fun checkAggregation(record: TraceRecord.StaticLibraryMethodCall) {

        }

        private fun checkAggregation(record: TraceRecord.InstanceLibraryMethodCall) {

        }
    }
}