package jcinterpret.core.ctx.frame.interpreted

import jcinterpret.core.control.ReturnException
import jcinterpret.core.control.ThrowException
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.ctx.frame.Instruction
import jcinterpret.core.ctx.frame.synthetic.SyntheticExecutionFrame
import jcinterpret.core.ctx.frame.synthetic.SyntheticInstruction
import jcinterpret.core.descriptors.signature
import jcinterpret.core.memory.heap.*
import jcinterpret.core.memory.stack.*
import jcinterpret.core.trace.TracerRecord
import jcinterpret.signature.*
import org.eclipse.jdt.core.dom.*
import org.eclipse.jdt.internal.compiler.ast.CharLiteral
import java.lang.reflect.Modifier
import java.util.*

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

data class decode_expr(val expr: Expression): InterpretedInstruction() {
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

//  Break & Continue

class break_push(val label: String?, val instructionSize: Int, val operandsSize: Int, val localDepth: Int, val continueInstruction: InterpretedInstruction?, val continueValue: StackValue?): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        frame.breaks.push(BreakScope(label, instructionSize, operandsSize, localDepth, continueInstruction, continueValue))
    }
}

object break_pop: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        frame.breaks.pop()
    }
}

//  Exception

class excp_push(val handles: List<ExceptionHandle>, val stackSize: Int, val localDepth: Int): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        frame.exceptions.push(ExceptionScope(handles, stackSize, localDepth))
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

        val decl = ctx.sourceLibrary.getDeclaration(type) as? TypeDeclaration
        if (decl != null) {
            tryCreateInitialiser(decl, ctx, obj)
        }
    }

    private fun tryCreateInitialiser(
        decl: TypeDeclaration,
        ctx: ExecutionContext,
        obj: ConcreteObject
    ) {
        val members = decl.bodyDeclarations()
            .filter {
                it is Initializer && !Modifier.isStatic(it.modifiers) || it is FieldDeclaration && !Modifier.isStatic(
                    it.modifiers
                )
            }

        if (members.isNotEmpty()) {
            val instructions = Stack<InterpretedInstruction>()

            for (member in members.reversed()) {
                if (member is Initializer) {
                    instructions.push(block_pop)
                    instructions.push(decode_stmt(member.body))
                    instructions.push(block_push)
                }

                if (member is FieldDeclaration) {
                    val type = member.type.resolveBinding().signature()

                    for (fragment in member.fragments()) {
                        (fragment as VariableDeclaration)

                        if (fragment.initializer != null) {
                            instructions.push(obj_put(fragment.name.identifier, type))
                            instructions.push(load("this"))
                            instructions.push(decode_expr(fragment.initializer))
                        }
                    }
                }
            }

            if (instructions.isNotEmpty()) {
                val operands = Stack<StackValue>()
                val locals = Locals()
                val exceptionScopes = Stack<ExceptionScope>()
                val breakScopes = Stack<BreakScope>()
                val desc = QualifiedMethodSignature(
                    type,
                    MethodSignature(
                        "<scinit>",
                        MethodTypeSignature(
                            emptyArray(),
                            PrimitiveTypeSignature.VOID
                        )
                    )
                )

                val tdesc = ctx.descriptorLibrary.getDescriptor(this.type)
                locals.allocate("this", tdesc)
                locals.assign("this", obj.ref())

                val frame =
                    InterpretedExecutionFrame(instructions, operands, locals, exceptionScopes, breakScopes, desc)
                ctx.frames.push(frame)
            }
        }
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

class stat_get(val staticType: ClassTypeSignature, val name: String, val fieldType: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, staticType, fieldType)

        val cls = ctx.classArea.getClass(staticType)
        val field = cls.getStaticField(name, fieldType)
        frame.push(field.value)
    }
}

class stat_put(val staticType: ClassTypeSignature, val name: String, val fieldType: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, staticType, fieldType)

        val value = frame.pop()

        val cls = ctx.classArea.getClass(staticType)
        val field = cls.getStaticField(name, fieldType)
        val oldValue = field.value
        field.value = value

        ctx.records.add(TracerRecord.StaticFieldPut(staticType, name, fieldType, oldValue, value))
    }
}

//  Arrays

