package jcinterpret.core.memory.stack

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.Serializable

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
data class NotValue (val value: StackValue): ComputedValue() {
    override val type: StackType get() = value.type

    override fun label(): String {
        return "!${value.label()}"
    }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
data class CastValue (val value: StackValue, override val type: StackType): ComputedValue() {
    override fun label(): String {
        return "(${type.name}) ${value.label()}"
    }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
data class BinaryOperationValue (val lhs: StackValue, val rhs: StackValue, override val type: StackType, val operator: BinaryOperator): ComputedValue() {
    override fun label(): String {
        return "${lhs.label()} ${operator.op} ${rhs.label()}"
    }
}

interface Operator: Serializable

object NotOperator: Operator
object ConcatOperator: Operator
object CastOperator: Operator
object StringifyOperator: Operator

enum class BinaryOperator(val op: String): Operator {
    ADD("+"),
    SUB("-"),
    MUL("*"),
    DIV("/"),
    MOD("%"),

    LSHIFT("<<"),
    RSHIFT(">>"),
    RSHIFT_UNSIGNED(">>>"),

    EQUALS("=="),
    NOTEQUALS("!="),

    AND("&&"),
    OR("||"),
    XOR("^"),

    LESS("<"),
    GREATER(">"),
    LESSEQUALS("<="),
    GREATEREQUALS(">="),

//    CONCAT("CONCAT")
}