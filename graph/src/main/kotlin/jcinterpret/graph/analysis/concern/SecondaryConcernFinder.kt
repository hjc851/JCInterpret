package jcinterpret.graph.analysis.concern

import jcinterpret.graph.analysis.taint.TaintedSubgraph
import jcinterpret.graph.copyAttributesFrom
import org.graphstream.graph.Edge
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.Graphs
import org.graphstream.graph.implementations.MultiGraph

object SecondaryConcernFinder {
    fun find(graph: Graph, taint: TaintedSubgraph): List<SecondaryConcern> {
        //  1. Extract the taints
        val withoutTaint = removeTaint(graph, taint)

        //  2. Identify the orphaned components
        val orphanedGraphs = extractOrphanedGraphs(withoutTaint)

        //  3. Restore direct neighbours
        orphanedGraphs.forEach { restoreRemovedNeighbours(it, graph) }

        return orphanedGraphs.map { SecondaryConcern(it) }
    }

    private fun restoreRemovedNeighbours(inGraph: Graph, fromGraph: Graph) {
        val insertedNodeIds = mutableSetOf<String>()
        val insertedEdgeIds = mutableSetOf<String>()

        // Get the current nodes (from current graph)
        val currentNodes = inGraph.getNodeSet<Node>()
            .map { it.id to it }.toMap()

        // Get the current nodes equivalents (from the other graph)
        val originalNodes = inGraph.getNodeSet<Node>()
            .map { it.id to fromGraph.getNode<Node>(it.id) }.toMap()

        for ((id, currentNode) in currentNodes) {
            val originalNode = originalNodes[id]!!

            // Restore Entering Edges
            val currentEnteringEdgeIds = currentNode.getEnteringEdgeSet<Edge>()
                .map { it.id }

            val missingEnteringEdges = originalNode.getEnteringEdgeSet<Edge>()
                .filter { !currentEnteringEdgeIds.contains(it.id) }

            for (missingEdge in missingEnteringEdges) {
                val opposite = missingEdge.getOpposite<Node>(originalNode)

                if (!insertedNodeIds.contains(opposite.id)) {
                    inGraph.addNode<Node>(opposite.id)
                        .copyAttributesFrom(opposite)
                    insertedNodeIds.add(opposite.id)
                }

                if (!insertedEdgeIds.contains(missingEdge.id)) {
                    inGraph.addEdge<Edge>(missingEdge.id, opposite.id, id, missingEdge.isDirected)
                        .copyAttributesFrom(missingEdge)
                    insertedEdgeIds.add(missingEdge.id)
                }
            }

            // Restore Leaving Edges
            val currentLeavingEdgeIds = currentNode.getLeavingEdgeSet<Edge>()
                .map { it.id }

            val missingLeavingEdges = originalNode.getLeavingEdgeSet<Edge>()
                .filter { !currentLeavingEdgeIds.contains(it.id) }

            for (missingEdge in missingLeavingEdges) {
                val opposite = missingEdge.getOpposite<Node>(originalNode)

                if (!insertedNodeIds.contains(opposite.id)) {
                    inGraph.addNode<Node>(opposite.id)
                        .copyAttributesFrom(opposite)
                    insertedNodeIds.add(opposite.id)
                }

                if (!insertedEdgeIds.contains(missingEdge.id)) {
                    inGraph.addEdge<Edge>(missingEdge.id, id, opposite.id, missingEdge.isDirected)
                        .copyAttributesFrom(missingEdge)
                    insertedEdgeIds.add(missingEdge.id)
                }
            }
        }
    }

