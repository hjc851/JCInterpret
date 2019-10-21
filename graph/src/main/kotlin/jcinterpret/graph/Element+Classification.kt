package jcinterpret.graph

import com.sun.org.apache.xpath.internal.operations.Bool
import jcinterpret.core.memory.heap.StringValue
import jcinterpret.graph.execution.EdgeType
import jcinterpret.graph.execution.NodeAttributeKeys.CONCRETE
import jcinterpret.graph.execution.NodeAttributeKeys.EDGETYPE
import jcinterpret.graph.execution.NodeAttributeKeys.ENTRYPARAMETER
import jcinterpret.graph.execution.NodeAttributeKeys.NODETYPE
import jcinterpret.graph.execution.NodeAttributeKeys.STRING
import jcinterpret.graph.execution.NodeAttributeKeys.SYMBOLIC
import jcinterpret.graph.execution.NodeAttributeKeys.SYNTHETIC
import jcinterpret.graph.execution.NodeType
import org.graphstream.graph.Edge
import org.graphstream.graph.Node

//
//  Nodes
//

//  Class

fun Node.isValue(): Boolean {
    return this.getAttribute<NodeType>(NODETYPE) == NodeType.VALUE
}

fun Node.isObject(): Boolean {
    return this.getAttribute<NodeType>(NODETYPE) == NodeType.OBJECT
}

fun Node.isOperator(): Boolean {
    return this.getAttribute<NodeType>(NODETYPE) == NodeType.OPERATOR
}

fun Node.isEntryPoint(): Boolean {
    return this.getAttribute<NodeType>(NODETYPE) == NodeType.ENTRYPOINT
}

fun Node.isMethodCall(): Boolean {
    return this.getAttribute<NodeType>(NODETYPE) == NodeType.METHODCALL
}

//  Traits

fun Node.isData(): Boolean {
    return this.isValue() || this.isObject()
}

fun Node.isEntryParameter(): Boolean {
    return this.getAttribute<Boolean>(ENTRYPARAMETER) == true
}

fun Node.isSynthetic(): Boolean {
    return this.getAttribute<Boolean>(SYNTHETIC) == true
}

fun Node.isConcrete(): Boolean {
    return this.getAttribute<Boolean>(CONCRETE) == true
}

fun Node.isSymbolic(): Boolean {
    return this.getAttribute<Boolean>(SYMBOLIC) == true
}

fun Node.isString(): Boolean {
    return this.getAttribute<StringValue?>(STRING) != null
}

//
//  Edges
//

fun Edge.isScope(): Boolean {
    return this.getAttribute<EdgeType>(EDGETYPE) == EdgeType.SCOPE
}

fun Edge.isSupplies(): Boolean {
    return this.getAttribute<EdgeType>(EDGETYPE) == EdgeType.SUPPLIES
}

fun Edge.isParameter(): Boolean {
    return this.getAttribute<EdgeType>(EDGETYPE) == EdgeType.PARAMETER
}

fun Edge.isAggregation(): Boolean {
    return this.getAttribute<EdgeType>(EDGETYPE) == EdgeType.AGGREGATION
}

fun Edge.isTransformation(): Boolean {
    return this.getAttribute<EdgeType>(EDGETYPE) == EdgeType.TRANSFORMATION
}


