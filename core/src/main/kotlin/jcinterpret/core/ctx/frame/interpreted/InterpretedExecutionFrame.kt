package jcinterpret.core.ctx.frame.interpreted

import jcinterpret.core.control.ClassAreaFault
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.ctx.frame.MethodBoundExecutionFrame
import jcinterpret.core.ctx.frame.synthetic.ReturnVoid
import jcinterpret.core.descriptors.MethodDescriptor
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.QualifiedMethodSignature
import org.eclipse.jdt.core.dom.Block
import java.util.*

class InterpretedExecutionFrame (
    val instructions: Stack<InterpretedInstruction>,
    val operands: Stack<StackValue>,
    val locals: Locals,
    val exceptions: Stack<ExceptionScope>,
    val breaks: Stack<BreakScope>,
    val desc: MethodDescriptor
): MethodBoundExecutionFrame() {

    val decoder = ASTDecoder(this)

    override val method: QualifiedMethodSignature
        get() = desc.qualifiedSignature

    override val isFinished: Boolean
        get() = instructions.isEmpty()

    override fun executeNextInstruction(ctx: ExecutionContext) {
        if (isFinished)
            return_void.execute(ctx, this)

        val instruction = instructions.pop()
        println("\t$instruction")

        try {
            instruction.execute(ctx, this)
        } catch (e: ClassAreaFault) {
            instructions.push(instruction)
            throw e
        }
    }

    override fun pop(): StackValue {
        return operands.pop()
    }

    override fun peek(): StackValue {
        return operands.peek()
    }

    override fun push(value: StackValue) {
        operands.push(value)
    }

    override fun toString(): String {
        return "IEF ${System.identityHashCode(this)}"
    }
}

class ExceptionScope (
    val handles: List<ExceptionHandle>
)

class ExceptionHandle (
    val name: String,
    val type: ClassTypeSignature,
    val handle: Block
)

class BreakScope