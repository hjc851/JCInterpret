package jcinterpret.core.memory.heap

import jcinterpret.core.memory.stack.StackValue
import jcinterpret.core.memory.stack.SymbolicValue
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.TypeSignature

abstract class BoxedObject<T> (
    id: Int,
    type: ClassTypeSignature,
    value: T
): ObjectType(id, type, mutableMapOf())

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
data class SymbolicStringValue(val value: SymbolicValue): StringValue()

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