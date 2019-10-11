package jcinterpret.core

import jcinterpret.core.ctx.ExecutionContext

class JavaConcolicInterpreter internal constructor (
    private val contexts: MutableList<ExecutionContext>
) {
    internal fun addContext(ctx: ExecutionContext) = contexts.add(ctx)

    fun execute(): List<ExecutionTrace> {
        val traces = mutableListOf<ExecutionTrace>()
        while (contexts.isNotEmpty()) {
            val ctx = contexts.removeAt(0)
            val trace = ctx.execute()
            traces.add(trace)
        }
        return traces
    }
}