class arr_allocate(val componentType: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, componentType)

        val arr = ctx.heapArea.allocateSymbolicArray(ctx, componentType.boxToArray(1))
        frame.push(arr.ref())
    }
}

object arr_store: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val value = frame.pop()
        val index = frame.pop()
        val ref = frame.pop() as StackReference

        val arr = ctx.heapArea.dereference(ref) as SymbolicArray

        val oldValue = arr.get(index, arr.componentType, ctx)
        arr.put(index, value, arr.componentType, ctx)
        ctx.records.add(TracerRecord.ArrayMemberPut(ref, index, oldValue, value))
    }
}

object arr_store_rev: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val ref = frame.pop() as StackReference
        val index = frame.pop()
        val value = frame.pop()

        try {
            val arr = ctx.heapArea.dereference(ref) as SymbolicArray

            val oldValue = arr.get(index, arr.componentType, ctx)
            arr.put(index, value, arr.componentType, ctx)
            ctx.records.add(TracerRecord.ArrayMemberPut(ref, index, oldValue, value))
        } catch (e: Exception) {
            throw e
        }
    }
}

object arr_load: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val index = frame.pop()
        val ref = frame.pop() as StackReference
        val arr = ctx.heapArea.dereference(ref) as SymbolicArray

        val value = arr.get(index, arr.componentType, ctx)
        frame.push(value)
    }
}

object arr_length: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val ref = frame.pop() as StackReference
        val arr = ctx.heapArea.dereference(ref) as SymbolicArray

        frame.push(arr.length())
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
        val self = frame.pop() as ReferenceValue

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

        val sig = qsig.methodSignature

        // Get parameters + self from current frame
        val paramCount = sig.typeSignature.argumentTypes.size
        val parameters = frame.pop(paramCount).reversedArray()
        val selfref = frame.pop() as ReferenceValue

        // Get self + invoke lookup cls
        val self = ctx.heapArea.dereference(selfref) as ObjectType
        val deccls = ctx.classArea.getClass(self.lookupType)

        // Get the special method
        val method = deccls.resolveVirtualMethod(sig)
        method.invoke(ctx, selfref, parameters)
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

class break_statement(val label: String?): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        while (frame.breaks.isNotEmpty()) {
            val scope = frame.breaks.pop()

            if (scope.label == label) {

                while (frame.instructions.size > scope.instructionsSize) frame.instructions.pop()
                while (frame.operands.size > scope.operandsSize) frame.operands.pop()
                while (frame.locals.scopes.size > scope.localDepth) frame.locals.scopes.pop()

                break
            }
        }
    }
}

class continue_statement(val label: String?): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        while (frame.breaks.isNotEmpty()) {
            val scope = frame.breaks.pop()

            if (scope.label == label) {

                while (frame.instructions.size > scope.instructionsSize) frame.instructions.pop()
                while (frame.operands.size > scope.operandsSize) frame.operands.pop()
                while (frame.locals.scopes.size > scope.localDepth) frame.locals.scopes.pop()

                val contInstruction = scope.contInstruction ?: throw IllegalStateException("Continue statement requires a continue instruction")
                frame.instructions.push(contInstruction)
                val contValue = scope.contValue
                if (contValue != null) frame.operands.push(contValue)

                break
            }
        }
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
        val rhs = frame.pop()
        val lhs = frame.pop()

        val result = if (lhs is StackReference && rhs is StackReference) ObjectOperatorUtils.add(lhs, rhs, ctx)
        else if (lhs is StackReference && rhs !is StackReference) ObjectOperatorUtils.add(lhs, rhs, ctx)
        else if (lhs !is StackReference && rhs is StackReference) ObjectOperatorUtils.add(lhs, rhs, ctx)
        else PrimaryOperationUtils.add(lhs, rhs, ctx)

        frame.push(result)
    }
}

object sub: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        val result = if (lhs is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else if (lhs is ReferenceValue && rhs !is ReferenceValue) {
            TODO()
        } else if (lhs !is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else {
            PrimaryOperationUtils.sub(lhs, rhs, ctx)
        }

        frame.push(result)
    }
}

object mul: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        val result = if (lhs is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else if (lhs is ReferenceValue && rhs !is ReferenceValue) {
            TODO()
        } else if (lhs !is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else {
            PrimaryOperationUtils.mul(lhs, rhs, ctx)
        }

        frame.push(result)
    }
}

