package jcinterpret.core.ctx.meta

import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.memory.heap.*
import jcinterpret.core.memory.stack.StackReference
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.core.memory.stack.SymbolicValue
import jcinterpret.signature.*
import java.util.concurrent.atomic.AtomicInteger

class HeapArea (
    val counter: AtomicInteger,
    val storage: MutableMap<Int, HeapValue>,
    val literalRefs: MutableMap<Any, Int>
) {
    //
    //  Validate counter must be greater than 0
    //
    init {
        if (counter.get() <= 0)
            throw IllegalStateException("Heap counter cannot be less than 1")
    }

    //
    //  Symbolic
    //

    fun allocateSymbolic(ctx: ExecutionContext, type: TypeSignature): StackValue {
        return when (type) {
            is PrimitiveTypeSignature -> allocateSymbol(ctx, type)
            is ClassTypeSignature -> allocateSymbolicObject(ctx, type).ref()
            is ArrayTypeSignature -> allocateSymbolicArray(ctx, type).ref()

            else -> throw IllegalStateException("Unknown type signature $type (${type::class.java})")
        }
    }

    fun allocateSymbol(ctx: ExecutionContext, type: PrimitiveTypeSignature): SymbolicValue {
        val desc = ctx.descriptorLibrary.getDescriptor(type)
        return SymbolicValue(counter.getAndIncrement(), desc.stackType)
    }

    fun allocateSymbolicObject(ctx: ExecutionContext, type: ClassTypeSignature): ObjectType {
        val obj = when {
            type == BoxedTypeSignature.CHARACTER    -> allocateSymbolicBoxedObject(ctx, type, PrimitiveTypeSignature.CHAR)
            type == BoxedTypeSignature.BOOLEAN      -> allocateSymbolicBoxedObject(ctx, type, PrimitiveTypeSignature.BOOLEAN)
            type == BoxedTypeSignature.BYTE         -> allocateSymbolicBoxedObject(ctx, type, PrimitiveTypeSignature.BYTE)
            type == BoxedTypeSignature.SHORT        -> allocateSymbolicBoxedObject(ctx, type, PrimitiveTypeSignature.SHORT)
            type == BoxedTypeSignature.INTEGER      -> allocateSymbolicBoxedObject(ctx, type, PrimitiveTypeSignature.INT)
            type == BoxedTypeSignature.LONG         -> allocateSymbolicBoxedObject(ctx, type, PrimitiveTypeSignature.LONG)
            type == BoxedTypeSignature.FLOAT        -> allocateSymbolicBoxedObject(ctx, type, PrimitiveTypeSignature.FLOAT)
            type == BoxedTypeSignature.DOUBLE       -> allocateSymbolicBoxedObject(ctx, type, PrimitiveTypeSignature.DOUBLE)

            type.className == "java/lang/String"    -> allocateSymbolicString(ctx)

            else -> SymbolicObject(counter.getAndIncrement(), type, mutableMapOf())
        }

        storage[obj.id] = obj
        return obj
    }

    fun allocateSymbolicArray(ctx: ExecutionContext, type: ArrayTypeSignature): SymbolicArray {
        val id = counter.getAndIncrement()
        val arr = SymbolicArray(id, type, mutableMapOf(), allocateSymbol(ctx, PrimitiveTypeSignature.INT))
        storage[id] = arr
        return arr
    }

    private fun allocateSymbolicBoxedObject(ctx: ExecutionContext, objType: ClassTypeSignature, type: PrimitiveTypeSignature): BoxedStackValueObject {
        val symbol = allocateSymbol(ctx, type)
        val obj = BoxedStackValueObject(counter.getAndIncrement(), objType, symbol)
        return obj
    }

    fun getOrAllocateString(str: String): BoxedStringObject {
        var id = literalRefs[str]

        if (id == null) {
            val value = ConcreteStringValue(str)
            val obj = BoxedStringObject(counter.getAndIncrement(), ClassTypeSignature("java/lang/String"), value)
            id = obj.id

            literalRefs[str] = id
            storage[id] = obj
        }

        return storage[id] as BoxedStringObject
    }

    private fun allocateSymbolicString(ctx: ExecutionContext): BoxedStringObject {
        val id = counter.getAndIncrement()
        val value = SymbolicStringValue(counter.getAndIncrement())

        val obj = BoxedStringObject(id, ClassTypeSignature("java/lang/String"), value)
        storage[id] = obj

        return obj
    }

    //
    //  Objects
    //

    fun allocateObject(ctx: ExecutionContext, type: ClassTypeSignature): ConcreteObject {
        val id = counter.getAndIncrement()
        val obj = ConcreteObject(id, type, mutableMapOf())
        storage[id] = obj
        return obj
    }

    fun promote(ctx: ExecutionContext, value: StackReference, type: ReferenceTypeSignature) {

        val id = value.id
        val existing = storage[id]!! as ObjectType

        val obj = when {
            type == BoxedTypeSignature.CHARACTER    -> BoxedStackValueObject(id, type as ClassTypeSignature, allocateSymbol(ctx, PrimitiveTypeSignature.CHAR))
            type == BoxedTypeSignature.BOOLEAN      -> BoxedStackValueObject(id, type as ClassTypeSignature, allocateSymbol(ctx, PrimitiveTypeSignature.BOOLEAN))
            type == BoxedTypeSignature.BYTE         -> BoxedStackValueObject(id, type as ClassTypeSignature, allocateSymbol(ctx, PrimitiveTypeSignature.BYTE))
            type == BoxedTypeSignature.SHORT        -> BoxedStackValueObject(id, type as ClassTypeSignature, allocateSymbol(ctx, PrimitiveTypeSignature.SHORT))
            type == BoxedTypeSignature.INTEGER      -> BoxedStackValueObject(id, type as ClassTypeSignature, allocateSymbol(ctx, PrimitiveTypeSignature.INT))
            type == BoxedTypeSignature.LONG         -> BoxedStackValueObject(id, type as ClassTypeSignature, allocateSymbol(ctx, PrimitiveTypeSignature.LONG))
            type == BoxedTypeSignature.FLOAT        -> BoxedStackValueObject(id, type as ClassTypeSignature, allocateSymbol(ctx, PrimitiveTypeSignature.FLOAT))
            type == BoxedTypeSignature.DOUBLE       -> BoxedStackValueObject(id, type as ClassTypeSignature, allocateSymbol(ctx, PrimitiveTypeSignature.DOUBLE))

            type.toString() == ("Ljava/lang/String;")    -> BoxedStringObject(id, type as ClassTypeSignature, SymbolicStringValue(counter.getAndIncrement()))

            type is ArrayTypeSignature -> SymbolicArray(id, type, mutableMapOf(), allocateSymbol(ctx, PrimitiveTypeSignature.INT))
            type is ClassTypeSignature -> TODO()

            else -> TODO()
        }

        existing.fields.toMap(obj.fields)
        storage[id] = obj
    }

    //
    //  Deref
    //

    fun dereference(ref: StackReference): HeapValue = dereference(ref.id)
    fun dereference(id: Int): HeapValue = storage[id]!!
}