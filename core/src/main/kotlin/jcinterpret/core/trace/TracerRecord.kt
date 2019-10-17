package jcinterpret.core.trace

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jcinterpret.core.memory.heap.StackValueStringValue
import jcinterpret.core.memory.heap.StringValue
import jcinterpret.core.memory.stack.*
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.QualifiedMethodSignature
import jcinterpret.signature.TypeSignature
import java.io.Serializable
import javax.xml.bind.annotation.XmlSeeAlso
import javax.xml.bind.annotation.XmlType

@JsonSubTypes(
    JsonSubTypes.Type(TracerRecord.EntryMethod::class),
    JsonSubTypes.Type(TracerRecord.EntryScope::class),
    JsonSubTypes.Type(TracerRecord.EntryParameter::class),
    JsonSubTypes.Type(TracerRecord.StaticLibraryMethodCall::class),
    JsonSubTypes.Type(TracerRecord.InstanceLibraryMethodCall::class),
    JsonSubTypes.Type(TracerRecord.StaticFieldPut::class),
    JsonSubTypes.Type(TracerRecord.ObjectFieldPut::class),
    JsonSubTypes.Type(TracerRecord.ArrayMemberPut::class),
    JsonSubTypes.Type(TracerRecord.DefaultInstanceFieldValue::class),
    JsonSubTypes.Type(TracerRecord.DefaultStaticFieldValue::class),
    JsonSubTypes.Type(TracerRecord.SynthesisedReturnValue::class),
    JsonSubTypes.Type(TracerRecord.StackTransformation::class),
    JsonSubTypes.Type(TracerRecord.NotValueTransformation::class),
    JsonSubTypes.Type(TracerRecord.StackCast::class),
    JsonSubTypes.Type(TracerRecord.StringConcat::class),
    JsonSubTypes.Type(TracerRecord.Stringification::class),
    JsonSubTypes.Type(TracerRecord.Assertion::class),
    JsonSubTypes.Type(TracerRecord.Halt::class),
    JsonSubTypes.Type(TracerRecord.UncaughtException::class)
)
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
abstract class TracerRecord: Serializable {
    data class EntryMethod(val sig: QualifiedMethodSignature): TracerRecord()
    data class EntryScope(val ref: StackReference): TracerRecord()
    data class EntryParameter(val ref: StackValue): TracerRecord()

    data class StaticLibraryMethodCall(val method: QualifiedMethodSignature, val params: Array<StackValue>, val result: StackValue?) : TracerRecord()
    data class InstanceLibraryMethodCall(val method: QualifiedMethodSignature, val scope: ReferenceValue, val params: Array<StackValue>, val result: StackValue?) : TracerRecord()

    data class StaticFieldPut(val staticType: ClassTypeSignature, val field: String, val type: TypeSignature, val oldValue: StackValue, val newValue: StackValue): TracerRecord()
    data class ObjectFieldPut(val ref: ReferenceValue, val field: String, val type: TypeSignature, val oldValue: StackValue, val newValue: StackValue) : TracerRecord()
    data class ArrayMemberPut(val ref: ReferenceValue, val index: StackValue, val oldValue: StackValue, val newValue: StackValue) : TracerRecord()

    data class DefaultInstanceFieldValue(val ref: ReferenceValue, val field: String, val value: StackValue): TracerRecord()
    data class DefaultStaticFieldValue(val type: ClassTypeSignature, val field: String, val value: StackValue): TracerRecord()
    data class SynthesisedReturnValue(val method: QualifiedMethodSignature, val result: StackValue): TracerRecord()

    data class StackTransformation(val lhs: StackValue, val rhs: StackValue, val result: StackValue, val operator: BinaryOperator) : TracerRecord()
    data class NotValueTransformation(val input: StackValue, val output: StackValue) : TracerRecord()
    data class StackCast(val lhs: StackValue, val rhs: StackValue): TracerRecord()

    data class StringConcat(val lhs: StringValue, val rhs: StringValue, val result: StringValue): TracerRecord()
    data class Stringification(val value: StackValue, val result: StringValue): TracerRecord()

    data class Assertion(val condition: StackValue, val truth: Boolean): TracerRecord()

    data class Halt(val msg: String): TracerRecord()
    data class UncaughtException(val type: ClassTypeSignature): TracerRecord()
}