object div: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        val result = if (lhs is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else if (lhs is ReferenceValue && rhs !is ReferenceValue) {
            TODO()
        } else if (lhs !is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else {
            PrimaryOperationUtils.div(lhs, rhs, ctx)
        }

        frame.push(result)
    }
}

object mod: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        val result = if (lhs is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else if (lhs is ReferenceValue && rhs !is ReferenceValue) {
            TODO()
        } else if (lhs !is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else {
            PrimaryOperationUtils.mod(lhs, rhs, ctx)
        }

        frame.push(result)
    }
}

//  Shift

object shl: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        if (lhs is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else if (lhs is ReferenceValue && rhs !is ReferenceValue) {
            TODO()
        } else if (lhs !is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else {
            TODO()
        }
    }
}

object shr: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        if (lhs is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else if (lhs is ReferenceValue && rhs !is ReferenceValue) {
            TODO()
        } else if (lhs !is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else {
            TODO()
        }
    }
}

object ushr: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        if (lhs is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else if (lhs is ReferenceValue && rhs !is ReferenceValue) {
            TODO()
        } else if (lhs !is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else {
            TODO()
        }
    }
}

//  Logical

object not: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val value = frame.pop()
        val result = PrimaryOperationUtils.not(value, ctx)
        frame.push(result)
    }
}

object or: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        val result = if (lhs is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else if (lhs is ReferenceValue && rhs !is ReferenceValue) {
            TODO()
        } else if (lhs !is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else {
            PrimaryOperationUtils.or(lhs, rhs, ctx)
        }

        frame.push(result)
    }
}

object and: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        val result = if (lhs is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else if (lhs is ReferenceValue && rhs !is ReferenceValue) {
            TODO()
        } else if (lhs !is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else {
            PrimaryOperationUtils.and(lhs, rhs, ctx)
        }

        frame.push(result)
    }
}

object xor: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        val result = if (lhs is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else if (lhs is ReferenceValue && rhs !is ReferenceValue) {
            TODO()
        } else if (lhs !is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else {
            PrimaryOperationUtils.xor(lhs, rhs, ctx)
        }

        frame.push(result)
    }
}

object equals: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        if (lhs is ReferenceValue && rhs is ReferenceValue) {
            val result = lhs.id != rhs.id
            val value = StackBoolean(result)

            frame.push(value)
            ctx.records.add(TracerRecord.StackTransformation(lhs, rhs, value, BinaryOperator.EQUALS))
        } else if (lhs is ReferenceValue && rhs !is ReferenceValue) {
            TODO()
        } else if (lhs !is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else {
            val value = if (lhs is ConcreteValue<*> && rhs is ConcreteValue<*>) {
                StackBoolean(lhs.number() == rhs.number())
            } else {
                BinaryOperationValue(lhs, rhs, StackType.BOOLEAN, BinaryOperator.EQUALS)
            }

            frame.push(value)
            ctx.records.add(TracerRecord.StackTransformation(lhs, rhs, value, BinaryOperator.EQUALS))
        }
    }
}

object notequals: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        frame.instructions.push(not)
        frame.instructions.push(equals)
    }
}

object less: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        val result = if (lhs is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else if (lhs is ReferenceValue && rhs !is ReferenceValue) {
            TODO()
        } else if (lhs !is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else {
            PrimaryOperationUtils.less(lhs, rhs, ctx)
        }

        frame.push(result)
    }
}

object greater: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        val result = if (lhs is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else if (lhs is ReferenceValue && rhs !is ReferenceValue) {
            TODO()
        } else if (lhs !is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else {
            PrimaryOperationUtils.greater(lhs, rhs, ctx)
        }

        frame.push(result)
    }
}

object lessequals: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        val result = if (lhs is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else if (lhs is ReferenceValue && rhs !is ReferenceValue) {
            TODO()
        } else if (lhs !is ReferenceValue && rhs is ReferenceValue) {
            TODO()
        } else {
            PrimaryOperationUtils.lessequals(lhs, rhs, ctx)
        }

        frame.push(result)
    }
}

