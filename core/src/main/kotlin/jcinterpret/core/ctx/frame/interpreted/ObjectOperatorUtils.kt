package jcinterpret.core.ctx.frame.interpreted

import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.memory.heap.*
import jcinterpret.core.memory.stack.*
import jcinterpret.core.trace.TraceRecord

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
                    ctx.records.add(TraceRecord.Stringification(robj.value, this))
                }
            } else {
                StackValueStringValue(robj.ref()).apply {
                    ctx.records.add(TraceRecord.Stringification(robj.ref(), this))
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

            ctx.records.add(TraceRecord.StringConcat(lstr, rstr, value))
//            ctx.records.add(TraceRecord.StackTransformation(lhs, rhs, ref, BinaryOperator.CONCAT))

            return ref

        } else if (lobj is BoxedStackValueObject && robj is BoxedStackValueObject) {
            val lvalue = lobj.value
            val rvalue = robj.value
            return PrimaryOperationUtils.add(lvalue, rvalue, ctx)
        } else if (lobj !is BoxedObject<*> && robj is BoxedStringObject) {
            val lstr = ctx.heapArea.allocateSymbolicStringValue()
            ctx.records.add(TraceRecord.Stringification(lhs, lstr))

            val rstr = robj.value
            val value = CompositeStringValue(lstr, rstr)

            ctx.records.add(TraceRecord.StringConcat(lstr, rstr, value))

            val str = ctx.heapArea.allocateSymbolicString(ctx, value)
            val ref = str.ref()
            return ref

        } else {
            TODO()
        }
    }

    fun add(lhs: StackReference, rhs: StackValue, ctx: ExecutionContext): StackValue {
        val lobj = ctx.heapArea.dereference(lhs)

        if (lobj is BoxedStringObject) {

            val lstr = lobj.value
            val rstr = StackValueStringValue(rhs)
            ctx.records.add(TraceRecord.Stringification(rhs, rstr))

            val value = if (lstr is ConcreteStringValue && rhs is ConcreteValue<*>)
                ConcreteStringValue(lstr.value + rhs.value)
            else
                CompositeStringValue(lstr, rstr)

            val str = ctx.heapArea.allocateSymbolicString(ctx, value)
            val ref = str.ref()

            ctx.records.add(TraceRecord.StringConcat(lstr, rstr, value))
//            ctx.records.add(TraceRecord.StackTransformation(lhs, rhs, ref, BinaryOperator.CONCAT))

            return ref

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
            val lstr = if (lhs is ConcreteValue<*>) ConcreteStringValue(lhs.value.toString())
            else StackValueStringValue(lhs)
            ctx.records.add(TraceRecord.Stringification(lhs, lstr))

            val rstr = robj.value

            val value = if (lhs is ConcreteValue<*> && rstr is ConcreteStringValue) ConcreteStringValue(lhs.value.toString() + rstr.value)
            else CompositeStringValue(lstr, rstr)

            val str = ctx.heapArea.allocateSymbolicString(ctx, value)
            val ref = str.ref()

            ctx.records.add(TraceRecord.StringConcat(lstr, rstr, value))
//            ctx.records.add(TraceRecord.StackTransformation(lhs, rhs, ref, BinaryOperator.CONCAT))

            return ref

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