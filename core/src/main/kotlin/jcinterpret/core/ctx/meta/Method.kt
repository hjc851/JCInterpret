package jcinterpret.core.ctx.meta

import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.ctx.frame.interpreted.*
import jcinterpret.core.descriptors.MethodDescriptor
import jcinterpret.core.memory.stack.ReferenceValue
import jcinterpret.core.memory.stack.StackReference
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.core.trace.TracerRecord
import jcinterpret.signature.PrimitiveTypeSignature
import jcinterpret.signature.QualifiedMethodSignature
import jcinterpret.signature.ReferenceTypeSignature
import org.eclipse.jdt.core.dom.BodyDeclaration
import org.eclipse.jdt.core.dom.MethodDeclaration
import org.eclipse.jdt.core.dom.SingleVariableDeclaration
import java.util.*

abstract class Method(val desc: MethodDescriptor) {

    val sig: QualifiedMethodSignature
        get() = desc.qualifiedSignature

    abstract fun invoke(ctx: ExecutionContext, selfRef: ReferenceValue?, params: Array<StackValue>)
}

val MAX_RECURSIVE_CALL_COUNT = 1

class InterpretedMethod(desc: MethodDescriptor, val decl: MethodDeclaration): Method(desc) {
    override fun invoke(ctx: ExecutionContext, selfRef: ReferenceValue?, params: Array<StackValue>) {

        val instructions = Stack<InterpretedInstruction>()
        val operands = Stack<StackValue>()
        val locals = Locals()
        val exceptionScopes = Stack<ExceptionScope>()
        val breakScopes = Stack<BreakScope>()

        if (!desc.isStatic) {
            val thistype = ctx.descriptorLibrary.getDescriptor(sig.declaringClassSignature)
            locals.allocate("this", thistype)
            locals.assign("this", selfRef!!)
        }

        for (i in 0 until params.size) {
            val declaredParameter = decl.parameters()[i] as SingleVariableDeclaration
            val name = declaredParameter.name.identifier

            val ptype = sig.methodSignature.typeSignature.argumentTypes[i]
            val param = params[i]

            val pdesc = ctx.descriptorLibrary.getDescriptor(ptype)
            locals.allocate(name, pdesc)
            locals.assign(name, param)
        }

        instructions.push(decode_stmt(decl.body))

        val callCount = ctx.countMethodOccuranceInCallStack(sig)
        val returnType = sig.methodSignature.typeSignature.returnType
        if (callCount > MAX_RECURSIVE_CALL_COUNT) {
            val result: StackValue?

            if (returnType != PrimitiveTypeSignature.VOID) {
                val signature = desc.qualifiedSignature
                val declaringClass = signature.declaringClassSignature

                result = if (selfRef != null && declaringClass == returnType) selfRef
                else ctx.heapArea.allocateSymbolic(ctx, returnType)
            } else {
                result = null
            }

            if (result != null)
                ctx.currentFrame.push(result)

            if (desc.isStatic) ctx.records.add(TracerRecord.StaticLibraryMethodCall(desc.qualifiedSignature, params, result))
            else ctx.records.add(TracerRecord.InstanceLibraryMethodCall(desc.qualifiedSignature, selfRef!!, params, result))
        } else {
            val frame = InterpretedExecutionFrame(instructions, operands, locals, exceptionScopes, breakScopes, desc.qualifiedSignature)
            ctx.frames.push(frame)
        }
    }
}

class OpaqueMethod(desc: MethodDescriptor): Method(desc) {
    override fun invoke(ctx: ExecutionContext, selfRef: ReferenceValue?, params: Array<StackValue>) {

        val returnType = desc.signature.typeSignature.returnType
        val frame = ctx.currentFrame
        val result: StackValue?

        if (returnType != PrimitiveTypeSignature.VOID) {
            val signature = desc.qualifiedSignature
            val declaringClass = signature.declaringClassSignature

            result = if (selfRef != null && declaringClass == returnType) selfRef
            else ctx.heapArea.allocateSymbolic(ctx, returnType)
        } else {
            result = null
        }

        if (result != null)
            frame.push(result)

        if (desc.isStatic) ctx.records.add(TracerRecord.StaticLibraryMethodCall(desc.qualifiedSignature, params, result))
        else ctx.records.add(TracerRecord.InstanceLibraryMethodCall(desc.qualifiedSignature, selfRef!!, params, result))
    }
}