    private fun extractOrphanedGraphs(graph: Graph): List<Graph> {
        val graphs = mutableListOf<Graph>()

        val unvisitedNodes = graph.getNodeSet<Node>().map { it.id }.toMutableList()
        //val visitedNodes = mutableSetOf<String>()
        val visitedEdges = mutableSetOf<String>()

        while (unvisitedNodes.isNotEmpty()) {
            val workingGraph = MultiGraph("")

            val toSearch = mutableListOf<String>()
            val nextId = unvisitedNodes[0]
            toSearch.add(nextId)

            while (toSearch.isNotEmpty()) {
                val node = graph.getNode<Node>(toSearch.removeAt(0))

                // Add the node (if it doesnt exist in the working graph)
                if (unvisitedNodes.contains(node.id)) {
                    workingGraph.addNode<Node>(node.id)
                        .copyAttributesFrom(node)
                    unvisitedNodes.remove(node.id)
                }

                // Add all of the entering edges
                for (enteringEdge in node.getEachEnteringEdge<Edge>()) {
                    val opposite = enteringEdge.getOpposite<Node>(node)
                    if (unvisitedNodes.contains(opposite.id)) {
                        workingGraph.addNode<Node>(opposite.id)
                            .copyAttributesFrom(opposite)
                        unvisitedNodes.remove(opposite.id)

                        toSearch.add(opposite.id)
                    }

                    if (!visitedEdges.contains(enteringEdge.id)) {
                        workingGraph.addEdge<Edge>(enteringEdge.id, opposite.id, node.id, enteringEdge.isDirected)
                            .copyAttributesFrom(enteringEdge)
                        visitedEdges.add(enteringEdge.id)
                    }
                }

                // Add all of the exiting edges
                for (exitingEdge in node.getEachLeavingEdge<Edge>()) {
                    val opposite = exitingEdge.getOpposite<Node>(node)
                    if (unvisitedNodes.contains(opposite.id)) {
                        workingGraph.addNode<Node>(opposite.id)
                            .copyAttributesFrom(opposite)
                        unvisitedNodes.remove(opposite.id)

                        toSearch.add(opposite.id)
                    }

                    if (!visitedEdges.contains(exitingEdge.id)) {
                        workingGraph.addEdge<Edge>(exitingEdge.id, node.id, opposite.id, exitingEdge.isDirected)
                            .copyAttributesFrom(exitingEdge)
                        visitedEdges.add(exitingEdge.id)
                    }
                }
            }

            if (workingGraph.nodeCount > 0)
                graphs.add(workingGraph)
        }

        return graphs
    }

    private fun removeCP(fromGraph: Graph, cp: Graph): Graph {
        val workingGraph = Graphs.clone(fromGraph)

        val containedNodes = fromGraph.getNodeSet<Node>().map { it.id }.toMutableSet()

        for (edge in cp.getEdgeSet<Edge>()) {
            val start = edge.getNode0<Node>()
            val finish = edge.getNode1<Node>()

            if (containedNodes.contains(start.id)) {
                workingGraph.removeNode<Node>(start.id)
                containedNodes.remove(start.id)
            }

            if (containedNodes.contains(finish.id)) {
                workingGraph.removeNode<Node>(finish.id)
                containedNodes.remove(finish.id)
            }
        }

        val containedEdges = workingGraph.getEdgeSet<Edge>().map { it.id }.toMutableSet()
        for (edge in cp.getEdgeSet<Edge>()) {
            if (containedEdges.contains(edge.id)) {
                workingGraph.removeEdge<Edge>(edge.id)
                containedEdges.remove(edge.id)
            }
        }

        workingGraph.removeNode<Node>("ENTRY")

        return workingGraph
    }

    private fun removeTaint(fromGraph: Graph, taint: TaintedSubgraph): Graph {
        val workingGraph = Graphs.clone(fromGraph)

        val containedNodes = fromGraph.getNodeSet<Node>().map { it.id }.toMutableSet()

        for (taints in taint.taints) {
            for ((start, edge, finish) in taints.transitions) {
                if (containedNodes.contains(start.id)) {
                    workingGraph.removeNode<Node>(start.id)
                    containedNodes.remove(start.id)
                }

                if (containedNodes.contains(finish.id)) {
                    workingGraph.removeNode<Node>(finish.id)
                    containedNodes.remove(finish.id)
                }
            }
        }

        val containedEdges = workingGraph.getEdgeSet<Edge>().map { it.id }.toMutableSet()
        for (taints in taint.taints) {
            for ((start, edge, finish) in taints.transitions) {
                if (containedEdges.contains(edge.id)) {
                    workingGraph.removeEdge<Edge>(edge.id)
                    containedEdges.remove(edge.id)
                }
            }
        }

        if (workingGraph.getNode<Node>("ENTRY") != null)
            workingGraph.removeNode<Node>("ENTRY")

        return workingGraph
    }
}