package jcinterpret.graph.serialization

import java.io.Serializable

abstract class AttributedElement: Serializable {
    abstract val attributes: Array<KeyValuePair>
}

data class KeyValuePair (
    val key: String,
    val value: Serializable
): Serializable

data class GraphSerializationAdapter (
    val id: String,
    val nodes: Array<NodeSerializationAdapter>,
    val edges: Array<EdgeSerializationAdapter>,
    override val attributes: Array<KeyValuePair>
): AttributedElement() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GraphSerializationAdapter) return false

        if (id != other.id) return false
        if (!nodes.contentEquals(other.nodes)) return false
        if (!edges.contentEquals(other.edges)) return false
        if (!attributes.contentEquals(other.attributes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + nodes.contentHashCode()
        result = 31 * result + edges.contentHashCode()
        result = 31 * result + attributes.contentHashCode()
        return result
    }
}

data class NodeSerializationAdapter (
    val id: String,
    override val attributes: Array<KeyValuePair>
): AttributedElement() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NodeSerializationAdapter) return false

        if (id != other.id) return false
        if (!attributes.contentEquals(other.attributes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + attributes.contentHashCode()
        return result
    }
}

data class EdgeSerializationAdapter (
    val id: String,
    val source: String,
    val target: String,
    val isDirected: Boolean,
    override val attributes: Array<KeyValuePair>
): AttributedElement() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EdgeSerializationAdapter) return false

        if (id != other.id) return false
        if (source != other.source) return false
        if (target != other.target) return false
        if (isDirected != other.isDirected) return false
        if (!attributes.contentEquals(other.attributes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + target.hashCode()
        result = 31 * result + isDirected.hashCode()
        result = 31 * result + attributes.contentHashCode()
        return result
    }
}