package jcinterpret.graph.analysis.taint

import jcinterpret.graph.*
import jcinterpret.graph.analysis.inputs.InputNodeFinder
import org.graphstream.graph.Edge
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.MultiGraph
import java.util.*

object TaintedSubgraphFinder {
    fun find(graph: Graph): TaintedSubgraph {
        val taints = mutableListOf<NodeTaint>()

        val inputs = InputNodeFinder.find(graph)
        for (input in inputs)
            taints.add(findTaint(input, graph))

        val graph = makeGraph(taints, "${graph.id} - taint")
        graph.copyAttributesFrom(graph)

        return TaintedSubgraph(inputs, taints, graph)
    }

    private fun findTaint(start: Node, graph: Graph): NodeTaint {
        val checked = mutableSetOf<Node>()
        val toCheck = Stack<Node>()
        toCheck.add(start)

        val transitions = mutableSetOf<Triple<Node, Edge, Node>>()
        while (toCheck.isNotEmpty()) {
            val node = toCheck.pop()

            if (checked.contains(node))
                continue

            when {
                node.isData() -> {
                    node.getLeavingEdgeSet<Edge>().forEach { edge ->
                        if (edge.isTransformation()) {
                            val opposite = edge.getOpposite<Node>(node)
                            transitions.add(Triple(node, edge, opposite))
                            if (!checked.contains(opposite)) {
                                toCheck.push(opposite)
                            }
                        }

                        if (edge.isAggregation()) {
                            val opposite = edge.getOpposite<Node>(node)
                            transitions.add(Triple(node, edge, opposite))
                            if (!checked.contains(opposite)) {
                                toCheck.push(opposite)
                            }
                        }

                        if (edge.isSupplies()) {
                            val opposite = edge.getOpposite<Node>(node)
                            transitions.add(Triple(node, edge, opposite))
                            if (!checked.contains(opposite)) {
                                toCheck.push(opposite)
                            }
                        }

                        if (edge.isParameter()) {
                            val opposite = edge.getOpposite<Node>(node)
                            transitions.add(Triple(node, edge, opposite))

                            if (!checked.contains(opposite)) {
                                toCheck.push(opposite)
                            }
                        }

                        if (edge.isScope()) {
                            val opposite = edge.getOpposite<Node>(node)
                            transitions.add(Triple(node, edge, opposite))

                            if (!checked.contains(opposite)) {
                                toCheck.push(opposite)
                            }
                        }
                    }

                    node.getEnteringEdgeSet<Edge>().forEach { edge ->
                        if (edge.isAggregation()) {
                            val opposite = edge.getOpposite<Node>(node)
                            transitions.add(Triple(node, edge, opposite))

                            if (!checked.contains(opposite)) {
                                toCheck.push(opposite)
                            }
                        }

                        if (edge.isScope()) {
                            val opposite = edge.getOpposite<Node>(node)
                            transitions.add(Triple(node, edge, opposite))
                            if (!checked.contains(opposite)) {
                                toCheck.push(opposite)
                            }
                        }
                    }
                }

                node.isOperator() -> {
                    node.getLeavingEdgeSet<Edge>().forEach { edge ->
                        if (edge.isTransformation()) {
                            val opposite = edge.getOpposite<Node>(node)
                            transitions.add(Triple(node, edge, opposite))
                            if (!checked.contains(opposite)) {
                                toCheck.push(opposite)
                            }
                        }
                    }
                }

                node.isMethodCall() -> {
                    node.getLeavingEdgeSet<Edge>().forEach { edge ->
                        if (edge.isSupplies()) {
                            val opposite = edge.getOpposite<Node>(node)
                            transitions.add(Triple(node, edge, opposite))
                            if (!checked.contains(opposite)) {
                                toCheck.push(opposite)
                            }
                        }

                        if (edge.isScope()) {
                            val opposite = edge.getOpposite<Node>(node)
                            transitions.add(Triple(node, edge, opposite))
                            if (!checked.contains(opposite)) {
                                toCheck.push(opposite)
                            }
                        }
                    }
                }
            }

            checked.add(node)
        }

        return NodeTaint(start, transitions.toList())
    }

    private fun makeGraph(taints: List<NodeTaint>, id: String): Graph {
        val taintGraph = MultiGraph(id)

        val nodeIds = mutableSetOf<String>()
        val edgeIds = mutableSetOf<String>()

        for (taint in taints) {
            for ((start, edge, finish) in taint.transitions) {
                if (!nodeIds.contains(start.id)) {
                    taintGraph.addNode<Node>(start.id)
                        .copyAttributesFrom(start)
                    nodeIds.add(start.id)
                }

                if (!nodeIds.contains(finish.id)) {
                    taintGraph.addNode<Node>(finish.id)
                        .copyAttributesFrom(finish)
                    nodeIds.add(finish.id)
                }

                if (!edgeIds.contains(edge.id)) {
                    taintGraph.addEdge<Edge>(edge.id, start.id, finish.id, edge.isDirected)
                        .copyAttributesFrom(edge)
                    edgeIds.add(edge.id)
                }
            }
        }

        return taintGraph
    }
}