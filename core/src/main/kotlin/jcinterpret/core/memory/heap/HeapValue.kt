package jcinterpret.core.memory.heap

import jcinterpret.core.memory.stack.StackReference
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.ReferenceTypeSignature

abstract class HeapValue {
    abstract val id: Int
    abstract val type: ReferenceTypeSignature

    abstract val lookupType: ClassTypeSignature

    fun ref() = StackReference(id)
}

