package jcinterpret.core.descriptors

import jcinterpret.core.bytecode.BytecodeLibrary
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.QualifiedMethodSignature

class BytecodeLibraryDescriptorResolver(
    val bcLibrary: BytecodeLibrary
): DescriptorResolver() {
    override fun tryResolveDescriptor(sig: ClassTypeSignature): ClassTypeDescriptor? {
        return bcLibrary.getDeclaration(sig)
    }

    override fun tryResolveDescriptor(sig: QualifiedMethodSignature): MethodDescriptor? {
        return bcLibrary.getDeclaration(sig)
    }
}