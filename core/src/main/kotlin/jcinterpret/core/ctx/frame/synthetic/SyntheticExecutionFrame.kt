package jcinterpret.core.ctx.frame.synthetic

import jcinterpret.core.control.ClassAreaFault
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.ctx.frame.ExecutionFrame
import jcinterpret.core.memory.stack.StackValue
import java.util.*

class SyntheticExecutionFrame (
    val label: String,
    val instructions: Stack<SyntheticInstruction>,
    val operands: Stack<StackValue>
): ExecutionFrame() {
    override val isFinished: Boolean
        get() = instructions.isEmpty()

    override fun executeNextInstruction(ctx: ExecutionContext) {
        if (isFinished)
            ReturnVoid.execute(ctx, this)

        val instruction = instructions.pop()
        println("\t$instruction")

        try {
            instruction.execute(ctx, this)
        } catch (e: ClassAreaFault) {
            instructions.push(instruction)
            throw e
        }
    }

    override fun push(value: StackValue) {
        operands.push(value)
    }

    override fun peek(): StackValue = operands.peek()
    override fun pop(): StackValue = operands.pop()

    override fun toString(): String {
        return "SEF ${System.identityHashCode(this)}"
    }
}