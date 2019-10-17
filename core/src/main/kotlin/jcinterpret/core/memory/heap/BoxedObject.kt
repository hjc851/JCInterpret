package jcinterpret.core.memory.heap

import com.fasterxml.jackson.annotation.JsonTypeInfo
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.TypeSignature
import java.io.Serializable
import javax.xml.bind.annotation.XmlSeeAlso
import javax.xml.bind.annotation.XmlType

@XmlSeeAlso (
    BoxedStackValueObject::class,
    BoxedStringObject::class,
    ClassObject::class
)
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
abstract class BoxedObject<T> (
    id: Int,
    type: ClassTypeSignature,
    val value: T
): ObjectType(id, type, mutableMapOf()) {

    override fun getField(name: String, type: TypeSignature, ctx: ExecutionContext): Field {
        throw IllegalStateException("Boxed values do not have fields")
    }
}

//
//  Stack Values
//

@XmlType(name = "BoxedStackValueObject")
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

@XmlSeeAlso (
    ConcreteStringValue::class,
    SymbolicStringValue::class,
    StackValueStringValue::class,
    CompositeStringValue::class
)
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
abstract class StringValue: Serializable

@XmlType(name = "ConcreteStringValue") data class ConcreteStringValue(val value: String): StringValue()
@XmlType(name = "SymbolicStringValue") data class SymbolicStringValue(val value: Int): StringValue()
@XmlType(name = "StackValueStringValue") data class StackValueStringValue(val value: StackValue): StringValue()
@XmlType(name = "CompositeStringValue") data class CompositeStringValue(val lhs: StringValue, val rhs: StringValue): StringValue()

@XmlType(name = "BoxedStringObject")
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

@XmlType(name = "ClassObject")
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
class ClassObject (
    id: Int,
    type: ClassTypeSignature,
    value: TypeSignature
): BoxedObject<TypeSignature>(id, type, value) {
    override val lookupType: ClassTypeSignature
        get() = type as ClassTypeSignature
}