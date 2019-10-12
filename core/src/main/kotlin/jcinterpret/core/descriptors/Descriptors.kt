package jcinterpret.core.descriptors

import jcinterpret.core.memory.stack.*
import jcinterpret.signature.*

//
//  Descriptors
//

interface Descriptor

interface TypeDescriptor: Descriptor {
    val signature: TypeSignature
    val defaultValue: StackValue
    val stackType: StackType
}

abstract class ReferenceTypeDescriptor: TypeDescriptor {
    abstract override val signature: ReferenceTypeSignature

    override val defaultValue: StackValue
        get() = StackNil

    override val stackType: StackType
        get() = StackType.REFERENCE

    override fun toString(): String {
        return signature.toString()
    }
}

abstract class ClassTypeDescriptor: ReferenceTypeDescriptor() {
    abstract override val signature: ClassTypeSignature

    abstract val superclass: ClassTypeSignature?
    abstract val interfaces: List<ClassTypeSignature>

    abstract val outerclass: ClassTypeSignature?
    abstract val innerclasses: List<ClassTypeSignature>
    abstract val enclosingMethod: QualifiedMethodSignature?

    abstract val fields: Map<String, FieldDescriptor>
    abstract val methods: Map<String, MethodDescriptor>
}

class ArrayTypeDescriptor (
    override val signature: ArrayTypeSignature,
    val componentType: TypeDescriptor
): ReferenceTypeDescriptor()

enum class PrimitiveTypeDescriptor (
    override val signature: PrimitiveTypeSignature,
    override val defaultValue: StackValue,
    override val stackType: StackType
): TypeDescriptor {
    BOOLEAN(PrimitiveTypeSignature.BOOLEAN, StackBoolean(false), StackType.BOOLEAN),
    BYTE(PrimitiveTypeSignature.BYTE, StackByte(0), StackType.BYTE),
    CHAR(PrimitiveTypeSignature.CHAR, StackChar('\u0000'), StackType.CHAR),
    SHORT(PrimitiveTypeSignature.SHORT, StackShort(0), StackType.SHORT),
    INT(PrimitiveTypeSignature.INT, StackInt(0), StackType.INT),
    LONG(PrimitiveTypeSignature.LONG, StackLong(0), StackType.LONG),
    FLOAT(PrimitiveTypeSignature.FLOAT, StackFloat(0.0f), StackType.FLOAT),
    DOUBLE(PrimitiveTypeSignature.DOUBLE, StackDouble(0.0), StackType.DOUBLE),
    VOID (PrimitiveTypeSignature.VOID, StackNil, StackType.VOID)
}

abstract class MethodDescriptor: Descriptor {
    abstract val qualifiedSignature: QualifiedMethodSignature
    val signature: MethodSignature
        get() = qualifiedSignature.methodSignature

    val parameters: List<TypeSignature>
        get() = qualifiedSignature.methodSignature.typeSignature.argumentTypes.toList()

    abstract val exceptions: List<ClassTypeSignature>

    abstract val isStatic: Boolean
    abstract val isAbstract: Boolean
    abstract val isVararg: Boolean

    override fun toString(): String {
        return qualifiedSignature.toString()
    }
}

abstract class FieldDescriptor: Descriptor {
    abstract val name: String
    abstract val type: TypeSignature

    abstract val isStatic: Boolean

    override fun toString(): String {
        return "$name: $type"
    }
}