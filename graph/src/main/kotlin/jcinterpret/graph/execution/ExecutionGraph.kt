package jcinterpret.graph.execution

import jcinterpret.core.ctx.meta.HeapArea
import jcinterpret.core.trace.TraceRecord
import org.graphstream.graph.Graph

class ExecutionGraph (
    val graph: Graph,
    val heap: HeapArea,
    val assertions: List<TraceRecord.Assertion>
)