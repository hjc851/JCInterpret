package jcinterpret.core.memory.heap

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.memory.stack.ConcreteValue
import jcinterpret.core.memory.stack.StackInt
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.core.memory.stack.SymbolicValue
import jcinterpret.core.trace.TraceRecord
import jcinterpret.signature.ArrayTypeSignature
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.ReferenceTypeSignature
import jcinterpret.signature.TypeSignature

@JsonSubTypes (
    JsonSubTypes.Type(ConcreteObject::class),
    JsonSubTypes.Type(SymbolicObject::class),
    JsonSubTypes.Type(SymbolicArray::class),
    JsonSubTypes.Type(BoxedObject::class)
)
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
abstract class ObjectType (
    override val id: Int,
    override val type: ReferenceTypeSignature,
    val fields: MutableMap<String, Field>
): HeapValue() {

    protected enum class Intent {
        GET,
        SET
    }

    protected abstract fun getField(name: String, type: TypeSignature, intent: Intent, ctx: ExecutionContext): Field

    fun store(name: String, type: TypeSignature, value: StackValue, ctx: ExecutionContext) {
        val field = getField(name, type, Intent.SET, ctx)
        field.value = value
    }

    fun load(name: String, type: TypeSignature, ctx: ExecutionContext): StackValue {
        val field = getField(name, type, Intent.GET, ctx)
        return field.value
    }
}

@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
class ConcreteObject (
    id: Int,
    type: ClassTypeSignature,
    fields: MutableMap<String, Field>
): ObjectType(id, type, fields) {
    override val lookupType: ClassTypeSignature
        get() = type as ClassTypeSignature

    override fun getField(name: String, type: TypeSignature, intent: Intent, ctx: ExecutionContext): Field {
        return fields.getOrPut(name) {
            val desc = ctx.descriptorLibrary.getDescriptor(type)
            val field = if (intent == Intent.GET) {
                val default = ctx.heapArea.allocateSymbolic(ctx, type)
                ctx.records.add(TraceRecord.DefaultInstanceFieldValue(ref(), name, default))
                Field(name, type, default)
            } else {
                Field(name, type, desc.defaultValue)
            }
            return@getOrPut field
        }
    }
}

@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
class SymbolicObject (
    id: Int,
    type: ClassTypeSignature,
    fields: MutableMap<String, Field>
): ObjectType(id, type, fields) {
    override val lookupType: ClassTypeSignature
        get() = type as ClassTypeSignature

    override fun getField(name: String, type: TypeSignature, intent: Intent, ctx: ExecutionContext): Field {
        return fields.getOrPut(name) {
            return fields.getOrPut(name) {
                val desc = ctx.descriptorLibrary.getDescriptor(type)
                val field = if (intent == Intent.GET) {
                    val default = ctx.heapArea.allocateSymbolic(ctx, type)
                    ctx.records.add(TraceRecord.DefaultInstanceFieldValue(ref(), name, default))
                    Field(name, type, default)
                } else {
                    Field(name, type, desc.defaultValue)
                }
                return@getOrPut field
            }
        }
    }
}

@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
class SymbolicArray (
    id: Int,
    type: ArrayTypeSignature,
    val storage: MutableMap<StackValue, StackValue>,
    val size: SymbolicValue
): ObjectType(id, type, mutableMapOf()) {
    override val lookupType: ClassTypeSignature
        get() = ClassTypeSignature.OBJECT

    val componentType: TypeSignature
        get() = (type as ArrayTypeSignature).componentType

    fun put(index: StackValue, value: StackValue, type: TypeSignature, ctx: ExecutionContext) {
        storage[index] = value
    }

    fun get(index: StackValue, type: TypeSignature, ctx: ExecutionContext): StackValue {
        if (!storage.containsKey(index)) {
            storage[index] = ctx.heapArea.allocateSymbolic(ctx, type)
        }

        return storage[index]!!
    }

    override fun getField(name: String, type: TypeSignature, intent: Intent, ctx: ExecutionContext): Field {
        throw IllegalStateException("Arrays do not have fields")
    }

    fun length(): StackValue {
        val onlyContainsConcreteIndicies = storage.keys
            .firstOrNull { it !is ConcreteValue<*> } == null

        return when {
            this.storage.isEmpty() -> size
            onlyContainsConcreteIndicies -> StackInt(storage.size)
            else -> size
        }
    }
}