package jcinterpret.core.ctx.meta

import com.sun.tools.classfile.Code_attribute
import jcinterpret.core.ExecutionConfig
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.ctx.frame.bytecode.BytecodeExecutionFrame
import jcinterpret.core.ctx.frame.interpreted.*
import jcinterpret.core.descriptors.ClassFileMethodDescriptor
import jcinterpret.core.descriptors.MethodDescriptor
import jcinterpret.core.memory.stack.ReferenceValue
import jcinterpret.core.memory.stack.StackInt
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.core.trace.TraceRecord
import jcinterpret.signature.PrimitiveTypeSignature
import jcinterpret.signature.QualifiedMethodSignature
import org.eclipse.jdt.core.dom.MethodDeclaration
import org.eclipse.jdt.core.dom.SingleVariableDeclaration
import org.eclipse.jdt.internal.core.ClassFile
import java.util.*

abstract class Method(val desc: MethodDescriptor) {

    val sig: QualifiedMethodSignature
        get() = desc.qualifiedSignature

    abstract fun invoke(ctx: ExecutionContext, selfRef: ReferenceValue?, params: Array<StackValue>)
}

class ProjectBytecodeMethod(desc: ClassFileMethodDescriptor): Method(desc) {
    override fun invoke(ctx: ExecutionContext, selfRef: ReferenceValue?, params: Array<StackValue>) {
        val desc = desc as ClassFileMethodDescriptor
        val cp = desc.cf.constant_pool
        val method = desc.method
        val code = method.attributes.map.getValue("Code") as Code_attribute

        val stack = Array<StackValue>(code.max_stack) { StackInt(0) }
        val locals = Array<StackValue>(code.max_locals) { StackInt(0) }

        var offset = 0
        if (!desc.isStatic) {
            locals[offset++] = selfRef!!
        }

        for (i in 0 until params.size) {
            locals[i+offset] = params[i]
        }

        val frame = BytecodeExecutionFrame(desc.qualifiedSignature, stack, locals, cp, code.code)
        ctx.frames.push(frame)
    }
}

class InterpretedMethod(desc: MethodDescriptor, val decl: MethodDeclaration): Method(desc) {
    override fun invoke(ctx: ExecutionContext, selfRef: ReferenceValue?, params: Array<StackValue>) {

        val instructions = Stack<InterpretedInstruction>()
        val operands = Stack<StackValue>()
        val locals = Locals()
        val exceptionScopes = Stack<ExceptionScope>()
        val breakScopes = Stack<BreakScope>()
        val continueScopes = Stack<ContinueScope>()

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

        val callCount = ctx.countMethodOccurenceInCallStack(sig)
        val returnType = sig.methodSignature.typeSignature.returnType
        if (callCount > ExecutionConfig.maxRecursiveCalls) {
            val result: StackValue?

            if (returnType != PrimitiveTypeSignature.VOID) {
                val signature = desc.qualifiedSignature
                val declaringClass = signature.declaringClassSignature

                result = if (selfRef != null && declaringClass == returnType) selfRef
                else ctx.heapArea.allocateSymbolic(ctx, returnType)
            } else {
                result = null
            }

            if (result != null) {
                ctx.currentFrame.push(result)
                ctx.records.add(TraceRecord.SynthesisedReturnValue(desc.qualifiedSignature, result))
            }

            if (desc.isStatic) ctx.records.add(TraceRecord.StaticLibraryMethodCall(desc.qualifiedSignature, params, result))
            else ctx.records.add(TraceRecord.InstanceLibraryMethodCall(desc.qualifiedSignature, selfRef!!, params, result))
        } else {
            val frame = InterpretedExecutionFrame(instructions, operands, locals, exceptionScopes, breakScopes, continueScopes, desc.qualifiedSignature)
            ctx.frames.push(frame)
        }
    }
}

class OpaqueMethod(desc: MethodDescriptor): Method(desc) {
    override fun invoke(ctx: ExecutionContext, selfRef: ReferenceValue?, params: Array<StackValue>) {

        val returnType = desc.signature.typeSignature.returnType
        val result: StackValue?

        if (returnType != PrimitiveTypeSignature.VOID) {
            val signature = desc.qualifiedSignature
            val declaringClass = signature.declaringClassSignature

            result = if (selfRef != null && declaringClass == returnType) selfRef
            else ctx.heapArea.allocateSymbolic(ctx, returnType)
        } else {
            result = null
        }

        if (result != null) {
            ctx.currentFrame.push(result)
            ctx.records.add(TraceRecord.SynthesisedReturnValue(desc.qualifiedSignature, result))
        }

        if (desc.isStatic) ctx.records.add(TraceRecord.StaticLibraryMethodCall(desc.qualifiedSignature, params, result))
        else ctx.records.add(TraceRecord.InstanceLibraryMethodCall(desc.qualifiedSignature, selfRef!!, params, result))
    }
}