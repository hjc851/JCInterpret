package jcinterpret.core.ctx.frame.interpreted

import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.memory.stack.*
import jcinterpret.core.trace.TraceRecord

object PrimaryOperationUtils {
    fun add(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val value = if (lhs is ConcreteValue<*> && rhs is ConcreteValue<*>) addConcrete(lhs, rhs)
        else BinaryOperationValue(lhs, rhs, lhs.type, BinaryOperator.ADD)

        ctx.records.add(TraceRecord.StackTransformation(lhs, rhs, value, BinaryOperator.ADD))
        return value
    }

    fun addConcrete(lhs: ConcreteValue<*>, rhs: ConcreteValue<*>): ConcreteValue<*> {

        if (lhs is StackChar || rhs is StackChar) {
            return StackInt(lhs.number().toInt() + rhs.number().toInt())
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
        val value = if (lhs is ConcreteValue<*> && rhs is ConcreteValue<*>) subConcrete(lhs, rhs)
        else BinaryOperationValue(lhs, rhs, lhs.type, BinaryOperator.SUB)

        ctx.records.add(TraceRecord.StackTransformation(lhs, rhs, value, BinaryOperator.SUB))
        return value
    }

    fun subConcrete(lhs: ConcreteValue<*>, rhs: ConcreteValue<*>): ConcreteValue<*> {

        if (lhs is StackChar || rhs is StackChar) {
            return StackInt(lhs.number().toInt() - rhs.number().toInt())
        }

        if (lhs is StackBoolean || rhs is StackBoolean) {
            TODO()
        }

        when {
            lhs is StackDouble || rhs is StackDouble -> return StackDouble(lhs.number().toDouble() - rhs.number().toDouble())
            lhs is StackFloat || rhs is StackFloat -> return StackFloat(lhs.number().toFloat() - rhs.number().toFloat())
            lhs is StackLong || rhs is StackLong -> return StackLong(lhs.number().toLong() - rhs.number().toLong())
            lhs is StackInt || rhs is StackInt -> return StackInt(lhs.number().toInt() - rhs.number().toInt())
            lhs is StackShort || rhs is StackShort -> return StackShort((lhs.number().toShort() - rhs.number().toShort()).toShort())
            lhs is StackByte || rhs is StackByte -> return StackByte((lhs.number().toByte() - rhs.number().toByte()).toByte())
        }

        TODO("Unknown or unsupported concrete values $lhs, $rhs")
    }

    fun mul(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val value = if (lhs is ConcreteValue<*> && rhs is ConcreteValue<*>) mulConcrete(lhs, rhs)
        else BinaryOperationValue(lhs, rhs, lhs.type, BinaryOperator.MUL)

        ctx.records.add(TraceRecord.StackTransformation(lhs, rhs, value, BinaryOperator.MUL))
        return value
    }

    fun mulConcrete(lhs: ConcreteValue<*>, rhs: ConcreteValue<*>): ConcreteValue<*> {

        if (lhs is StackChar || rhs is StackChar) {
            return StackInt(lhs.number().toInt() * rhs.number().toInt())
        }

        when {
            lhs is StackDouble || rhs is StackDouble -> return StackDouble(lhs.number().toDouble() * rhs.number().toDouble())
            lhs is StackFloat || rhs is StackFloat -> return StackFloat(lhs.number().toFloat() * rhs.number().toFloat())
            lhs is StackLong || rhs is StackLong -> return StackLong(lhs.number().toLong() * rhs.number().toLong())
            lhs is StackInt || rhs is StackInt -> return StackInt(lhs.number().toInt() * rhs.number().toInt())
            lhs is StackShort || rhs is StackShort -> return StackShort((lhs.number().toShort() * rhs.number().toShort()).toShort())
            lhs is StackByte || rhs is StackByte -> return StackByte((lhs.number().toByte() * rhs.number().toByte()).toByte())
        }

        TODO("Unknown or unsupported concrete values $lhs, $rhs")
    }

    fun div(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val value = if (lhs is ConcreteValue<*> && rhs is ConcreteValue<*>) divConcrete(lhs, rhs)
        else BinaryOperationValue(lhs, rhs, lhs.type, BinaryOperator.DIV)

        ctx.records.add(TraceRecord.StackTransformation(lhs, rhs, value, BinaryOperator.DIV))
        return value
    }

    fun divConcrete(lhs: ConcreteValue<*>, rhs: ConcreteValue<*>): ConcreteValue<*> {

        if (lhs is StackChar || rhs is StackChar) {
            return StackInt(lhs.number().toInt() / rhs.number().toInt())
        }

        when {
            lhs is StackDouble || rhs is StackDouble -> return StackDouble(lhs.number().toDouble() / rhs.number().toDouble())
            lhs is StackFloat || rhs is StackFloat -> return StackFloat(lhs.number().toFloat() / rhs.number().toFloat())
            lhs is StackLong || rhs is StackLong -> return StackLong(lhs.number().toLong() / rhs.number().toLong())
            lhs is StackInt || rhs is StackInt -> return StackInt(lhs.number().toInt() / rhs.number().toInt())
            lhs is StackShort || rhs is StackShort -> return StackShort((lhs.number().toShort() / rhs.number().toShort()).toShort())
            lhs is StackByte || rhs is StackByte -> return StackByte((lhs.number().toByte() / rhs.number().toByte()).toByte())
        }

        TODO("Unknown or unsupported concrete values $lhs, $rhs")
    }

    fun mod(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val value = if (lhs is ConcreteValue<*> && rhs is ConcreteValue<*>) modConcrete(lhs, rhs)
        else BinaryOperationValue(lhs, rhs, lhs.type, BinaryOperator.MOD)

        ctx.records.add(TraceRecord.StackTransformation(lhs, rhs, value, BinaryOperator.MOD))
        return value
    }

    fun modConcrete(lhs: ConcreteValue<*>, rhs: ConcreteValue<*>): ConcreteValue<*> {

        if (lhs is StackChar || rhs is StackChar) {
            return StackInt(lhs.number().toInt() % rhs.number().toInt())
        }

        when {
            lhs is StackDouble || rhs is StackDouble -> return StackDouble(lhs.number().toDouble() % rhs.number().toDouble())
            lhs is StackFloat || rhs is StackFloat -> return StackFloat(lhs.number().toFloat() % rhs.number().toFloat())
            lhs is StackLong || rhs is StackLong -> return StackLong(lhs.number().toLong() % rhs.number().toLong())
            lhs is StackInt || rhs is StackInt -> return StackInt(lhs.number().toInt() % rhs.number().toInt())
            lhs is StackShort || rhs is StackShort -> return StackShort((lhs.number().toShort() % rhs.number().toShort()).toShort())
            lhs is StackByte || rhs is StackByte -> return StackByte((lhs.number().toByte() % rhs.number().toByte()).toByte())
        }

        TODO("Unknown or unsupported concrete values $lhs, $rhs")
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

    fun greater(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val value = if (lhs is ConcreteValue<*> && rhs is ConcreteValue<*>) StackBoolean(lhs.number().toDouble() > rhs.number().toDouble())
        else BinaryOperationValue(lhs, rhs, StackType.BOOLEAN, BinaryOperator.GREATER)

        ctx.records.add(TraceRecord.StackTransformation(lhs, rhs, value, BinaryOperator.GREATER))
        return value
    }

    fun less(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val value = if (lhs is ConcreteValue<*> && rhs is ConcreteValue<*>) StackBoolean(lhs.number().toDouble() < rhs.number().toDouble())
        else BinaryOperationValue(lhs, rhs, StackType.BOOLEAN, BinaryOperator.LESS)

        ctx.records.add(TraceRecord.StackTransformation(lhs, rhs, value, BinaryOperator.LESS))
        return value
    }

    fun lessequals(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val value = if (lhs is ConcreteValue<*> && rhs is ConcreteValue<*>) StackBoolean(lhs.number().toDouble() <= rhs.number().toDouble())
        else BinaryOperationValue(lhs, rhs, StackType.BOOLEAN, BinaryOperator.LESSEQUALS)

        ctx.records.add(TraceRecord.StackTransformation(lhs, rhs, value, BinaryOperator.LESSEQUALS))
        return value
    }

    fun greaterequals(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val value = if (lhs is ConcreteValue<*> && rhs is ConcreteValue<*>) StackBoolean(lhs.number().toDouble() >= rhs.number().toDouble())
        else BinaryOperationValue(lhs, rhs, StackType.BOOLEAN, BinaryOperator.GREATEREQUALS)

        ctx.records.add(TraceRecord.StackTransformation(lhs, rhs, value, BinaryOperator.GREATEREQUALS))
        return value
    }

    fun eqls(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val value = if (lhs is ConcreteValue<*> && rhs is ConcreteValue<*>) StackBoolean(lhs.number().toDouble() == rhs.number().toDouble())
        else BinaryOperationValue(lhs, rhs, StackType.BOOLEAN, BinaryOperator.EQUALS)

        ctx.records.add(TraceRecord.StackTransformation(lhs, rhs, value, BinaryOperator.EQUALS))
        return value
    }

    fun neql(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val value = if (lhs is ConcreteValue<*> && rhs is ConcreteValue<*>) StackBoolean(lhs.number().toDouble() != rhs.number().toDouble())
        else BinaryOperationValue(lhs, rhs, StackType.BOOLEAN, BinaryOperator.NOTEQUALS)

        ctx.records.add(TraceRecord.StackTransformation(lhs, rhs, value, BinaryOperator.NOTEQUALS))
        return value
    }

    fun not(lhs: StackValue, ctx: ExecutionContext): StackValue {
        val value = if (lhs is StackBoolean) StackBoolean(!lhs.value)
        else NotValue(lhs)

        ctx.records.add(TraceRecord.NotValueTransformation(lhs, value))
        return value
    }

    fun or(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val value = if (lhs is StackBoolean && rhs is StackBoolean) StackBoolean(lhs.value || rhs.value)
        else BinaryOperationValue(lhs, rhs, StackType.BOOLEAN, BinaryOperator.AND)

        ctx.records.add(TraceRecord.StackTransformation(lhs, rhs, value, BinaryOperator.AND))
        return value
    }

    fun and(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val value = if (lhs is StackBoolean && rhs is StackBoolean) StackBoolean(lhs.value && rhs.value)
        else BinaryOperationValue(lhs, rhs, StackType.BOOLEAN, BinaryOperator.AND)

        ctx.records.add(TraceRecord.StackTransformation(lhs, rhs, value, BinaryOperator.AND))
        return value
    }

    fun xor(lhs: StackValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val value = if (lhs is StackBoolean && rhs is StackBoolean) StackBoolean(lhs.value.xor(rhs.value))
        else BinaryOperationValue(lhs, rhs, StackType.BOOLEAN, BinaryOperator.AND)

        ctx.records.add(TraceRecord.StackTransformation(lhs, rhs, value, BinaryOperator.AND))
        return value
    }
}