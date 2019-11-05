package jcinterpret.core.memory.stack

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.Serializable

@JsonSubTypes (
    JsonSubTypes.Type(ReferenceValue::class),
    JsonSubTypes.Type(ConcreteValue::class),
    JsonSubTypes.Type(SymbolicValue::class),
    JsonSubTypes.Type(ComputedValue::class)
)
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
abstract class StackValue: Serializable {
    abstract val type: StackType

    abstract fun label(): String

//    abstract fun <T, U> accept(visitor: StackValueVisitor<T, U>, arg: T): U
}

//interface StackValueVisitor<T, U> {
//    fun visit(value: SymbolicValue, arg: T): U
//
//    fun visit(value: StackReference, arg: T): U
//    fun visit(value: StackNil, arg: T): U
//
//    fun visit(value: StackByte, arg: T): U
//    fun visit(value: StackShort, arg: T): U
//    fun visit(value: StackInt, arg: T): U
//    fun visit(value: StackLong, arg: T): U
//    fun visit(value: StackFloat, arg: T): U
//    fun visit(value: StackDouble, arg: T): U
//    fun visit(value: StackChar, arg: T): U
//    fun visit(value: StackBoolean, arg: T): U
//
//    fun visit(value: NotValue, arg: T): U
//    fun visit(value: CastValue, arg: T): U
//    fun visit(value: BinaryOperationValue, arg: T): U
//}

enum class StackType {
    REFERENCE,
    VOID,
    BYTE,
    SHORT,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    CHAR,
    BOOLEAN
}