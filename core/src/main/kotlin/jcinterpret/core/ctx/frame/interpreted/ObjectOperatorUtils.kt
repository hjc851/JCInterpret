package jcinterpret.core.ctx.frame.interpreted

import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.ctx.frame.interpreted.ObjectOperatorUtils.getStringValue
import jcinterpret.core.memory.heap.*
import jcinterpret.core.memory.stack.*
import jcinterpret.core.trace.TraceRecord

object ObjectOperatorUtils {

    fun HeapValue.getStringValue(ctx: ExecutionContext): StringValue {
        if (this is BoxedStringObject) return this.value

        if (this is BoxedStackValueObject) {
            val str = this.value.toStringValue()

            ctx.records.add(TraceRecord.Stringification(this.value, str))
            return str
        }

        val str = StackValueStringValue(this.ref())
        ctx.records.add(TraceRecord.Stringification(this.ref(), str))
        return str
    }

    fun StackValue.toStringValue(): StringValue {
        return if (this is ConcreteValue<*>) ConcreteStringValue(this.value.toString())
        else StackValueStringValue(this)
    }

    fun add (lhs: StringValue, rhs: StringValue, ctx: ExecutionContext): StackValue {
        val value = if (lhs is ConcreteStringValue && rhs is ConcreteStringValue) ConcreteStringValue(lhs.value + rhs.value)
        else CompositeStringValue(lhs, rhs)

        val str = ctx.heapArea.allocateSymbolicString(ctx, value)
        val ref = str.ref()

        ctx.records.add(TraceRecord.StringConcat(lhs, rhs, value))
        return ref
    }

    fun add(lhs: StackReference, rhs: StackReference, ctx: ExecutionContext): StackValue {
        val lobj = ctx.heapArea.dereference(lhs)
        val robj = ctx.heapArea.dereference(rhs)

        if (lobj is BoxedStringObject || robj is BoxedStringObject) {
            val lstr = lobj.getStringValue(ctx)
            val rstr = robj.getStringValue(ctx)

            return add(lstr, rstr, ctx)
        }

        if (lobj is BoxedStackValueObject && robj is BoxedStackValueObject) {
            val lvalue = lobj.value
            val rvalue = robj.value

            return PrimaryOperationUtils.add(lvalue, rvalue, ctx)
        }

        TODO()
    }

    fun add(lhs: StackReference, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val lobj = ctx.heapArea.dereference(lhs)

        if (lobj is BoxedStringObject) {
            val rstr = rhs.toStringValue()
            ctx.records.add(TraceRecord.Stringification(rhs, rstr))
            return ObjectOperatorUtils.add(lobj.value, rstr, ctx)

        } else if (lobj is BoxedStackValueObject) {

            val lval = lobj.value
            return PrimaryOperationUtils.add(lval, rhs, ctx)

        } else {
            TODO()
        }
    }

    fun add(lhs: StackValue, rhs: StackReference, ctx: ExecutionContext): StackValue {

        val robj = ctx.heapArea.dereference(rhs)

        if (robj is BoxedStringObject) {
            val lstr = lhs.toStringValue()
            ctx.records.add(TraceRecord.Stringification(lhs, lstr))
            return add(lstr, robj.value, ctx)

        } else if (robj is BoxedStackValueObject) {
            val rval = robj.value
            return PrimaryOperationUtils.add(lhs, rval, ctx)
        }

        TODO()
    }

    fun sub(lhs: ReferenceValue, rhs: ReferenceValue, ctx: ExecutionContext): StackValue {
        val lobj = ctx.heapArea.dereference(lhs)
        val robj = ctx.heapArea.dereference(rhs)

        if (lobj is BoxedStackValueObject && robj is BoxedStackValueObject) {
            val lval = lobj.value
            val rval = robj.value

            return PrimaryOperationUtils.sub(lval, rval, ctx)
        }

        TODO()
    }

    fun sub(lhs: StackValue, rhs: ReferenceValue, ctx: ExecutionContext): StackValue {
        val robj = ctx.heapArea.dereference(rhs)

        if (robj is BoxedStackValueObject) {
            val lval = lhs
            val rval = robj.value

            return PrimaryOperationUtils.sub(lval, rval, ctx)
        }

        TODO()
    }

    fun sub(lhs: ReferenceValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val lobj = ctx.heapArea.dereference(lhs)

        if (lobj is BoxedStackValueObject) {
            val lval = lobj.value
            val rval = rhs

            return PrimaryOperationUtils.sub(lval, rval, ctx)
        }

        TODO()
    }

    fun less(lhs: ReferenceValue, rhs: ReferenceValue, ctx: ExecutionContext): StackValue {
        val lobj = ctx.heapArea.dereference(lhs)
        val robj = ctx.heapArea.dereference(rhs)

        TODO()
    }

    fun less(lhs: ReferenceValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val lobj = ctx.heapArea.dereference(lhs)

        if (lobj is BoxedStackValueObject) {
            val lvalue = lobj.value

            val result = if (lvalue is ConcreteValue<*> && rhs is ConcreteValue<*>)
                StackBoolean(lvalue.number().toDouble() < rhs.number().toDouble())
            else
                BinaryOperationValue(lvalue, rhs, StackType.BOOLEAN, BinaryOperator.LESS)

            ctx.records.add(TraceRecord.StackTransformation(lvalue, rhs, result, BinaryOperator.LESS))
            return result
        }


        TODO()
    }

    fun less(lhs: StackValue, rhs: ReferenceValue, ctx: ExecutionContext): StackValue {
        val robj = ctx.heapArea.dereference(rhs)

        if (robj is BoxedStackValueObject) {
            val rvalue = robj.value

            return PrimaryOperationUtils.less(lhs, rvalue, ctx)
        }

        TODO()
    }

