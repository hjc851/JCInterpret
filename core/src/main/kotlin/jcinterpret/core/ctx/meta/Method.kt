package jcinterpret.core.ctx.meta

import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.descriptors.MethodDescriptor
import jcinterpret.core.memory.stack.StackReference
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.core.trace.TracerRecord
import jcinterpret.signature.PrimitiveTypeSignature
import jcinterpret.signature.QualifiedMethodSignature

abstract class Method(val sig: QualifiedMethodSignature) {
    abstract fun invoke(ctx: ExecutionContext, selfRef: StackReference?, params: Array<StackValue>)
}

class InterpretedMethod(val desc: MethodDescriptor, sig: QualifiedMethodSignature): Method(sig) {
    override fun invoke(ctx: ExecutionContext, selfRef: StackReference?, params: Array<StackValue>) {

        TODO()

//        val decl = ctx.resolver.ctx.declarations.getMethod(sig.toString())
//
//        val instructions = Stack<InterpretedInstruction>()
//        val operands = Stack<StackValue>()
//        val locals = Locals()
//        val exceptionScopes = Stack<ExceptionScope>()
//        val breakScopes = Stack<Pair<String?, Int>>()
//
//        if (!desc.isStatic) {
//            locals.allocate("this", desc.declaringClass.signature)
//            locals.assign("this", selfRef!!)
//        }
//
//        for (i in 0 until params.size) {
//            val declaredParameter = decl.parameters()[i] as SingleVariableDeclaration
//            val name = declaredParameter.name.identifier
//
//            val ptype = sig.methodSignature.typeSignature.argumentTypes[i]
//            val param = params[i]
//
//            if (ptype is PrimitiveTypeSignature && param is StackReference ||
//                ptype is ReferenceTypeSignature && param !is StackReference)
//                TODO("Handle boxing")
//
//            if (declaredParameter.isVarargs)
//                TODO("Varargs - make array of (i until params.size) + break")
//
//            locals.allocate(name, ptype)
//            locals.assign(name, param)
//        }
//
//        instructions.push(decode_stmt(decl.body))
//
//        val frame = InterpretedExecutionContextFrame(instructions, operands, locals, exceptionScopes, breakScopes, desc)
//        ctx.frames.push(frame)
    }
}

class OpaqueMethod(val desc: MethodDescriptor, sig: QualifiedMethodSignature): Method(sig) {
    override fun invoke(ctx: ExecutionContext, selfRef: StackReference?, params: Array<StackValue>) {

        if (desc.isStatic && selfRef != null)
            throw IllegalStateException("Attempting to call static method with this value")

        if (!desc.isStatic && selfRef == null)
            throw IllegalStateException("Attempting to call instance method without this value")

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