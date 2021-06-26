package jcinterpret.core.ctx.meta

import jcinterpret.core.JavaConcolicInterpreterFactory
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.descriptors.ClassFileMethodDescriptor
import jcinterpret.core.descriptors.MethodBindingMethodDescriptor
import jcinterpret.core.descriptors.MethodDescriptor

object MethodFactory {
    fun build(ctx: ExecutionContext, descriptor: MethodDescriptor): Method {
        if (descriptor.isAbstract)
            return OpaqueMethod(descriptor)

        if (ctx.executionMode == JavaConcolicInterpreterFactory.ExecutionMode.PROJECT_BYTECODE) {
            if (descriptor is ClassFileMethodDescriptor && !descriptor.isAbstract) {
                val isProjectDescriptor = ctx.bytecodeLibrary.getDeclaration(descriptor.qualifiedSignature) != null
                if (isProjectDescriptor) {
                    return ProjectBytecodeMethod(descriptor)
                }
            }
        }

        if (descriptor is MethodBindingMethodDescriptor) {
            val declaration = ctx.sourceLibrary.getDeclaration(descriptor.qualifiedSignature)
            if (declaration != null) {
                return InterpretedMethod(descriptor, declaration)
            }
        }

        return OpaqueMethod(descriptor)
    }
}