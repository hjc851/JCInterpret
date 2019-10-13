package jcinterpret.core.ctx.frame.interpreted

import jcinterpret.core.control.ReturnException
import jcinterpret.core.control.ThrowException
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.ctx.frame.Instruction
import jcinterpret.core.memory.stack.StackReference
import jcinterpret.signature.MethodSignature
import jcinterpret.signature.QualifiedMethodSignature
import jcinterpret.signature.TypeSignature
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
        stmt.accept(frame.decoder)
    }
}

class decode_expr(val expr: Expression): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        expr.accept(frame.decoder)
    }
}

//
//  Scoping
//

object block_push: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        frame.locals.push()
    }
}

object block_pop: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        frame.locals.pop()
    }
}

//
//  Variables
//

//  Locals

class allocate(val name: String, val fieldType: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, fieldType)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class store(val name: String, val fieldType: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, fieldType)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class load(val name: String, val fieldType: TypeSignature? = null): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        if (fieldType != null) validateSignatures(ctx, fieldType)

        val value = frame.locals.resolve(name)
        frame.push(value)
    }
}

//  Objects

class obj_allocate(val type: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, type)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class obj_get(val name: String, val fieldType: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, fieldType)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class obj_put(val name: String, val fieldType: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, fieldType)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

//  Statics

class stat_get(val staticType: TypeSignature, val name: String, val fieldType: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, staticType, fieldType)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class stat_put(val staticType: TypeSignature, val name: String, val fieldType: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, staticType, fieldType)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

//  Arrays

class arr_allocate(val componentType: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, componentType)

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object arr_store: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object arr_load: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

//
//  Invoke
//

class invoke_static(val sig: QualifiedMethodSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, sig)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class invoke_virtual(val sig: MethodSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, sig)
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

object throw_exception: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val value = frame.pop() as StackReference
        throw ThrowException(value)
    }
}

//
//  Stack Management
//

object pop: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        frame.pop()
    }
}

object swap: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val first = frame.pop()
        val second = frame.pop()

        frame.push(first)
        frame.push(second)
    }
}

//
//  Primary Operators
//

//  Basic

object add: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object sub: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object mul: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object div: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object mod: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

//  Shift

object shl: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object shr: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object ushr: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

//  Logical

object not: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object or: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object and: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object xor: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object equals: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object notequals: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object less: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object greater: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object lessquals: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object greaterequals: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

//
//  Constants
//

class ldc_boolean(val value: Boolean): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class ldc_char(val value: Char): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object ldc_null: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class ldc_number(val value: String, val type: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class ldc_string(val value: String): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val string = ctx.heapArea.getOrAllocateString(value)
        frame.push(string.ref())
    }
}

class ldc_type(val value: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}