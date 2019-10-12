package jcinterpret.core.ctx.frame

import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.memory.stack.StackNil
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.signature.QualifiedMethodSignature

abstract class ExecutionFrame {
    abstract val isFinished: Boolean

    abstract fun executeNextInstruction(ctx: ExecutionContext)

    abstract fun push(value: StackValue)
    abstract fun pop(): StackValue
    abstract fun peek(): StackValue

    fun pop(n: Int): Array<StackValue> {
        val arr = Array<StackValue>(n) { StackNil }
        for (i in 0 until n)
            arr[i] = pop()
        return arr
    }
}

abstract class MethodBoundExecutionFrame: ExecutionFrame() {
    abstract val method: QualifiedMethodSignature
}