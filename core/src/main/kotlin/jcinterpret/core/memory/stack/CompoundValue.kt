package jcinterpret.core.memory.stack

//
//  Compound Value
//

abstract class ComputedValue: StackValue()

class NotValue (val value: StackValue): ComputedValue() {
    override val type: StackType get() = value.type
}

class BinaryValue(val lhs: StackValue, val rhs: StackValue, override val type: StackType, val operator: BinaryOperator): ComputedValue()

enum class BinaryOperator {
    ADD,
    SUB,
    MUL,
    DIV,
    REM,

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
    GREATEREQUALS
}