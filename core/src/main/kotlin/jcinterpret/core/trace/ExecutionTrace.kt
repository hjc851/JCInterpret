package jcinterpret.core.trace

import jcinterpret.core.ctx.meta.HeapArea
import java.io.Serializable

class ExecutionTrace (
    val records: MutableList<TraceRecord>,
    val heapArea: HeapArea
): Serializable