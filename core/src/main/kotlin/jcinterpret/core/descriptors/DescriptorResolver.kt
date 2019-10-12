package jcinterpret.core.descriptors

import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.QualifiedMethodSignature

abstract class DescriptorResolver {
    abstract fun tryResolveDescriptor(sig: ClassTypeSignature): ClassTypeDescriptor?
    abstract fun tryResolveDescriptor(sig: QualifiedMethodSignature): MethodDescriptor?

    fun resolveDescriptor(sig: ClassTypeSignature): ClassTypeDescriptor {
        val descriptor = tryResolveDescriptor(sig) ?: throw UnresolvableDescriptorException(sig)
        return descriptor
    }

    fun resolveDescriptor(sig: QualifiedMethodSignature): MethodDescriptor {
        val descriptor = tryResolveDescriptor(sig) ?: throw UnresolvableDescriptorException(sig)
        return descriptor
    }
}