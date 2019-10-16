package jcinterpret.core.memory.heap

import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.core.memory.stack.SymbolicValue
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.TypeSignature
import sun.reflect.generics.reflectiveObjects.NotImplementedException

abstract class BoxedObject<T> (
    id: Int,
    type: ClassTypeSignature,
    val value: T
): ObjectType(id, type, mutableMapOf()) {

    override fun getField(name: String, type: TypeSignature, ctx: ExecutionContext): Field {
        throw IllegalStateException("Boxed values do not have fields")
    }
}

//
//  Stack Values
//

class BoxedStackValueObject (
    id: Int,
    type: ClassTypeSignature,
    value: StackValue
): BoxedObject<StackValue>(id, type, value) {
    override val lookupType: ClassTypeSignature
        get() = type as ClassTypeSignature
}

//
//  String Values
//

abstract class StringValue
data class ConcreteStringValue(val value: String): StringValue()
data class SymbolicStringValue(val value: Int): StringValue()
data class StackValueStringValue(val value: StackValue): StringValue()
data class CompositeStringValue(val lhs: StringValue, val rhs: StringValue): StringValue()

class BoxedStringObject (
    id: Int,
    type: ClassTypeSignature,
    value: StringValue
): BoxedObject<StringValue>(id, type, value) {
    override val lookupType: ClassTypeSignature
        get() = type as ClassTypeSignature
}

//
//  Class Value
//

class ClassObject (
    id: Int,
    type: ClassTypeSignature,
    value: TypeSignature
): BoxedObject<TypeSignature>(id, type, value) {
    override val lookupType: ClassTypeSignature
        get() = type as ClassTypeSignature
}