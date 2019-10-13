package jcinterpret.core.ctx.frame.synthetic

import jcinterpret.core.control.ClassAreaFault
import jcinterpret.core.control.HaltException
import jcinterpret.core.control.ReturnException
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.ctx.frame.Instruction
import jcinterpret.core.memory.heap.ObjectType
import jcinterpret.core.memory.stack.StackReference
import jcinterpret.core.trace.TracerRecord
import jcinterpret.signature.*

abstract class SyntheticInstruction: Instruction() {
    abstract fun execute(ctx: ExecutionContext, frame: SyntheticExecutionFrame)
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

class InvokeVirtual(val sig: MethodSignature): SyntheticInstruction() {
    override fun execute(ctx: ExecutionContext, frame: SyntheticExecutionFrame) {
        validateSignatures(ctx, sig)

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
    }
}

//
//  Allocation
//

class AllocateSymbolic(val type: TypeSignature): SyntheticInstruction() {
    override fun execute(ctx: ExecutionContext, frame: SyntheticExecutionFrame) {
        validateSignatures(ctx, type)

        val symb = ctx.heapArea.allocateSymbolic(ctx, type)
        frame.push(symb)
    }
}

class ValidateClassDependencies(val sig: ClassTypeSignature) : SyntheticInstruction() {
    override fun execute(ctx: ExecutionContext, frame: SyntheticExecutionFrame) {
        val desc = ctx.descriptorLibrary.getDescriptor(sig)
        val sigs = mutableSetOf<Signature>()

        if (desc.superclass != null)
            sigs.add(desc.superclass!!)

        for (iface in desc.interfaces)
            sigs.add(iface)

        for ((id, field) in desc.fields)
            sigs.add(field.type)

        for ((key, method) in desc.methods) {
            sigs.add(method.signature.typeSignature.returnType)

            for (arg in method.signature.typeSignature.argumentTypes)
                sigs.add(arg)

            for (exception in method.exceptions)
                sigs.add(exception)
        }

        validateSignatures(ctx, *sigs.toTypedArray())
        println("Validated class ${sig}")
    }
}

class AllocateClassType(val sig: ClassTypeSignature) : SyntheticInstruction() {
    override fun execute(ctx: ExecutionContext, frame: SyntheticExecutionFrame) {
        val desc = ctx.descriptorLibrary.getDescriptor(sig)
        ctx.classArea.allocateClassType(ctx, desc)
    }
}

//
//  Tracing
//

class Tracehook(val handle: (ExecutionContext) -> TracerRecord): SyntheticInstruction() {
    override fun execute(ctx: ExecutionContext, frame: SyntheticExecutionFrame) {
        val record = handle(ctx)
        ctx.records.add(record)
    }
}