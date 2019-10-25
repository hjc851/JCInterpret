package jcinterpret.core.memory.heap

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.TypeSignature
import java.io.Serializable
import javax.xml.bind.annotation.XmlSeeAlso
import javax.xml.bind.annotation.XmlType

@JsonSubTypes (
    JsonSubTypes.Type(BoxedStackValueObject::class),
    JsonSubTypes.Type(BoxedStringObject::class),
    JsonSubTypes.Type(ClassObject::class)
)
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
abstract class BoxedObject<T> (
    id: Int,
    type: ClassTypeSignature,
    val value: T
): ObjectType(id, type, mutableMapOf()) {

    override fun getField(name: String, type: TypeSignature, intent: Intent, ctx: ExecutionContext): Field {
        throw IllegalStateException("Boxed values do not have fields")
    }
}

//
//  Stack Values
//

@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
class BoxedStackValueObject (
    id: Int,
    type: ClassTypeSignature,
    value: StackValue
): BoxedObject<StackValue>(id, type, value) {
    override val lookupType: ClassTypeSignature
        get() = type as ClassTypeSignature
}

//
//  String Values
//

@JsonSubTypes (
    JsonSubTypes.Type(ConcreteStringValue::class),
    JsonSubTypes.Type(SymbolicStringValue::class),
    JsonSubTypes.Type(StackValueStringValue::class),
    JsonSubTypes.Type(CompositeStringValue::class)
)
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
abstract class StringValue: Serializable {
    abstract fun label(): String
}

@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
data class ConcreteStringValue(val value: String): StringValue() {
    override fun label(): String {
        if (value.isEmpty()) return "EMPTY"
        else return value
    }
}

@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
data class SymbolicStringValue(val value: Int): StringValue() {
    override fun label(): String {
        return "#$value STRING"
    }
}

@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
data class StackValueStringValue(val value: StackValue): StringValue() {
    override fun label(): String {
        return value.label()
    }
}

@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
data class CompositeStringValue(val lhs: StringValue, val rhs: StringValue): StringValue() {
    override fun label(): String {
        return "#CS:" + System.identityHashCode(this) // lhs.label() + " + " + rhs.label()
    }
}

@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
class BoxedStringObject (
    id: Int,
    type: ClassTypeSignature,
    value: StringValue
): BoxedObject<StringValue>(id, type, value) {
    override val lookupType: ClassTypeSignature
        get() = type as ClassTypeSignature
}

//
//  Class Value
//

@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
class ClassObject (
    id: Int,
    type: ClassTypeSignature,
    value: TypeSignature
): BoxedObject<TypeSignature>(id, type, value) {
    override val lookupType: ClassTypeSignature
        get() = type as ClassTypeSignature
}