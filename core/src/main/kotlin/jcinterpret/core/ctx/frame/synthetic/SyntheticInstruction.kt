package jcinterpret.core.ctx.frame.synthetic

import jcinterpret.core.control.ClassAreaFault
import jcinterpret.core.control.HaltException
import jcinterpret.core.control.ReturnException
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.signature.*

abstract class SyntheticInstruction {
    abstract fun execute(ctx: ExecutionContext, frame: SyntheticExecutionFrame)

    fun validateSignatures(ctx: ExecutionContext, vararg signatures: Signature) {
        val unloaded = mutableSetOf<ClassTypeSignature>()

        fun validate(sig: Signature) {
            when (sig) {
                is ClassTypeSignature -> if (!ctx.classArea.isClassLoaded(sig))
                    unloaded.add(sig)

                is ArrayTypeSignature -> validate(sig.componentType)

                is QualifiedMethodSignature -> {
                    validate(sig.declaringClassSignature)
                    validate(sig.methodSignature)
                }

                is MethodSignature -> validate(sig.typeSignature)

                is MethodTypeSignature -> {
                    validate(sig.returnType)
                    for (arg in sig.argumentTypes)
                        validate(arg)
                }
            }
        }

        for (sig in signatures)
            validate(sig)

        if (unloaded.isNotEmpty())
            throw ClassAreaFault(unloaded)
    }
}

//
//  Linkage
//

object ReturnVoid: SyntheticInstruction() {
    override fun execute(ctx: ExecutionContext, frame: SyntheticExecutionFrame) {
        throw ReturnException(null, null)
    }
}

object ReturnValue: SyntheticInstruction() {
    override fun execute(ctx: ExecutionContext, frame: SyntheticExecutionFrame) {
        val value = frame.pop()
        throw ReturnException(value, null)
    }
}

object Halt : SyntheticInstruction() {
    override fun execute(ctx: ExecutionContext, frame: SyntheticExecutionFrame) {
        throw HaltException("HALT")
    }
}

//
//  Invoke
//


class InvokeStatic(val sig: QualifiedMethodSignature): SyntheticInstruction() {
    override fun execute(ctx: ExecutionContext, frame: SyntheticExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class InvokeVirtual(val sig: MethodSignature): SyntheticInstruction() {
    override fun execute(ctx: ExecutionContext, frame: SyntheticExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

//
//  Allocation
//

class AllocateSymbolic(val type: TypeSignature): SyntheticInstruction() {
    override fun execute(ctx: ExecutionContext, frame: SyntheticExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class ValidateClassDependencies(val sig: ClassTypeSignature) : SyntheticInstruction() {
    override fun execute(ctx: ExecutionContext, frame: SyntheticExecutionFrame) {
TODO()
//        val desc = ctx.library.getDescriptor(sig)
//        val sigs = mutableSetOf<Signature>()
//
//        if (desc.superclass != null)
//            sigs.add(desc.superclass!!.signature)
//
//        for (iface in desc.interfaces)
//            sigs.add(iface.signature)
//
//        for ((id, field) in desc.fields)
//            sigs.add(field.type)
//
//        for ((key, method) in desc.methods) {
//            sigs.add(method.signature.typeSignature.returnType)
//
//            for (arg in method.signature.typeSignature.argumentTypes)
//                sigs.add(arg)
//
//            for (exception in method.exceptions)
//                sigs.add(exception.signature)
//        }
//
//        validateSignatures(ctx, *sigs.toTypedArray())
//        println("Validated class ${sig}")
    }
}

class AllocateClassSpace(val sig: ClassTypeSignature) : SyntheticInstruction() {
    override fun execute(ctx: ExecutionContext, frame: SyntheticExecutionFrame) {
        TODO()
//        val desc = ctx.resolver.resolveClassDescriptor(sig)
//        ctx.classArea.allocateClassType(ctx, desc)
    }
}

//
//  Tracing
//

class Tracehook(val handle: (ExecutionContext) -> Unit): SyntheticInstruction() {
    override fun execute(ctx: ExecutionContext, frame: SyntheticExecutionFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}