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
            type == BoxedTypeSignature.VOID -> allocateSymbolicBoxedObject(ctx, type, PrimitiveTypeSignature.VOID)
            type == BoxedTypeSignature.CHARACTER -> allocateSymbolicBoxedObject(ctx, type, PrimitiveTypeSignature.CHAR)
            type == BoxedTypeSignature.BOOLEAN -> allocateSymbolicBoxedObject(ctx, type, PrimitiveTypeSignature.BOOLEAN)
            type == BoxedTypeSignature.BYTE -> allocateSymbolicBoxedObject(ctx, type, PrimitiveTypeSignature.BYTE)
            type == BoxedTypeSignature.SHORT -> allocateSymbolicBoxedObject(ctx, type, PrimitiveTypeSignature.SHORT)
            type == BoxedTypeSignature.INTEGER -> allocateSymbolicBoxedObject(ctx, type, PrimitiveTypeSignature.INT)
            type == BoxedTypeSignature.LONG -> allocateSymbolicBoxedObject(ctx, type, PrimitiveTypeSignature.LONG)
            type == BoxedTypeSignature.FLOAT -> allocateSymbolicBoxedObject(ctx, type, PrimitiveTypeSignature.FLOAT)
            type == BoxedTypeSignature.DOUBLE -> allocateSymbolicBoxedObject(ctx, type, PrimitiveTypeSignature.DOUBLE)

            type.className == "java/lang/String"    -> allocateSymbolicString(ctx)

            else -> SymbolicObject(counter.getAndIncrement(), type, mutableMapOf())
        }

        storage[obj.id] = obj
        return obj
    }

    fun allocateSymbolicArray(ctx: ExecutionContext, type: ArrayTypeSignature): SymbolicArray {
//        val id = counter.getAndIncrement()
//        val arr = SymbolicArray(id, type, mutableMapOf(), allocateSymbol(ctx, PrimitiveTypeSignature.INT))
//        storage[id] = arr
//        return arr
        TODO()
    }

    private fun allocateSymbolicBoxedObject(ctx: ExecutionContext, objType: ClassTypeSignature, type: PrimitiveTypeSignature): BoxedStackValueObject {
//        val symbol = allocateSymbol(ctx, type)
//        val obj = BoxedStackValue(counter.getAndIncrement(), objType, symbol)
//        return obj
        TODO()
    }

    fun getOrAllocateString(value: String): BoxedStringObject {
//        var id = literalCache[value]
//
//        if (id == null) {
//            val value = ConcreteStringValue(value)
//            val obj = StringObject(counter.getAndIncrement(), value)
//            id = obj.id
//            storage[id] = obj
//        }
//
//        return storage[id] as StringObject
        TODO()
    }

    private fun allocateSymbolicString(ctx: ExecutionContext): BoxedStringObject {
//        return StringObject(counter.getAndIncrement(), SymbolicStringValue(counter.getAndIncrement()))
        TODO()
    }

    //
    //  Objects
    //

    fun allocateObject(ctx: ExecutionContext, type: ClassTypeSignature): ConcreteObject {
//        val cls = ctx.classArea.getClass(type)
//        val fields = mutableListOf<Field>()
//
//        var currentClass: ClassTypeDescriptor? = cls.descriptor
//        while (currentClass != null) {
//            val currentClassSignature = currentClass.signature
//            currentClass.fields.values
//                .filter { !it.isStatic }
//                .map { Field(currentClassSignature, it.name, it.type, false, it.type.defaultValue) }
//                .toCollection(fields)
//
//            currentClass = currentClass.superclass
//        }
//
//        val fieldStorage = fields.map { it.name to it }.toMap()
//        val id = counter.getAndIncrement()
//        val obj = ConcreteObject(id, type, fieldStorage)
//        storage[id] = obj
//        return obj
        TODO()
    }

    //
    //  Deref
    //

    fun dereference(ref: StackReference): HeapValue = dereference(ref.id)
    fun dereference(id: Int): HeapValue = storage[id]!!
}