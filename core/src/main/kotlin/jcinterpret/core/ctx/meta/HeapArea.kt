package jcinterpret.core.ctx.meta

import jcinterpret.core.control.HaltException
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.memory.heap.*
import jcinterpret.core.memory.stack.*
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

    fun getOrBox(value: StackValue): BoxedStackValueObject {
        var id = literalRefs[value]

        if (id == null) {
            val type = boxedTypeForStackType(value.type)
            id = counter.getAndIncrement()
            val obj = BoxedStackValueObject(id, type, value)

            storage[id] = obj
            literalRefs[value] = id
        }

        return storage[id] as BoxedStackValueObject
    }

    private fun boxedTypeForStackType(type: StackType): ClassTypeSignature {
        return when (type) {
            StackType.REFERENCE -> ClassTypeSignature.OBJECT
            StackType.VOID      -> BoxedTypeSignature.VOID
            StackType.BYTE      -> BoxedTypeSignature.BYTE
            StackType.SHORT     -> BoxedTypeSignature.SHORT
            StackType.INT       -> BoxedTypeSignature.INTEGER
            StackType.LONG      -> BoxedTypeSignature.LONG
            StackType.FLOAT     -> BoxedTypeSignature.FLOAT
            StackType.DOUBLE    -> BoxedTypeSignature.DOUBLE
            StackType.CHAR      -> BoxedTypeSignature.CHARACTER
            StackType.BOOLEAN   -> BoxedTypeSignature.BOOLEAN
        }
    }

    fun allocateSymbolicArray(ctx: ExecutionContext, type: ArrayTypeSignature): SymbolicArray {
        val id = counter.getAndIncrement()
        val arr = SymbolicArray(id, type, mutableMapOf(), allocateSymbol(ctx, PrimitiveTypeSignature.INT))
        storage[id] = arr
        return arr
    }

    fun allocateSymbolicBoxedObject(ctx: ExecutionContext, objType: ClassTypeSignature, type: PrimitiveTypeSignature): BoxedStackValueObject {
        val symbol = allocateSymbol(ctx, type)
        val obj = BoxedStackValueObject(counter.getAndIncrement(), objType, symbol)
        storage[obj.id] = obj
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

    fun getOrAllocateClassObject(type: TypeSignature): ClassObject {
        var id = literalRefs[type]

        if (id == null) {
            id = counter.getAndIncrement()
            val obj = ClassObject(id, ClassTypeSignature("java/lang/Class"), type)

            storage[id] = obj
            literalRefs[type] = id
        }

        return storage[id] as ClassObject
    }

    fun allocateSymbolicString(ctx: ExecutionContext, value: StringValue? = null): BoxedStringObject {
        val id = counter.getAndIncrement()
        val value = value ?: SymbolicStringValue(counter.getAndIncrement())

        val obj = BoxedStringObject(id, ClassTypeSignature("java/lang/String"), value)
        storage[id] = obj

        return obj
    }

    //
    //  Concrete
    //

    fun allocateObject(ctx: ExecutionContext, type: ClassTypeSignature): ObjectType {

        if (type.className == "java/lang/String")
            return getOrAllocateString("")

        val id = counter.getAndIncrement()
        val obj = ConcreteObject(id, type, mutableMapOf())
        storage[id] = obj
        return obj
    }

    //
    //  Promotion
    //

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

            type.toString() == "Ljava/lang/String;"     -> BoxedStringObject(id, type as ClassTypeSignature, SymbolicStringValue(counter.getAndIncrement()))
            type.toString() == "Ljava/lang/Class;"      -> TODO()

            type is ArrayTypeSignature -> SymbolicArray(id, type, mutableMapOf(), allocateSymbol(ctx, PrimitiveTypeSignature.INT))

            type is ClassTypeSignature -> SymbolicObject(id, type, existing.fields)

            else -> throw IllegalStateException("Unexpected object type for promotion ${existing.javaClass}, ${type}")
        }

        existing.fields.toMap(obj.fields)
        storage[obj.id] = obj
    }

    //
    //  Deref
    //

    fun dereference(ref: ReferenceValue): HeapValue = dereference(ref.id)
    fun dereference(id: Int): HeapValue {
        if (id == 0)
            throw HaltException("Null pointer exception")
        else
            return storage[id]!!
    }
}