package jcinterpret.core.ctx.frame.interpreted

import jcinterpret.core.ctx.ExecutionContext

abstract class InterpretedInstruction {
    abstract fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame)
}