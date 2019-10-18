package jcinterpret.graph

import jcinterpret.graph.execution.NodeAttributeKeys
import jcinterpret.graph.execution.NodeType
import org.graphstream.graph.Edge
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.Graphs

object LiteralChainGraphPruner {
    fun prune(graph: Graph): Graph {
        val graph = Graphs.clone(graph)

        var pruneIdCounter = 0
        fun nextId(): String {
            return "LCPRUNE${pruneIdCounter++}"
        }

        val operators = graph.getNodeSet<Node>()
            .filter { it.getAttribute<NodeType>(NodeAttributeKeys.NODETYPE) == NodeType.OPERATOR }
            .toMutableSet()

        var madeChange = true
        while (madeChange) {
            madeChange = false

            val iterator = operators.iterator()
            while (iterator.hasNext()) {
                val operator = iterator.next()

                if (operator.inDegree == 1 && operator.outDegree == 1) { // Not, stringify, cast
                    val lhsEdge = operator.getEnteringEdge<Edge>(0)
                    val lhs = lhsEdge.getOpposite<Node>(operator)

                    val resultEdge = operator.getLeavingEdge<Edge>(0)
                    val result = resultEdge.getOpposite<Node>(operator)

                    if (lhs.inDegree == 0) {
                        graph.removeEdge<Edge>(lhsEdge)
                        graph.removeNode<Node>(lhs)

                        graph.removeEdge<Edge>(resultEdge)
                        graph.removeNode<Node>(operator)

                        iterator.remove()
                        madeChange = true
                    } else if (lhs.outDegree == 1) { /* inDegree must be > 0 */
                        for (edge in lhs.getEnteringEdgeSet<Edge>()) {
                            val opposite = edge.getOpposite<Node>(lhs)

                            graph.addEdge<Edge>(nextId(), opposite, result, true)
                                .copyAttributesFrom(edge)

                            graph.removeEdge<Edge>(edge)
                        }

                        graph.removeEdge<Edge>(lhsEdge)
                        graph.removeNode<Node>(lhs)

                        graph.removeEdge<Edge>(resultEdge)
                        graph.removeNode<Node>(operator)

                        iterator.remove()
                        madeChange = true
                    }

                } else  if (operator.inDegree == 2 && operator.outDegree == 1) { // Any binary operator
                    val lhsEdge = operator.getEnteringEdge<Edge>(0)
                    val lhs = lhsEdge.getOpposite<Node>(operator)

                    val rhsEdge = operator.getEnteringEdge<Edge>(1)
                    val rhs = rhsEdge.getOpposite<Node>(operator)

                    val resultEdge = operator.getLeavingEdge<Edge>(0)
                    val result = resultEdge.getOpposite<Node>(operator)

                    /* Use this for only literals
                    lhs.getAttribute<Boolean>(LITERAL) == true &&
                        rhs.getAttribute<Boolean>(LITERAL) == true &&
                        result.getAttribute<Boolean>(LITERAL) == true

                        TODO Maybe we want to apply this for VALUE types (currently this does it for anything)
                     */

                    if (lhs.inDegree == 0 && rhs.inDegree == 0) { // Two values being combined, otherwise not used elsewhere
                        graph.removeEdge<Edge>(lhsEdge)
                        graph.removeEdge<Edge>(rhsEdge)
                        graph.removeEdge<Edge>(resultEdge)

                        if (lhs.outDegree == 0) graph.removeNode<Node>(lhs)
                        if (rhs.outDegree == 0) graph.removeNode<Node>(rhs)
                        graph.removeNode<Node>(operator)

                        iterator.remove()
                        madeChange = true
                    } else if ((lhs.inDegree >= 1 && rhs.inDegree == 0 ||
                            lhs.inDegree == 0 && rhs.inDegree >= 1)) {

                        val (floatingNode, connectedNode) = if (lhs.inDegree == 0) lhs to rhs
                        else rhs to lhs

                        val connectedIncomingEdge = connectedNode.getEnteringEdge<Edge>(0)
                        val opposite = connectedIncomingEdge.getOpposite<Node>(connectedNode)

                        graph.addEdge<Edge>(nextId(), opposite, result, true)
                            .copyAttributesFrom(connectedIncomingEdge)

                        graph.removeNode<Node>(connectedNode)
                        graph.removeNode<Node>(floatingNode)
                        graph.removeNode<Node>(operator)

                        iterator.remove()
                        madeChange = true
                    }
                }
            }
        }

        val nodeIterator = graph.getNodeIterator<Node>()
        while (nodeIterator.hasNext()) {
            val node = nodeIterator.next()
            if (node.degree == 0)
                nodeIterator.remove()
        }

        return graph
    }
}