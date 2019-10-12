package jcinterpret.core.memory.heap

import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.signature.ArrayTypeSignature
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.ReferenceTypeSignature
import jcinterpret.signature.TypeSignature

abstract class ObjectType (
    override val id: Int,
    override val type: ReferenceTypeSignature,
    val fields: MutableMap<String, Field>
): HeapValue() {
    fun store(name: String, type: TypeSignature, value: StackValue, ctx: ExecutionContext) {
        TODO()
    }

    fun load(name: String, type: TypeSignature, ctx: ExecutionContext): StackValue {
        TODO()
    }
}

class ConcreteObject (
    id: Int,
    type: ClassTypeSignature,
    fields: MutableMap<String, Field>
): ObjectType(id, type, fields) {
    override val lookupType: ClassTypeSignature
        get() = type as ClassTypeSignature
}

class SymbolicObject (
    id: Int,
    type: ClassTypeSignature,
    fields: MutableMap<String, Field>
): ObjectType(id, type, fields) {
    override val lookupType: ClassTypeSignature
        get() = type as ClassTypeSignature
}

class SymbolicArray (
    id: Int,
    type: ArrayTypeSignature,
    val storage: MutableMap<StackValue, StackValue>
): ObjectType(id, type, mutableMapOf()) {
    override val lookupType: ClassTypeSignature
        get() = ClassTypeSignature.OBJECT

    fun put(index: StackValue, value: StackValue, type: TypeSignature, ctx: ExecutionContext) {
        TODO()
    }

    fun get(index: StackValue, type: TypeSignature, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun length(): StackValue {
        TODO()
    }
}