object greaterequals: InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val rhs = frame.pop()
        val lhs = frame.pop()

        val result =  if (lhs is ReferenceValue && rhs is ReferenceValue) {
            ObjectOperatorUtils.greaterequals(lhs, rhs, ctx)
        } else if (lhs is ReferenceValue && rhs !is ReferenceValue) {
            ObjectOperatorUtils.greaterequals(lhs, rhs, ctx)
        } else if (lhs !is ReferenceValue && rhs is ReferenceValue) {
            ObjectOperatorUtils.greaterequals(lhs, rhs, ctx)
        } else {
            PrimaryOperationUtils.greaterequals(lhs, rhs, ctx)
        }

        frame.push(result)
    }
}

//
//  Constants
//

class ldc_boolean(val value: Boolean): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        frame.push(StackBoolean(value))
    }
}

class ldc_char(val value: Char): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        frame.push(StackChar(value))
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
        validateSignatures(ctx, value)

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

//
//  Introspection
//

class cast(val type: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, type)

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
            val desc = ctx.descriptorLibrary.getDescriptor(type)

            if (value is StackReference) {
                TODO()
            } else {
                val result = CastValue(value, desc.stackType)
                ctx.records.add(TracerRecord.StackCast(value, result))
            }

        } else {
            throw IllegalArgumentException("Unknown type signature ${type}")
        }
    }
}

class instanceof(val type: TypeSignature): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        validateSignatures(ctx, type)

        val value = frame.pop() as StackReference

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

//
//  Loops
//

class foreach_loop(val variable: String, val type: TypeSignature, val body: Statement): InterpretedInstruction() {

    private var initialised = false
    private val instructions = Stack<InterpretedInstruction>()
    private var popCount = 0

    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        if (!initialised) {
            setup(ctx, frame)
        }

        if (instructions.isNotEmpty()) {

            frame.instructions.push(this)

            val next = Stack<InterpretedInstruction>()
            for (i in 0 until popCount)
                next.push(instructions.pop())

            for (i in 0 until popCount)
                frame.instructions.push(next.pop())
        }
    }

    private fun setup(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        initialised = true

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
                instructions.push(block_pop)
                instructions.push(decode_stmt(body))
                instructions.push(store(variable, type))
                instructions.push(push(value))
                instructions.push(allocate(variable, type))
                instructions.push(block_push)
            }

            popCount = 6

        } else if (collection is SymbolicObject) {
            TODO()
        } else if (collection is ConcreteObject) {
            TODO()
        } else {
            TODO()
        }
    }
}

val MAX_LOOP_ITERATIONS = 10

class for_loop(val condition: Expression, val body: Statement, val updaters: MutableList<Expression>): InterpretedInstruction() {

    var counter = 0

    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val result = frame.pop()

        if (counter < MAX_LOOP_ITERATIONS) {
            if (result is StackBoolean) {
                if (result.value) {
                    frame.instructions.push(this)
                    frame.instructions.push(decode_expr(condition))
                    updaters.reversed().forEach {
                        if (it.resolveTypeBinding().name != "void") frame.instructions.push(pop)
                        frame.instructions.push(decode_expr(it))
                    }
                    frame.instructions.push(decode_stmt(body))
                }
            } else {
                frame.instructions.push(decode_stmt(body))
            }
        }

        counter++
    }
}

class while_loop(val condition: Expression, val body: Statement): InterpretedInstruction() {
    var counter = 0

    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val result = frame.pop()

        if (counter < MAX_LOOP_ITERATIONS) {
            if (result is StackBoolean) {
                if (result.value) {
                    frame.instructions.push(this)
                    frame.instructions.push(decode_expr(condition))
                    frame.instructions.push(decode_stmt(body))
                }
            } else {
                frame.instructions.push(decode_stmt(body))
            }
        }

        counter++
    }
}

//
//  Conditional
//

class conditional_if(val then: Statement, val otherwise: Statement?): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val condition = frame.pop()

        if (condition is StackBoolean) {
            if (condition.value) {
                frame.instructions.push(decode_stmt(then))
            } else if (otherwise != null) {
                frame.instructions.push(decode_stmt(otherwise))
            }
        } else {

            if (otherwise != null) {
                ctx.fork {
                    it.records.add(TracerRecord.Assertion(condition, false))
                    (it.currentFrame as InterpretedExecutionFrame).instructions.push(decode_stmt(otherwise))
                }
            }

            ctx.records.add(TracerRecord.Assertion(condition, true))
            frame.instructions.push(decode_stmt(then))
        }
    }
}