    fun equals(lhs: StackValue, rhs: ReferenceValue, ctx: ExecutionContext): StackValue {
        val robj = ctx.heapArea.dereference(rhs)

        if (robj is BoxedStackValueObject) {
            val rvalue = robj.value

            val result = if (rvalue is ConcreteValue<*> && lhs is ConcreteValue<*>)
                StackBoolean(rvalue.number().toDouble() == lhs.number().toDouble())
            else
                BinaryOperationValue(rvalue, lhs, StackType.BOOLEAN, BinaryOperator.EQUALS)

            return result
        }

        TODO()
    }

    fun equals(lhs: ReferenceValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val lobj = ctx.heapArea.dereference(lhs)

        if (lobj is BoxedStackValueObject) {
            val lvalue = lobj.value

            val result = if (lvalue is ConcreteValue<*> && rhs is ConcreteValue<*>)
                StackBoolean(lvalue.number().toDouble() == rhs.number().toDouble())
            else
                BinaryOperationValue(lvalue, rhs, StackType.BOOLEAN, BinaryOperator.EQUALS)

            return result
        }

        TODO()
    }

    fun greater(lhs: ReferenceValue, rhs: ReferenceValue, ctx: ExecutionContext): StackValue {
        val lobj = ctx.heapArea.dereference(lhs)
        val robj = ctx.heapArea.dereference(rhs)

        if (lobj is BoxedStackValueObject && robj is BoxedStackValueObject) {
            val lvalue = lobj.value
            val rvalue = robj.value

            return PrimaryOperationUtils.greater(lvalue, rvalue, ctx)
        }

        TODO()
    }

    fun greater(lhs: ReferenceValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val lobj = ctx.heapArea.dereference(lhs)

        if (lobj is BoxedStackValueObject) {
            val lvalue = lobj.value

            val result = if (lvalue is ConcreteValue<*> && rhs is ConcreteValue<*>)
                StackBoolean(lvalue.number().toDouble() > rhs.number().toDouble())
            else
                BinaryOperationValue(lvalue, rhs, StackType.BOOLEAN, BinaryOperator.GREATER)

            ctx.records.add(TraceRecord.StackTransformation(lvalue, rhs, result, BinaryOperator.GREATER))
            return result
        }

        TODO()
    }

    fun greater(lhs: StackValue, rhs: ReferenceValue, ctx: ExecutionContext): StackValue {
        TODO()
    }

    fun greaterequals(lhs: ReferenceValue, rhs: ReferenceValue, ctx: ExecutionContext): StackValue {
        val lobj = ctx.heapArea.dereference(lhs)
        val robj = ctx.heapArea.dereference(rhs)

        if (lobj is BoxedStackValueObject && robj is BoxedStackValueObject) {
            val lvalue = lobj.value
            val rvalue = robj.value

            return PrimaryOperationUtils.greaterequals(lvalue, rvalue, ctx)
        }

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

            ctx.records.add(TraceRecord.StackTransformation(lvalue, rhs, result, BinaryOperator.GREATEREQUALS))
            return result
        }

        TODO()
    }

    fun greaterequals(lhs: StackValue, rhs: ReferenceValue, ctx: ExecutionContext): StackValue {
        val robj = ctx.heapArea.dereference(rhs)

        if (robj is BoxedStackValueObject) {
            val lval = lhs
            val rval = robj.value

            return PrimaryOperationUtils.greaterequals(lval, rval, ctx)
        }

        TODO()
    }

    fun div(lhs: ReferenceValue, rhs: ReferenceValue, ctx: ExecutionContext): StackValue {
        val lobj = ctx.heapArea.dereference(lhs)
        val robj = ctx.heapArea.dereference(rhs)

        if (lobj is BoxedStackValueObject && robj is BoxedStackValueObject) {
            val lval = lobj.value
            val rval = robj.value

            return PrimaryOperationUtils.div(lval, rval, ctx)
        }

        TODO()
    }

    fun div(lhs: StackValue, rhs: ReferenceValue, ctx: ExecutionContext): StackValue {
        val robj = ctx.heapArea.dereference(rhs)

        if (robj is BoxedStackValueObject) {
            val lval = lhs
            val rval = robj.value

            return PrimaryOperationUtils.div(lval, rval, ctx)
        }

        TODO()
    }

    fun div(lhs: ReferenceValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val lobj = ctx.heapArea.dereference(lhs)

        if (lobj is BoxedStackValueObject) {
            val lval = lobj.value
            val rval = rhs

            return PrimaryOperationUtils.div(lval, rval, ctx)
        }

        TODO()
    }

    fun or(lhs: ReferenceValue, rhs: ReferenceValue, ctx: ExecutionContext): StackValue {
        val lobj = ctx.heapArea.dereference(lhs)
        val robj = ctx.heapArea.dereference(rhs)

        if (lobj is BoxedStackValueObject && robj is BoxedStackValueObject) {
            val lval = lobj.value
            val rval = robj.value

            return PrimaryOperationUtils.or(lval, rval, ctx)
        }

        TODO()
    }

    fun or(lhs: StackValue, rhs: ReferenceValue, ctx: ExecutionContext): StackValue {
        val robj = ctx.heapArea.dereference(rhs)

        if (robj is BoxedStackValueObject) {
            val lval = lhs
            val rval = robj.value

            return PrimaryOperationUtils.or(lval, rval, ctx)
        }

        TODO()
    }

    fun or(lhs: ReferenceValue, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val lobj = ctx.heapArea.dereference(lhs)

        if (lobj is BoxedStackValueObject) {
            val lval = lobj.value
            val rval = rhs

            return PrimaryOperationUtils.or(lval, rval, ctx)
        }

        TODO()
    }
}