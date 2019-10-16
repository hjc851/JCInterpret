package jcinterpret.core.ctx.frame.interpreted

import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.memory.heap.*
import jcinterpret.core.memory.stack.*
import jcinterpret.core.trace.TracerRecord

object ObjectOperatorUtils {
    fun add(lhs: StackReference, rhs: StackReference, ctx: ExecutionContext): StackValue {
        val lobj = ctx.heapArea.dereference(lhs)
        val robj = ctx.heapArea.dereference(rhs)

        if (lobj is BoxedStringObject) {
            val lstr = lobj.value

            val rstr = if (robj is BoxedStringObject) {
                robj.value
            } else if (robj is BoxedStackValueObject) {
                StackValueStringValue(robj.value).apply {
                    ctx.records.add(TracerRecord.Stringification(robj.value, this))
                }
            } else {
                StackValueStringValue(robj.ref()).apply {
                    ctx.records.add(TracerRecord.Stringification(robj.ref(), this))
                }
            }

            val value = if (lstr is ConcreteStringValue && rstr is ConcreteStringValue)
                ConcreteStringValue(lstr.value + rstr.value)
            else if (lstr is ConcreteStringValue && rstr is StackValueStringValue && rstr.value is ConcreteValue<*>)
                ConcreteStringValue(lstr.value + rstr.value.value)
            else
                CompositeStringValue(lstr, rstr)

            val str = ctx.heapArea.allocateSymbolicString(ctx, value)
            val ref = str.ref()

            ctx.records.add(TracerRecord.StringConcat(lstr, rstr, value))
            ctx.records.add(TracerRecord.StackTransformation(lhs, rhs, ref, BinaryOperator.CONCAT))

            return ref

        } else if (lobj is BoxedStackValueObject) {
            TODO()
        } else {
            TODO()
        }
    }

    fun add(lhs: StackReference, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val lobj = ctx.heapArea.dereference(lhs)

        if (lobj is BoxedStringObject) {

            val lstr = lobj.value
            val rstr = StackValueStringValue(rhs)
            ctx.records.add(TracerRecord.Stringification(rhs, rstr))

            val value = if (lstr is ConcreteStringValue && rhs is ConcreteValue<*>)
                ConcreteStringValue(lstr.value + rhs.value)
            else
                CompositeStringValue(lstr, rstr)

            val str = ctx.heapArea.allocateSymbolicString(ctx, value)
            val ref = str.ref()

            ctx.records.add(TracerRecord.StringConcat(lstr, rstr, value))
            ctx.records.add(TracerRecord.StackTransformation(lhs, rhs, ref, BinaryOperator.CONCAT))

            return ref

        } else if (lobj is BoxedStackValueObject) {
            TODO()
        } else {
            TODO()
        }
    }

    fun add(lhs: StackValue, rhs: StackReference, ctx: ExecutionContext): StackValue {

        val robj = ctx.heapArea.dereference(rhs)

        if (robj is BoxedStringObject) {
            val lstr = if (lhs is ConcreteValue<*>) ConcreteStringValue(lhs.value.toString())
            else StackValueStringValue(lhs)
            ctx.records.add(TracerRecord.Stringification(lhs, lstr))

            val rstr = robj.value

            val value = if (lhs is ConcreteValue<*> && rstr is ConcreteStringValue) ConcreteStringValue(lhs.value.toString() + rstr.value)
            else CompositeStringValue(lstr, rstr)

            val str = ctx.heapArea.allocateSymbolicString(ctx, value)
            val ref = str.ref()

            ctx.records.add(TracerRecord.StringConcat(lstr, rstr, value))
            ctx.records.add(TracerRecord.StackTransformation(lhs, rhs, ref, BinaryOperator.CONCAT))

            return ref
        }


        TODO()
    }

    fun greaterequals(lhs: ReferenceValue, rhs: ReferenceValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun greaterequals(lhs: ReferenceValue, rhs: StackValue, ctx: ExecutionContext): StackValue {

        val lobj = ctx.heapArea.dereference(lhs)

        if (lobj is BoxedStackValueObject) {
            val lvalue = lobj.value

            val result = if (lvalue is ConcreteValue<*> && rhs is ConcreteValue<*>)
                StackBoolean(lvalue.number().toDouble() >= rhs.number().toDouble())
            else
                BinaryOperationValue(lvalue, rhs, StackType.BOOLEAN, BinaryOperator.GREATEREQUALS)

            ctx.records.add(TracerRecord.StackTransformation(lvalue, rhs, result, BinaryOperator.GREATEREQUALS))
            return result
        }

        TODO()
    }

    fun greaterequals(lhs: StackValue, rhs: ReferenceValue, ctx: ExecutionContext): StackValue {
        TODO()
    }
}