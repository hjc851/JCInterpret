package jcinterpret.core.memory.stack

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import javax.xml.bind.annotation.XmlSeeAlso
import javax.xml.bind.annotation.XmlType

//
//  Compound Value
//

@JsonSubTypes (
    JsonSubTypes.Type(NotValue::class),
    JsonSubTypes.Type(CastValue::class),
    JsonSubTypes.Type(BinaryOperationValue::class)
)
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
abstract class ComputedValue: StackValue()

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
class NotValue (val value: StackValue): ComputedValue() {
    override val type: StackType get() = value.type
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
class CastValue (val value: StackValue, override val type: StackType): ComputedValue()

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
class BinaryOperationValue (val lhs: StackValue, val rhs: StackValue, override val type: StackType, val operator: BinaryOperator): ComputedValue()

enum class BinaryOperator {
    ADD,
    SUB,
    MUL,
    DIV,
    MOD,

    LSHIFT,
    RSHIFT,
    RSHIFT_UNSIGNED,

    EQUALS,
    NOTEQUALS,

    AND,
    OR,
    XOR,

    LESS,
    GREATER,
    LESSEQUALS,
    GREATEREQUALS,

    CONCAT
}