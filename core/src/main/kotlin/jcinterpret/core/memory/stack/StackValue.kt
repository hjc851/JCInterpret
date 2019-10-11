package jcinterpret.core.memory.stack

abstract class StackValue {
    abstract val type: StackType
}

enum class StackType {
    REFERENCE,
    BYTE,
    SHORT,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    CHAR,
    BOOLEAN
}