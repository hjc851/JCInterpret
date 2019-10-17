package jcinterpret.signature

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.Serializable

@JsonSubTypes (
    JsonSubTypes.Type(QualifiedMethodSignature::class),
    JsonSubTypes.Type(MethodSignature::class),
    JsonSubTypes.Type(MethodTypeSignature::class),
    JsonSubTypes.Type(TypeSignature::class)
)
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
abstract class Signature : Serializable

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
data class QualifiedMethodSignature(val declaringClassSignature: ClassTypeSignature, val methodSignature: MethodSignature) : Signature() {
    override fun toString(): String {
        return "$declaringClassSignature$methodSignature"
    }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
data class MethodSignature(val name: String, val typeSignature: MethodTypeSignature) : Signature() {
    override fun toString(): String {
        return "$name$typeSignature"
    }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
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

@JsonSubTypes (
    JsonSubTypes.Type(ClassTypeSignature::class),
    JsonSubTypes.Type(ArrayTypeSignature::class),
    JsonSubTypes.Type(PrimitiveTypeSignature::class)
)
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
abstract class TypeSignature : Signature()

@JsonSubTypes (
    JsonSubTypes.Type(ClassTypeSignature::class),
    JsonSubTypes.Type(ArrayTypeSignature::class)
)
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
abstract class ReferenceTypeSignature : TypeSignature()

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
data class ClassTypeSignature(val className: String) : ReferenceTypeSignature() {
    companion object {
        val OBJECT = ClassTypeSignature("java/lang/Object")
    }

    init {
        if ((className.startsWith("L") && className.endsWith(";")) || className.contains("["))
            throw IllegalArgumentException("Invalid class name format")
    }

    override fun toString(): String {
        return "L$className;"
    }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
data class ArrayTypeSignature(val componentType: TypeSignature) : ReferenceTypeSignature() {
    override fun toString(): String {
        return "[$componentType"
    }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
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