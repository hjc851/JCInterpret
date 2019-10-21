package jcinterpret.graph.analysis.inputs

import jcinterpret.graph.isEntryParameter
import jcinterpret.graph.isSynthetic
import org.graphstream.graph.Graph
import org.graphstream.graph.Node

object InputNodeFinder {
    fun find(graph: Graph): Set<Node> {
        val inputs = mutableSetOf<Node>()

        for (node in graph.getNodeSet<Node>()) {
            if (node.isEntryParameter() || node.isSynthetic())
                inputs.add(node)
        }

        return inputs
    }
}