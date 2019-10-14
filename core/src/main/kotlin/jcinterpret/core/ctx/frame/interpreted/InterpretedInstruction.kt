package jcinterpret.core.ctx.frame.interpreted

import jcinterpret.core.control.ReturnException
import jcinterpret.core.control.ThrowException
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.ctx.frame.Instruction
import jcinterpret.core.memory.heap.ConcreteObject
import jcinterpret.core.memory.heap.ObjectType
import jcinterpret.core.memory.heap.SymbolicArray
import jcinterpret.core.memory.heap.SymbolicObject
import jcinterpret.core.memory.stack.*
import jcinterpret.core.trace.TracerRecord
import jcinterpret.signature.*
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

//  Lexical

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

//  Exception

class excp_push(val handles: List<ExceptionHandle>): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        frame.exceptions.push(ExceptionScope(handles))
    }
}

object excp_pop: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        frame.exceptions.pop()
    }
}

//  Control/Breaking

//
//  Variables
//

//  Locals

class allocate(val name: String, val fieldType: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, fieldType)

        val desc = ctx.descriptorLibrary.getDescriptor(fieldType)
        frame.locals.allocate(name, desc)
    }
}

class store(val name: String, val fieldType: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, fieldType)

        val value = frame.pop()
        frame.locals.assign(name, value)
    }
}

data class load(val name: String, val fieldType: TypeSignature? = null): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        if (fieldType != null) validateSignatures(ctx, fieldType)

        val value = frame.locals.resolve(name)
        frame.push(value)
    }
}

//  Objects

class obj_allocate(val type: ClassTypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, type)

        val obj = ctx.heapArea.allocateObject(ctx, type)
        frame.push(obj.ref())
    }
}

class obj_get(val name: String, val fieldType: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, fieldType)

        val ref = frame.pop() as StackReference
        val self = ctx.heapArea.dereference(ref) as ObjectType
        val value = self.load(name, fieldType, ctx)
        frame.push(value)
    }
}

class obj_put(val name: String, val fieldType: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, fieldType)

        val ref = frame.pop() as StackReference
        val self = ctx.heapArea.dereference(ref) as ObjectType

        val oldValue = self.load(name, fieldType, ctx)
        val value = frame.pop()
        self.store(name, fieldType, value, ctx)

        ctx.records.add(TracerRecord.ObjectFieldPut(ref, name, fieldType, oldValue, value))
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

data class invoke_static(val sig: QualifiedMethodSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, sig)

        // Get parameters + self from current frame
        val paramCount = sig.methodSignature.typeSignature.argumentTypes.size
        val parameters = frame.pop(paramCount).reversedArray()

        // Get declaring class
        val deccls = ctx.classArea.getClass(sig.declaringClassSignature)

        // Get the special method
        val method = deccls.resolveStaticMethod(sig.methodSignature)
        method.invoke(ctx, null, parameters)
    }
}

data class invoke_special(val sig: QualifiedMethodSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, sig)

        // Get parameters + self from current frame
        val paramCount = sig.methodSignature.typeSignature.argumentTypes.size
        val parameters = frame.pop(paramCount).reversedArray()
        val self = frame.pop() as StackReference

        // Get declaring class
        val deccls = ctx.classArea.getClass(sig.declaringClassSignature)

        // Get the special method
        val method = deccls.resolveSpecialMethod(sig)
        method.invoke(ctx, self, parameters)
    }
}

data class invoke_virtual(val qsig: QualifiedMethodSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, qsig)

        try {
            val sig = qsig.methodSignature

            // Get parameters + self from current frame
            val paramCount = sig.typeSignature.argumentTypes.size
            val parameters = frame.pop(paramCount).reversedArray()
            val selfref = frame.pop() as StackReference

            // Get self + invoke lookup cls
            val self = ctx.heapArea.dereference(selfref) as ObjectType
            val deccls = ctx.classArea.getClass(self.lookupType)

            // Get the special method
            val method = deccls.resolveVirtualMethod(sig)
            method.invoke(ctx, selfref, parameters)
        } catch (e: Exception) {
            throw e
        }
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

class push(val value: StackValue): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        frame.push(value)
    }
}

object pop: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        frame.pop()
    }
}

object dup: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val first = frame.pop()
        frame.push(first)
        frame.push(first)
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
        val value = frame.pop()

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object or: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object and: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object xor: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object equals: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object notequals: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        if (lhs is SymbolicValue || rhs is SymbolicValue) {
            TODO()
        } else if (lhs is ReferenceValue && rhs is ReferenceValue) {
            val result = lhs.id != rhs.id
            val value = StackBoolean(result)

            frame.push(value)
            ctx.records.add(TracerRecord.StackTransformation(lhs, rhs, value, BinaryOperator.NOTEQUALS))
        } else {
            TODO()
        }
    }
}

object less: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object greater: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object lessequals: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object greaterequals: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

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
        frame.push(StackNil)
    }
}

class ldc_number(val value: String, val type: PrimitiveTypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {

        val value = when {
            type.code == 'B' -> StackByte((value.toByte()))
            type.code == 'S' -> StackShort((value.toShort()))
            type.code == 'I' -> StackInt((value.toInt()))
            type.code == 'J' -> StackLong((value.replace("L", "").toLong()))
            type.code == 'F' -> StackFloat((value.toFloat()))
            type.code == 'D' -> StackDouble((value.toDouble()))

            else -> throw IllegalArgumentException("Unknown primitive type ${type}")
        }
        
        frame.push(value)
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

//
//  Introspection
//

class cast(val type: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val value = frame.peek()

        if (type is ReferenceTypeSignature) {

            if (value is StackReference) {
                val obj = ctx.heapArea.dereference(value)

                if (!ctx.classArea.castWillSucceed(obj.type, type)) {
                    ctx.heapArea.promote(ctx, value, type)
                }

            } else {
                TODO()
            }

        } else if (type is PrimitiveTypeSignature) {

            if (value is StackReference) {
                TODO()
            } else {
                TODO()
            }

        } else {
            throw IllegalArgumentException("Unknown type signature ${type}")
        }
    }
}

class instanceof(val type: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {

        val value = frame.pop() as StackReference

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

//
//  Loops
//

class iterate(val variable: String, val type: TypeSignature, val body: Statement): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val ref = frame.pop() as StackReference
        val collection = ctx.heapArea.dereference(ref)

        if (collection is SymbolicArray) {

            if (collection.storage.isEmpty()) {
                val componentType = (collection.type as ArrayTypeSignature).componentType
                val index = ctx.heapArea.allocateSymbolic(ctx, PrimitiveTypeSignature.INT)
                val item = ctx.heapArea.allocateSymbolic(ctx, componentType)

                val oldItem = collection.get(index, componentType, ctx)
                collection.put(index, item, componentType, ctx)
                ctx.records.add(TracerRecord.ArrayMemberPut(ref, index, oldItem, item))
            }

            for (value in collection.storage.values.reversed()) {
                frame.instructions.push(block_pop)
                frame.instructions.push(decode_stmt(body))
                frame.instructions.push(store(variable, type))
                frame.instructions.push(push(value))
                frame.instructions.push(allocate(variable, type))
                frame.instructions.push(block_push)
            }

        } else if (collection is SymbolicObject) {
            TODO()
        } else if (collection is ConcreteObject) {
            TODO()
        } else {
            TODO()
        }
    }
}

//
//  Conditional
//

class conditional_if(val then: Statement, val otherwise: Statement?): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val condition = frame.pop()

        if (condition is ConcreteValue<*>) {

        } else {

        }

        TODO()
    }
}