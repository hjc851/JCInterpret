package jcinterpret.graph.condition

import jcinterpret.core.memory.stack.StackValue
import jcinterpret.core.trace.TraceRecord
import jcinterpret.signature.ClassTypeSignature

object ConditionalGraphBuilder {

    fun build(records: List<List<TraceRecord>>): RootBranchGraphNode {
        val root = RootBranchGraphNode()
        root.next = build(records, 0).apply {
            parent = root
        }
        return root
    }

    private fun build(records: List<List<TraceRecord>>, lastForkIndex: Int): BranchGraphNode {
        val first = records.firstOrNull()

        if (first == null) {
            return RegularTerminateNode(emptyList())
        }

        val nextForkIndex = nextAssertionIndex(first, lastForkIndex)
        if (nextForkIndex != null) {

            val assertion = first[nextForkIndex] as TraceRecord.Assertion
            val condition = assertion.condition

            val precedingRecords = if (lastForkIndex == 0) first.subList(0, nextForkIndex-1)
            else first.subList(lastForkIndex+1, nextForkIndex-1)

            val trueTracers = records.filter { (it[nextForkIndex] as TraceRecord.Assertion).truth }
            val falseTracers = records.filterNot { (it[nextForkIndex] as TraceRecord.Assertion).truth }

            val node = ConditionalBranchGraphNode(condition, precedingRecords)
            node.trueBranch = build(trueTracers, nextForkIndex).apply {
                parent = node
            }

            node.falseBranch = build(falseTracers, nextForkIndex).apply {
                parent = node
            }

            return node

        } else {
            val startIndex = if (lastForkIndex == 0) 0
            else if (lastForkIndex+1 > first.size-1) first.size-1
            else lastForkIndex+1
            val endIndex = first.size-1

            val remainingRecords = first.subList(startIndex, endIndex)

            val last = remainingRecords.lastOrNull()

            if (last is TraceRecord.UncaughtException)
                return UncaughtExceptionTerminalNode(last.type, remainingRecords)

            if (last is TraceRecord.Halt)
                return HaltTerminalNode(last.msg, remainingRecords)

            return RegularTerminateNode(remainingRecords)
        }
    }

    private fun nextAssertionIndex(records: List<TraceRecord>, lastForkIndex: Int): Int? {
        for (i in lastForkIndex+1 until records.size)
            if (records[i] is TraceRecord.Assertion)
                return i

        return null
    }
}

abstract class BranchGraphNode {
    abstract val children: List<BranchGraphNode>
    abstract var parent: BranchGraphNode

    fun height(): Int {
        return 1 + (children.map { it.height() }.max() ?: 0)
    }

    fun size(): Int {
        return children.map { it.size() }.sum() + 1
    }

    fun terminals(): List<TerminalBranchNode> {
        return if (this is TerminalBranchNode) listOf(this)
        else children.flatMap { it.terminals() }
    }
}

class RootBranchGraphNode: BranchGraphNode() {
    override lateinit var parent: BranchGraphNode
    var next: BranchGraphNode? = null

    override val children: List<BranchGraphNode>
        get() = if (next != null) listOf(next!!)
        else emptyList()

//    fun width(): Int {
//        if (next == null) return 0
//
//        val nchildren = next!!.children
//        if (nchildren.isEmpty()) return 1
//        if (nchildren.size == 1) return height()
//
//        val heights = nchildren.map { it.height() }
//            .sortedDescending()
//
//        TODO()
//    }

    fun collect(): List<BranchGraphNode> {
        val toCheck = mutableListOf<BranchGraphNode>()
        toCheck.add(this)

        val nodes = mutableListOf<BranchGraphNode>()

        while (toCheck.isNotEmpty()) {
            val node = toCheck.removeAt(0)

            nodes.add(node)
            toCheck.addAll(node.children)
        }

        return nodes
    }

    fun internals(): List<ConditionalBranchGraphNode> {
        return collect().filterIsInstance<ConditionalBranchGraphNode>()
    }

    fun uniqueConditions(): Set<Set<Pair<StackValue, Boolean>>> {
        val conds = ((next as? ConditionalBranchGraphNode)?.accumulateConditions() ?: emptyList())
            .map { it.toSet() }
            .toSet()
        return conds
    }
}

class ConditionalBranchGraphNode(
    val condition: StackValue,
    val precedingRecords: List<TraceRecord>
): BranchGraphNode() {
    override lateinit var parent: BranchGraphNode

    lateinit var trueBranch: BranchGraphNode
    lateinit var falseBranch: BranchGraphNode

    override val children: List<BranchGraphNode>
        get() = listOf(trueBranch, falseBranch)

    fun accumulateConditions(): List<MutableList<Pair<StackValue, Boolean>>> {
        val trueAccumulation = (trueBranch as? ConditionalBranchGraphNode)?.accumulateConditions()
            ?: listOf(mutableListOf())
        val falseAccumulation = (falseBranch as? ConditionalBranchGraphNode)?.accumulateConditions()
            ?: listOf(mutableListOf())

        trueAccumulation.forEach { it.add(0, condition to true) }
        falseAccumulation.forEach { it.add(0, condition to false) }

        return trueAccumulation + falseAccumulation
    }
}

abstract class TerminalBranchNode: BranchGraphNode()

class UncaughtExceptionTerminalNode(
    val type: ClassTypeSignature,
    val precedingRecords: List<TraceRecord>
): TerminalBranchNode() {
    override lateinit var parent: BranchGraphNode

    override val children: List<BranchGraphNode>
        get() = emptyList()
}

class HaltTerminalNode(
    val msg: String,
    val precedingRecords: List<TraceRecord>
): TerminalBranchNode() {
    override lateinit var parent: BranchGraphNode

    override val children: List<BranchGraphNode>
        get() = emptyList()
}

class RegularTerminateNode(val precedingRecords: List<TraceRecord>): TerminalBranchNode() {
    override lateinit var parent: BranchGraphNode

    override val children: List<BranchGraphNode>
        get() = emptyList()
}