class conditional_ternary(val then: Expression, val otherwise: Expression): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val condition = frame.pop()

        if (condition is StackBoolean) {
            if (condition.value) {
                frame.instructions.push(decode_expr(then))
            } else {
                frame.instructions.push(decode_expr(otherwise))
            }
        } else {
            ctx.fork {
                it.records.add(TracerRecord.Assertion(condition, false))
                (it.currentFrame as InterpretedExecutionFrame).instructions.push(decode_expr(otherwise))
            }

            ctx.records.add(TracerRecord.Assertion(condition, true))
            frame.instructions.push(decode_expr(then))
        }
    }
}

class conditional_switch(val statements: List<Statement>): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val value = frame.pop()

        if (value is ConcreteValue<*>) {

            for (i in 0 until statements.size) {
                val statement = statements[i]

                if (statement is SwitchCase) {
                    if (statement.isDefault) {
                        statements.subList(i+1, statements.size-1)
                            .reversed()
                            .forEach { frame.instructions.push(decode_stmt(it)) }

                    } else {
                        val expr = statement.expression

                        val match = when (expr) {
                            is NumberLiteral -> expr.token == value.value.toString()
                            is CharacterLiteral -> expr.charValue() == value.value

                            else -> throw IllegalArgumentException("Unknown literal in switch statement $expr")
                        }

                        if (match) {
                            statements.subList(i+1, statements.size-1)
                                .reversed()
                                .forEach { frame.instructions.push(decode_stmt(it)) }
                        }
                    }
                }
            }

        } else if (value is ReferenceValue) {

            val obj = ctx.heapArea.dereference(value)

            if (obj is BoxedStackValueObject) {
                TODO()

            } else if (obj is BoxedStringObject) {
                TODO()

            } else {
                TODO()
            }

        } else /* symbolic or computed */ {

            // This thread will execute the default, or skip if none
            for (i in 0 until statements.size) {
                val statement = statements[i]

                if (statement is SwitchCase) {
                    if (statement.isDefault) {
                        statements.subList(i+1, statements.size-1)
                            .reversed()
                            .forEach { frame.instructions.push(decode_stmt(it)) }

                        for (j in i downTo 0) {
                            val reverseStatement = statements[j]
                            if (reverseStatement is SwitchCase && !reverseStatement.isDefault) {
                                frame.instructions.push(tracehook { TracerRecord.Assertion(it.currentFrame.pop(), false) })
                                frame.instructions.push(equals)
                                frame.instructions.push(decode_expr(statement.expression))
                                frame.instructions.push(push(value))
                            }
                        }

                    } else {
                        ctx.fork { ctx ->
                            val forkFrame = (ctx.currentFrame as InterpretedExecutionFrame)
                            statements.subList(i+1, statements.size-1)
                                .reversed()
                                .forEach { forkFrame.instructions.push(decode_stmt(it)) }

                            for (j in i downTo 0) {
                                val reverseStatement = statements[j]
                                if (reverseStatement is SwitchCase && !reverseStatement.isDefault) {
                                    forkFrame.instructions.push(tracehook { TracerRecord.Assertion(it.currentFrame.pop(), false) })
                                    forkFrame.instructions.push(equals)
                                    forkFrame.instructions.push(decode_expr(statement.expression))
                                    forkFrame.instructions.push(push(value))
                                }
                            }

                            forkFrame.instructions.push(tracehook { TracerRecord.Assertion(it.currentFrame.pop(), true) })
                            forkFrame.instructions.push(equals)
                            forkFrame.instructions.push(decode_expr(statement.expression))
                            forkFrame.instructions.push(push(value))
                        }
                    }
                }
            }
        }
    }
}

//
//  Tracing
//

class tracehook(val handle: (ExecutionContext) -> TracerRecord): InterpretedInstruction() {
    override fun execute(ctx: ExecutionContext, frame: InterpretedExecutionFrame) {
        val record = handle(ctx)
        ctx.records.add(record)
    }
}