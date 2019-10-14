package jcinterpret.core.memory.heap

import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.core.memory.stack.SymbolicValue
import jcinterpret.core.trace.TracerRecord
import jcinterpret.signature.ArrayTypeSignature
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.ReferenceTypeSignature
import jcinterpret.signature.TypeSignature

abstract class ObjectType (
    override val id: Int,
    override val type: ReferenceTypeSignature,
    val fields: MutableMap<String, Field>
): HeapValue() {

    protected abstract fun getField(name: String, type: TypeSignature, ctx: ExecutionContext): Field

    fun store(name: String, type: TypeSignature, value: StackValue, ctx: ExecutionContext) {
        val field = getField(name, type, ctx)
        field.value = value
    }

    fun load(name: String, type: TypeSignature, ctx: ExecutionContext): StackValue {
        val field = getField(name, type, ctx)
        return field.value
    }
}

class ConcreteObject (
    id: Int,
    type: ClassTypeSignature,
    fields: MutableMap<String, Field>
): ObjectType(id, type, fields) {
    override val lookupType: ClassTypeSignature
        get() = type as ClassTypeSignature

    override fun getField(name: String, type: TypeSignature, ctx: ExecutionContext): Field {
        return fields.getOrPut(name) {
            val desc = ctx.descriptorLibrary.getDescriptor(type)
            Field(name, type, desc.defaultValue)
        }
    }
}

class SymbolicObject (
    id: Int,
    type: ClassTypeSignature,
    fields: MutableMap<String, Field>
): ObjectType(id, type, fields) {
    override val lookupType: ClassTypeSignature
        get() = type as ClassTypeSignature

    override fun getField(name: String, type: TypeSignature, ctx: ExecutionContext): Field {
        return fields.getOrPut(name) {
            val desc = ctx.descriptorLibrary.getDescriptor(type)
            val value = ctx.heapArea.allocateSymbolic(ctx, type)
            ctx.records.add(TracerRecord.ObjectFieldPut(this.ref(), name, type, desc.defaultValue, value))
            Field(name, type, value)
        }
    }
}

class SymbolicArray (
    id: Int,
    type: ArrayTypeSignature,
    val storage: MutableMap<StackValue, StackValue>,
    val size: SymbolicValue
): ObjectType(id, type, mutableMapOf()) {
    override val lookupType: ClassTypeSignature
        get() = ClassTypeSignature.OBJECT

    fun put(index: StackValue, value: StackValue, type: TypeSignature, ctx: ExecutionContext) {
        storage[index] = value
    }

    fun get(index: StackValue, type: TypeSignature, ctx: ExecutionContext): StackValue {
        if (!storage.containsKey(index)) {
            storage[index] = ctx.heapArea.allocateSymbolic(ctx, type)
        }

        return storage[index]!!
    }

    override fun getField(name: String, type: TypeSignature, ctx: ExecutionContext): Field {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun length(): StackValue {
        return size
    }
}