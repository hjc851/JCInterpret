package jcinterpret.core.ctx.frame.bytecode

import com.sun.tools.classfile.ClassFile
import com.sun.tools.classfile.ConstantPool
import jcinterpret.core.ExecutionConfig
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.ctx.frame.MethodBoundExecutionFrame
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.core.utils.ByteUtils
import jcinterpret.signature.QualifiedMethodSignature
import org.apache.commons.io.EndianUtils

class BytecodeExecutionFrame(
    override val method: QualifiedMethodSignature,
    val stack: Array<StackValue>,
    val locals: Array<StackValue>,
    val cp: ConstantPool,
    val code: ByteArray,
    var sc: Int = 0,
    var pc: Int = 0,
    var loopCounter: MutableMap<Int, Int> = mutableMapOf()
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

    //  //  //  //  //  //
    //  Loop Prevention //
    //  //  //  //  //  //

    fun markLoop() {
        loopCounter[this.pc-1] = loopCounter.getOrDefault(this.pc-1, 0) + 1
    }

    fun exceededLoopLimit(): Boolean {
        return loopCounter.getValue(this.pc-1) >= ExecutionConfig.maxLoopExecutions
    }

    //  //  //  //
    //  Helpers //
    //  //  //  //

    fun branch(offset: Short) {
        this.pc += offset

        if (this.pc > code.size) {
            throw IllegalStateException("pc is larger than code size")
        }
    }

    fun nextByte(): Byte {
        return this.code[this.pc++]
    }

    fun nextShort(): Short {
        val short = ByteUtils.readShort(this.code, this.pc)
        this.pc += 2
        return short
    }
}