package jcinterpret.core.ctx.frame.interpreted

import jcinterpret.core.control.ReturnException
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.ctx.frame.Instruction
import org.eclipse.jdt.core.dom.Expression
import org.eclipse.jdt.core.dom.Statement

abstract class InterpretedInstruction: Instruction() {
    abstract fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame)
}

//
//  Decode
//

class decode_stmt(val stmt: Statement): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class decode_expr(val expr: Expression): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

//
//  Linkage
//

object return_void: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        throw ReturnException(null, frame.method)
    }
}

object return_value: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        throw ReturnException(frame.pop(), frame.method)
    }
}