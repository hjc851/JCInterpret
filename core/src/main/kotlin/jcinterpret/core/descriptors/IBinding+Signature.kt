package jcinterpret.core.descriptors

import jcinterpret.signature.*
import org.eclipse.jdt.core.dom.IMethodBinding
import org.eclipse.jdt.core.dom.ITypeBinding

fun ITypeBinding.signature(): TypeSignature {
    when {
        this.isPrimitive -> {
            return when (this.name) {
                "byte" -> PrimitiveTypeSignature.BYTE
                "char" -> PrimitiveTypeSignature.CHAR
                "double" -> PrimitiveTypeSignature.DOUBLE
                "float" -> PrimitiveTypeSignature.FLOAT
                "int" -> PrimitiveTypeSignature.INT
                "long" -> PrimitiveTypeSignature.LONG
                "short" -> PrimitiveTypeSignature.SHORT
                "void" -> PrimitiveTypeSignature.VOID
                "boolean" -> PrimitiveTypeSignature.BOOLEAN

                else -> throw IllegalArgumentException("Unknown primitive type $this")
            }
        }

        this.isClass || this.isInterface -> {
            val sig = "L${this.binaryName.replace(".", "/")};"
            return SignatureParser(sig).parseClassTypeSignature()
        }

        this.isArray -> {
            return ArrayTypeSignature(this.componentType.signature())
        }

        else -> throw IllegalArgumentException("Unknown type binding classification")
    }
}

fun IMethodBinding.qualifiedSignature(): QualifiedMethodSignature {
    val decClass = this.declaringClass.signature() as ClassTypeSignature
    val sig = this.signature()

    return QualifiedMethodSignature(decClass, sig)
}

fun IMethodBinding.signature(): MethodSignature {
    val name = if (this.isConstructor) "<init>" else this.name
    val params = this.parameterTypes.map { it.signature() }.toTypedArray()
    val retn = this.returnType.signature()

    return MethodSignature(
        name,
        MethodTypeSignature(params, retn)
    )
}