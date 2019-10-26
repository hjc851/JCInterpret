package jcinterpret.core.trace

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jcinterpret.core.memory.heap.StringValue
import jcinterpret.core.memory.stack.BinaryOperator
import jcinterpret.core.memory.stack.ReferenceValue
import jcinterpret.core.memory.stack.StackReference
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.QualifiedMethodSignature
import jcinterpret.signature.TypeSignature
import java.io.Serializable

@JsonSubTypes(
    JsonSubTypes.Type(TraceRecord.EntryMethod::class),
    JsonSubTypes.Type(TraceRecord.EntryScope::class),
    JsonSubTypes.Type(TraceRecord.EntryParameter::class),
    JsonSubTypes.Type(TraceRecord.StaticLibraryMethodCall::class),
    JsonSubTypes.Type(TraceRecord.InstanceLibraryMethodCall::class),
    JsonSubTypes.Type(TraceRecord.StaticFieldPut::class),
    JsonSubTypes.Type(TraceRecord.ObjectFieldPut::class),
    JsonSubTypes.Type(TraceRecord.ArrayMemberPut::class),
    JsonSubTypes.Type(TraceRecord.ArrayMemberGet::class),
    JsonSubTypes.Type(TraceRecord.DefaultInstanceFieldValue::class),
    JsonSubTypes.Type(TraceRecord.DefaultStaticFieldValue::class),
    JsonSubTypes.Type(TraceRecord.SynthesisedReturnValue::class),
    JsonSubTypes.Type(TraceRecord.StackTransformation::class),
    JsonSubTypes.Type(TraceRecord.NotValueTransformation::class),
    JsonSubTypes.Type(TraceRecord.StackCast::class),
    JsonSubTypes.Type(TraceRecord.StringConcat::class),
    JsonSubTypes.Type(TraceRecord.Stringification::class),
    JsonSubTypes.Type(TraceRecord.Assertion::class),
    JsonSubTypes.Type(TraceRecord.Halt::class),
    JsonSubTypes.Type(TraceRecord.UncaughtException::class)
)
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
abstract class TraceRecord: Serializable {

    //  Visitor method

    abstract fun <T> accept(visitor: Visitor<T>, arg: T)

    //
    //  Records
    //

    data class EntryMethod(val sig: QualifiedMethodSignature): TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }
    data class EntryScope(val ref: StackReference): TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }
    data class EntryParameter(val ref: StackValue): TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }

    data class StaticLibraryMethodCall(val method: QualifiedMethodSignature, val params: Array<StackValue>, val result: StackValue?) : TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }
    data class InstanceLibraryMethodCall(val method: QualifiedMethodSignature, val scope: ReferenceValue, val params: Array<StackValue>, val result: StackValue?) : TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }
    data class SynthesisedReturnValue(val method: QualifiedMethodSignature, val result: StackValue): TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }

    data class StaticFieldPut(val staticType: ClassTypeSignature, val field: String, val type: TypeSignature, val oldValue: StackValue, val newValue: StackValue): TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }
    data class ObjectFieldPut(val ref: ReferenceValue, val field: String, val type: TypeSignature, val oldValue: StackValue, val newValue: StackValue) : TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }
    data class ArrayMemberPut(val ref: ReferenceValue, val index: StackValue, val oldValue: StackValue, val newValue: StackValue) : TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }
    data class ArrayMemberGet(val ref: ReferenceValue, val index: StackValue, val value: StackValue) : TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }

    data class DefaultInstanceFieldValue(val ref: ReferenceValue, val field: String, val value: StackValue): TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }
    data class DefaultStaticFieldValue(val type: ClassTypeSignature, val field: String, val value: StackValue): TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }

    data class StackTransformation(val lhs: StackValue, val rhs: StackValue, val result: StackValue, val operator: BinaryOperator) : TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }
    data class NotValueTransformation(val input: StackValue, val output: StackValue) : TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }
    data class StackCast(val lhs: StackValue, val rhs: StackValue): TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }

    data class StringConcat(val lhs: StringValue, val rhs: StringValue, val result: StringValue): TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }
    data class Stringification(val value: StackValue, val result: StringValue): TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }

    data class Assertion(val condition: StackValue, val truth: Boolean): TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }

    data class Halt(val msg: String): TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }
    data class UncaughtException(val type: ClassTypeSignature): TraceRecord() { override fun <T> accept(visitor: Visitor<T>, arg: T) { visitor.visit(this, arg) } }

    //
    //  Visitor
    //

    abstract class Visitor<T> {
        abstract fun visit(record: EntryMethod, arg: T)
        abstract fun visit(record: EntryScope, arg: T)
        abstract fun visit(record: EntryParameter, arg: T)
        abstract fun visit(record: StaticLibraryMethodCall, arg: T)
        abstract fun visit(record: InstanceLibraryMethodCall, arg: T)
        abstract fun visit(record: SynthesisedReturnValue, arg: T)
        abstract fun visit(record: StaticFieldPut, arg: T)
        abstract fun visit(record: ObjectFieldPut, arg: T)
        abstract fun visit(record: ArrayMemberPut, arg: T)
        abstract fun visit(record: ArrayMemberGet, arg: T)
        abstract fun visit(record: DefaultInstanceFieldValue, arg: T)
        abstract fun visit(record: DefaultStaticFieldValue, arg: T)
        abstract fun visit(record: StackTransformation, arg: T)
        abstract fun visit(record: NotValueTransformation, arg: T)
        abstract fun visit(record: StackCast, arg: T)
        abstract fun visit(record: StringConcat, arg: T)
        abstract fun visit(record: Stringification, arg: T)
        abstract fun visit(record: Assertion, arg: T)
        abstract fun visit(record: Halt, arg: T)
        abstract fun visit(record: UncaughtException, arg: T)
    }
}