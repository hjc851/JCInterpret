package jcinterpret.graph.analysis.taint

import org.graphstream.graph.Edge
import org.graphstream.graph.Graph
import org.graphstream.graph.Node

class TaintedSubgraph (
    val starts: Set<Node>,
    val taints: List<NodeTaint>,
    val graph: Graph
)

class NodeTaint (
    val start: Node,
    val transitions: List<Triple<Node, Edge, Node>>
)