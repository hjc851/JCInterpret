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
}

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