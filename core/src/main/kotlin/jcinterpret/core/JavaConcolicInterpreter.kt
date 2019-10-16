package jcinterpret.core

import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.trace.ExecutionTrace

class JavaConcolicInterpreter internal constructor (
    private val contexts: MutableList<ExecutionContext>
) {
    internal fun addContext(ctx: ExecutionContext) = contexts.add(ctx)

    fun execute(): List<ExecutionTrace> {
        val traces = mutableListOf<ExecutionTrace>()
        while (contexts.isNotEmpty()) {
            val ctx = contexts.removeAt(0)
            println("\t\tContexts remaining: ${contexts.size}, Traces produced: ${traces.size}")
            val trace = ctx.execute()
            traces.add(trace)
        }
        return traces
    }
}