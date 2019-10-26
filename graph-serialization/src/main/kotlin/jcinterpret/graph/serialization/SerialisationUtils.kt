package jcinterpret.graph.serialization

import org.graphstream.graph.Edge
import org.graphstream.graph.Element
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.MultiGraph
import java.io.Serializable

fun Graph.toSerializable(): GraphSerializationAdapter {
    val nodes = this.getNodeSet<Node>()
        .map {
            NodeSerializationAdapter(it.id, it.serializableAttributes())
        }.toTypedArray()

    val edges = this.getEdgeSet<Edge>()
        .map {
            EdgeSerializationAdapter(it.id, it.getSourceNode<Node>().id, it.getTargetNode<Node>().id, it.isDirected, it.serializableAttributes())
        }.toTypedArray()

    val attributes = this.serializableAttributes()

    return GraphSerializationAdapter (
        this.id,
        nodes,
        edges,
        attributes
    )
}

fun GraphSerializationAdapter.toGraph(): Graph {
    val graph = MultiGraph(this.id)
    graph.addAttributes(this.attributes)

    for (node in this.nodes) {
        val gnode = graph.addNode<Node>(node.id)
        gnode.addAttributes(node.attributes)
    }

    for (edge in this.edges) {
        val gedge = graph.addEdge<Edge>(edge.id, edge.source, edge.target, edge.isDirected)
        gedge.addAttributes(edge.attributes)
    }

    return graph
}

fun Element.addAttributes(attribs: Array<KeyValuePair>) {
    for ((key, value) in attribs) {
        this.setAttribute(key, value)
    }
}

fun Element.serializableAttributes(): Array<KeyValuePair> {
    return this.attributeKeySet
        .map { KeyValuePair(it, this.getAttribute<Serializable>(it)) }
        .toTypedArray()
}