package jcinterpret.testconsole.pipeline.comparison

import jcinterpret.core.memory.stack.BinaryOperationValue
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.core.trace.TraceRecord

object AssertionEquivalenceChecker {
    fun check(lhs: TraceRecord.Assertion, rhs: TraceRecord.Assertion, pairings: Pair<Map<String, String>, Map<String, String>>): Boolean {
        return false
        val swapOperator = lhs.truth != rhs.truth
        return check(lhs.condition, rhs.condition, swapOperator, pairings)
    }

    fun check(lhs: StackValue, rhs: StackValue, swapOperator: Boolean, pairings: Pair<Map<String, String>, Map<String, String>>): Boolean {

        if (lhs is BinaryOperationValue && rhs is BinaryOperationValue) {



            TODO()
        }



        TODO()
    }
}