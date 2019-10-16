package jcinterpret.core.memory.stack

//
//  Concrete Values
//

abstract class ConcreteValue<T: Any>: StackValue() {
    abstract val value: T
    abstract fun number(): Number
}

data class StackByte(override val value: Byte) : ConcreteValue<Byte>() {
    override fun number(): Number = value
    override val type: StackType get() = StackType.BYTE
}

data class StackShort(override val value: Short) : ConcreteValue<Short>() {
    override fun number(): Number = value
    override val type: StackType get() = StackType.SHORT
}

data class StackInt(override val value: Int) : ConcreteValue<Int>() {
    override fun number(): Number = value
    override val type: StackType get() = StackType.INT
}

data class StackLong(override val value: Long) : ConcreteValue<Long>() {
    override fun number(): Number = value
    override val type: StackType get() = StackType.LONG
}

data class StackFloat(override val value: Float) : ConcreteValue<Float>() {
    override fun number(): Number = value
    override val type: StackType get() = StackType.FLOAT
}

data class StackDouble(override val value: Double) : ConcreteValue<Double>() {
    override fun number(): Number = value
    override val type: StackType get() = StackType.DOUBLE
}

data class StackChar(override val value: Char) : ConcreteValue<Char>() {
    override fun number(): Number = value.toInt()
    override val type: StackType get() = StackType.CHAR
}

data class StackBoolean(override val value: Boolean) : ConcreteValue<Boolean>() {
    override fun number(): Number = if (value) 1 else 0
    override val type: StackType get() = StackType.BOOLEAN
}