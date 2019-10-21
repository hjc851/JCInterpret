package jcinterpret.comparison.iterative

import jcinterpret.core.memory.heap.StringValue
import jcinterpret.core.memory.stack.Operator
import jcinterpret.core.memory.stack.StackNil
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.graph.*
import jcinterpret.graph.execution.NodeAttributeKeys.METHODSIGNATURE
import jcinterpret.graph.execution.NodeAttributeKeys.OPERATOR
import jcinterpret.graph.execution.NodeAttributeKeys.STRING
import jcinterpret.graph.execution.NodeAttributeKeys.TYPE
import jcinterpret.graph.execution.NodeAttributeKeys.VALUE
import jcinterpret.signature.*
import org.graphstream.graph.Edge
import org.graphstream.graph.Node

object ElementMatcher {
    fun match(lhs: Edge, rhs: Edge): Int {
        if (lhs.isAggregation() && rhs.isAggregation())
            return 1

        if (lhs.isTransformation() && rhs.isTransformation())
            return 1

        if (lhs.isSupplies() && rhs.isSupplies())
            return 1

        if (lhs.isScope() && rhs.isScope())
            return 1

        if (lhs.isParameter() && rhs.isParameter())
            return 1

        if (lhs.isParameter() && rhs.isScope() || lhs.isScope() && rhs.isParameter())
            return 1

        return 0
    }

    fun match(lhs: Node, rhs: Node): Int {
        if (lhs.isEntryPoint() && rhs.isEntryPoint()) {
            return 1
        }

        if (lhs.isOperator() && rhs.isOperator()) {
            val lop = lhs.getAttribute<Operator>(OPERATOR)
            val rop = rhs.getAttribute<Operator>(OPERATOR)

            return if (lop == rop) 1 else 0
        }

        if (lhs.isMethodCall() && rhs.isMethodCall()) {
            val lsig = lhs.getAttribute<QualifiedMethodSignature>(METHODSIGNATURE)
            val rsig = rhs.getAttribute<QualifiedMethodSignature>(METHODSIGNATURE)

            if (lsig == rsig) return 1

            if (lsig.declaringClassSignature == rsig.declaringClassSignature &&
                    lsig.methodSignature.name == rsig.methodSignature.name) return 1

            if (lsig.methodSignature.name == rsig.methodSignature.name ||
                    lsig.methodSignature.name.startsWith(rsig.methodSignature.name) ||
                    rsig.methodSignature.name.startsWith(lsig.methodSignature.name)) return 1

            if (lsig.methodSignature.typeSignature.returnType == rsig.methodSignature.typeSignature.returnType) return 1
        }

        if (lhs.isData() && rhs.isData()) {

            if (lhs.isValue() && rhs.isValue()) {

                if (lhs.isString() && rhs.isString()) {
                    val lvalue = lhs.getAttribute<StringValue>(STRING)
                    val rvalue = rhs.getAttribute<StringValue>(STRING)

                    return if (ValueComparator.compare(lvalue, rvalue)) 1 else 0
                } else {
                    val lvalue = lhs.getAttribute<StackValue>(VALUE) ?: StackNil
                    val rvalue = rhs.getAttribute<StackValue>(VALUE) ?: StackNil

                    return if (ValueComparator.compare(lvalue, rvalue)) 1 else 0
                }
            }

            if (lhs.isObject() && rhs.isObject()) {

                val ltype = lhs.getAttribute<TypeSignature>(TYPE)
                val rtype = rhs.getAttribute<TypeSignature>(TYPE)

                return if (lhs.isConcrete() && rhs.isConcrete()) {
                    if (compareTypes(ltype, rtype) || compareComposition(lhs, rhs)) 1 else 0
                } else if (lhs.isSymbolic() && rhs.isSymbolic()) {
                    if (compareTypes(ltype, rtype) || compareComposition(lhs, rhs)) 1 else 0
                } else {
                    0
                }
            }
        }

        return 0
    }

    fun compareComposition(lhs: Node, rhs: Node): Boolean {

        val lownerships = lhs.getEnteringEdgeSet<Edge>()
            .filter { it.isAggregation() }
            .map { it.getOpposite<Node>(lhs) }

        val rownerships = rhs.getEnteringEdgeSet<Edge>()
            .filter { it.isAggregation() }
            .map { it.getOpposite<Node>(rhs) }

        if (lownerships.isEmpty() && rownerships.isEmpty()) return false
        if (lownerships.size != rownerships.size) return false

        val ltypes = lownerships.map { it.getAttribute<TypeSignature>(TYPE) }.toSet()
        val rtypes = rownerships.map { it.getAttribute<TypeSignature>(TYPE) }.toSet()

        val overlap = ltypes.union(rtypes)

        return ltypes.size == overlap.size
    }

    fun compareTypes(ltype: TypeSignature, rtype: TypeSignature): Boolean {

        if (ltype == rtype) return true

        if (ltype is ArrayTypeSignature && rtype is ArrayTypeSignature) {
            return compareTypes(ltype.componentType, rtype.componentType)
        }

        if (ltype is PrimitiveTypeSignature && rtype is PrimitiveTypeSignature) {
            return true
        }

        if (ltype is ClassTypeSignature && rtype is ClassTypeSignature) {
            if (ltype.className == rtype.className) return true
        }

        return false
    }
}