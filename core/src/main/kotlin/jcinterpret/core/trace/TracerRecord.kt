package jcinterpret.core.trace

import jcinterpret.core.memory.heap.StackValueStringValue
import jcinterpret.core.memory.heap.StringValue
import jcinterpret.core.memory.stack.*
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.QualifiedMethodSignature
import jcinterpret.signature.TypeSignature

abstract class TracerRecord {
    data class EntryMethod(val sig: QualifiedMethodSignature): TracerRecord()
    data class EntryScope(val ref: StackReference): TracerRecord()
    data class EntryParameter(val ref: StackValue): TracerRecord()

    data class StaticLibraryMethodCall(val method: QualifiedMethodSignature, val params: Array<StackValue>, val result: StackValue?) : TracerRecord()
    data class InstanceLibraryMethodCall(val method: QualifiedMethodSignature, val scope: ReferenceValue, val params: Array<StackValue>, val result: StackValue?) : TracerRecord()

    data class StaticFieldPut(val staticType: ClassTypeSignature, val field: String, val type: TypeSignature, val oldValue: StackValue, val newValue: StackValue): TracerRecord()
    data class ObjectFieldPut(val ref: ReferenceValue, val field: String, val type: TypeSignature, val oldValue: StackValue, val newValue: StackValue) : TracerRecord()
    data class ArrayMemberPut(val ref: ReferenceValue, val index: StackValue, val oldValue: StackValue, val newValue: StackValue) : TracerRecord()

    data class StackTransformation(val lhs: StackValue, val rhs: StackValue, val result: StackValue, val operator: BinaryOperator) : TracerRecord()
    data class NotValueTransformation(val input: StackValue, val output: StackValue) : TracerRecord()
    data class StackCast(val lhs: StackValue, val rhs: StackValue): TracerRecord()

    data class StringConcat(val lhs: StringValue, val rhs: StringValue, val result: StringValue): TracerRecord()
    data class Stringification(val value: StackValue, val result: StringValue): TracerRecord()

    data class Assertion(val condition: StackValue, val truth: Boolean): TracerRecord()

    data class Halt(val msg: String): TracerRecord()
    data class UncaughtException(val type: ClassTypeSignature): TracerRecord()
}