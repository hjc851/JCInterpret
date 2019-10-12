package jcinterpret.signature

import java.io.Serializable

abstract class Signature : Serializable

data class QualifiedMethodSignature(val declaringClassSignature: ClassTypeSignature, val methodSignature: MethodSignature) : Signature() {
    override fun toString(): String {
        return "$declaringClassSignature$methodSignature"
    }
}

data class MethodSignature(val name: String, val typeSignature: MethodTypeSignature) : Signature() {
    override fun toString(): String {
        return "$name$typeSignature"
    }
}

data class MethodTypeSignature(val argumentTypes: Array<TypeSignature>, val returnType: TypeSignature) : Signature() {
    override fun toString(): String {
        val builder = StringBuilder()

        builder.append('(')
        for (arg in argumentTypes)
            builder.append(arg.toString())
        builder.append(')')
        builder.append(returnType.toString())

        return builder.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MethodTypeSignature

        if (!argumentTypes.contentEquals(other.argumentTypes)) return false
        if (returnType != other.returnType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = argumentTypes.contentHashCode()
        result = 31 * result + returnType.hashCode()
        return result
    }
}

abstract class TypeSignature : Signature()

abstract class ReferenceTypeSignature : TypeSignature()

data class ClassTypeSignature(val className: String) : ReferenceTypeSignature() {
    companion object {
        val OBJECT = ClassTypeSignature("java/lang/Object")
    }

    init {
        if (className.startsWith("L") || className.endsWith(";") || className.contains("["))
            throw IllegalArgumentException("Invalid class name format")
    }

    override fun toString(): String {
        return "L$className;"
    }
}

data class ArrayTypeSignature(val componentType: TypeSignature) : ReferenceTypeSignature() {
    override fun toString(): String {
        return "[$componentType"
    }
}

data class PrimitiveTypeSignature internal constructor(
    val code: Char
) : TypeSignature() {

    companion object {
        val BOOLEAN = PrimitiveTypeSignature('Z')
        val BYTE = PrimitiveTypeSignature('B')
        val CHAR = PrimitiveTypeSignature('C')
        val SHORT = PrimitiveTypeSignature('S')
        val INT = PrimitiveTypeSignature('I')
        val LONG = PrimitiveTypeSignature('J')
        val FLOAT = PrimitiveTypeSignature('F')
        val DOUBLE = PrimitiveTypeSignature('D')
        val VOID = PrimitiveTypeSignature('V')
    }

    override fun toString(): String {
        return "$code"
    }
}

fun TypeSignature.boxToArray(dimensions: Int): ArrayTypeSignature {
    if (dimensions <= 0)
        throw IllegalArgumentException("Array dimensions must be greater than 0")

    var sig: TypeSignature = this
    for (i in 0 until dimensions)
        sig = ArrayTypeSignature(sig)

    return sig as ArrayTypeSignature
}