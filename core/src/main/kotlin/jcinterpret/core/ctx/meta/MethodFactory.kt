package jcinterpret.core.ctx.meta

import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.descriptors.MethodBindingMethodDescriptor
import jcinterpret.core.descriptors.MethodDescriptor

object MethodFactory {
    fun build(ctx: ExecutionContext, descriptor: MethodDescriptor): Method {
        if (descriptor.isAbstract)
            return OpaqueMethod(descriptor)

        if (descriptor is MethodBindingMethodDescriptor) {
            val declaration = ctx.sourceLibrary.getDeclaration(descriptor.qualifiedSignature)
            if (declaration != null) {
                return InterpretedMethod(descriptor, declaration)
            }
        }

        return OpaqueMethod(descriptor)
    }
}