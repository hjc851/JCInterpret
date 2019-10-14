package jcinterpret.core.ctx.frame.interpreted

import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.memory.stack.*

object PrimaryOperationUtils {
    fun add(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {

        if (lhs is ConcreteValue<*> && rhs is ConcreteValue<*>)
            return addConcrete(lhs, rhs)

        TODO()
    }

    fun addConcrete(lhs: ConcreteValue<*>, rhs: ConcreteValue<*>): ConcreteValue<*> {

        if (lhs is StackChar || rhs is StackChar) {
            TODO()
        }

        if (lhs is StackBoolean || rhs is StackBoolean) {
            TODO()
        }

        when {
            lhs is StackDouble || rhs is StackDouble -> return StackDouble(lhs.number().toDouble() + rhs.number().toDouble())
            lhs is StackFloat || rhs is StackFloat -> return StackFloat(lhs.number().toFloat() + rhs.number().toFloat())
            lhs is StackLong || rhs is StackLong -> return StackLong(lhs.number().toLong() + rhs.number().toLong())
            lhs is StackInt || rhs is StackInt -> return StackInt(lhs.number().toInt() + rhs.number().toInt())
            lhs is StackShort || rhs is StackShort -> return StackShort((lhs.number().toShort() + rhs.number().toShort()).toShort())
            lhs is StackByte || rhs is StackByte -> return StackByte((lhs.number().toByte() + rhs.number().toByte()).toByte())
        }

        TODO("Unknown or unsupported concrete values $lhs, $rhs")
    }

    fun sub(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun mul(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun div(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun rem(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun shl(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun shr(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun shru(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun not(lhs: StackValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun grtr(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun less(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun lequ(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun grel(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun eqls(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun neql(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun or(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun and(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun xor(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        TODO()
    }
}