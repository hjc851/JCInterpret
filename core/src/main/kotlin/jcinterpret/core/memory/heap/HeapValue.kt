package jcinterpret.core.memory.heap

import jcinterpret.core.memory.stack.StackReference
import jcinterpret.core.signature.ClassTypeSignature
import jcinterpret.core.signature.ReferenceTypeSignature

abstract class HeapValue {
    abstract val id: Int
    abstract val type: ReferenceTypeSignature

    abstract val lookupType: ClassTypeSignature

    fun ref() = StackReference(id)
}

