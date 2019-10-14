package jcinterpret.core.trace

import jcinterpret.core.memory.stack.BinaryOperator
import jcinterpret.core.memory.stack.StackBoolean
import jcinterpret.core.memory.stack.StackReference
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.signature.QualifiedMethodSignature
import jcinterpret.signature.TypeSignature

abstract class TracerRecord {

    data class EntryMethod(val sig: QualifiedMethodSignature): TracerRecord()
    data class EntryScope(val ref: StackReference): TracerRecord()
    data class EntryParameter(val ref: StackValue): TracerRecord()

    data class InstanceLibraryMethodCall(val method: QualifiedMethodSignature, val scope: StackReference, val params: Array<StackValue>, val result: StackValue?) : TracerRecord()
    data class StaticLibraryMethodCall(val method: QualifiedMethodSignature, val params: Array<StackValue>, val result: StackValue?) : TracerRecord()

    data class ObjectFieldPut(val ref: StackReference, val field: String, val type: TypeSignature, val oldValue: StackValue, val newValue: StackValue) : TracerRecord()
    data class ArrayMemberPut(val ref: StackReference, val index: StackValue, val oldValue: StackValue, val newValue: StackValue) : TracerRecord()

    data class StackTransformation(val lhs: StackValue, val rhs: StackValue, val result: StackValue, val operator: BinaryOperator) : TracerRecord()
}