package jcinterpret.graph.analysis.concern

import jcinterpret.graph.copyAttributesFrom
import org.graphstream.graph.Edge
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.MultiGraph

class SecondaryConcern (
    val graph: Graph
)

fun List<SecondaryConcern>.toGraph(): Graph {
    val graph = MultiGraph("")

    val nodeIds = mutableSetOf<String>()
    val edgeIds = mutableSetOf<String>()

    for (sc in this) {
        val scgraph = sc.graph
        graph.copyAttributesFrom(scgraph)

        scgraph.getNodeSet<Node>().forEach { node ->
            if (!nodeIds.contains(node.id)) {
                graph.addNode<Node>(node.id)
                    .copyAttributesFrom(node)
                nodeIds.add(node.id)
            }
        }

        scgraph.getEdgeSet<Edge>().forEach { edge ->
            if (!edgeIds.contains(edge.id)) {
                graph.addEdge<Edge>(edge.id, edge.getSourceNode<Node>().id, edge.getTargetNode<Node>().id)
                    .copyAttributesFrom(edge)
                edgeIds.add(edge.id)
            }
        }
    }

    return graph
}