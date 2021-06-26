package jcinterpret.core.ctx.frame.bytecode

import com.sun.tools.classfile.ConstantPool
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.ctx.frame.MethodBoundExecutionFrame
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.signature.QualifiedMethodSignature
import org.apache.commons.io.EndianUtils

class BytecodeExecutionFrame(
    override val method: QualifiedMethodSignature,
    val stack: Array<StackValue>,
    val locals: Array<StackValue>,
    val cp: ConstantPool,
    val code: ByteArray,
    var sc: Int = 0,
    var pc: Int = 0
): MethodBoundExecutionFrame() {
    override val isFinished: Boolean
        get() = pc >= code.size

    override fun executeNextInstruction(ctx: ExecutionContext) {
        BytecodeExecutor.executeNextInstruction(ctx, this)
    }

    override fun push(value: StackValue) {
        stack[sc++] = value
    }

    override fun pop(): StackValue {
        return stack[--sc]
    }

    override fun peek(): StackValue {
        return stack[sc-1]
    }

    //  //  //  //
    //  Helpers //
    //  //  //  //

    fun nextByte(): Short {
        return (code[pc++].toInt() and 0xff).toShort()
    }

    fun nextShort(): Int {
        val short = EndianUtils.readSwappedUnsignedShort(this.code, this.pc)
        this.pc += 2
        return short
    }

    fun nextUnsignedShort(): Int {
        val short = EndianUtils.readSwappedUnsignedShort(this.code, this.pc)
        this.pc += 2
        return short
    }
}