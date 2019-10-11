package jcinterpret.core.memory.stack

abstract class StackValue {
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