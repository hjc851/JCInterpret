package jcinterpret.core.memory.stack

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

//
//  Concrete Values
//

@JsonSubTypes (
    JsonSubTypes.Type(StackByte::class),
    JsonSubTypes.Type(StackShort::class),
    JsonSubTypes.Type(StackInt::class),
    JsonSubTypes.Type(StackLong::class),
    JsonSubTypes.Type(StackFloat::class),
    JsonSubTypes.Type(StackDouble::class),
    JsonSubTypes.Type(StackChar::class),
    JsonSubTypes.Type(StackBoolean::class)
)
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
abstract class ConcreteValue<T: Any>: StackValue() {
    abstract val value: T
    abstract fun number(): Number

    override fun label(): String {
        return "${number()} ${type.name}"
    }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
data class StackByte(override val value: Byte) : ConcreteValue<Byte>() {
    override fun number(): Number = value
    override val type: StackType get() = StackType.BYTE

//    override fun <T, U> accept(visitor: StackValueVisitor<T, U>, arg: T): U {
//        return visitor.visit(this, arg)
//    }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
data class StackShort(override val value: Short) : ConcreteValue<Short>() {
    override fun number(): Number = value
    override val type: StackType get() = StackType.SHORT

//    override fun <T, U> accept(visitor: StackValueVisitor<T, U>, arg: T): U {
//        return visitor.visit(this, arg)
//    }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
data class StackInt(override val value: Int) : ConcreteValue<Int>() {
    override fun number(): Number = value
    override val type: StackType get() = StackType.INT

//    override fun <T, U> accept(visitor: StackValueVisitor<T, U>, arg: T): U {
//        return visitor.visit(this, arg)
//    }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
data class StackLong(override val value: Long) : ConcreteValue<Long>() {
    override fun number(): Number = value
    override val type: StackType get() = StackType.LONG

//    override fun <T, U> accept(visitor: StackValueVisitor<T, U>, arg: T): U {
//        return visitor.visit(this, arg)
//    }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
data class StackFloat(override val value: Float) : ConcreteValue<Float>() {
    override fun number(): Number = value
    override val type: StackType get() = StackType.FLOAT

//    override fun <T, U> accept(visitor: StackValueVisitor<T, U>, arg: T): U {
//        return visitor.visit(this, arg)
//    }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
data class StackDouble(override val value: Double) : ConcreteValue<Double>() {
    override fun number(): Number = value
    override val type: StackType get() = StackType.DOUBLE

//    override fun <T, U> accept(visitor: StackValueVisitor<T, U>, arg: T): U {
//        return visitor.visit(this, arg)
//    }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
data class StackChar(override val value: Char) : ConcreteValue<Char>() {
    override fun number(): Number = value.toInt()
    override val type: StackType get() = StackType.CHAR

//    override fun <T, U> accept(visitor: StackValueVisitor<T, U>, arg: T): U {
//        return visitor.visit(this, arg)
//    }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
data class StackBoolean(override val value: Boolean) : ConcreteValue<Boolean>() {
    override fun number(): Number = if (value) 1 else 0
    override val type: StackType get() = StackType.BOOLEAN

//    override fun <T, U> accept(visitor: StackValueVisitor<T, U>, arg: T): U {
//        return visitor.visit(this, arg)
//    }
}