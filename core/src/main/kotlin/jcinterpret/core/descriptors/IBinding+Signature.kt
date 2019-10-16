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

        this.isAnonymous -> {
            return SignatureParser(key).parseClassTypeSignature()
        }

        this.isCapture -> {
            TODO()
        }

        this.isGenericType -> {
            val qname = this.qualifiedName
            val sig = "L${qname.replace(".", "/")};"
            return SignatureParser(sig).parseClassTypeSignature()
        }

        this.isIntersectionType -> {
            TODO()
        }

        this.isParameterizedType -> {
            val erasure = this.erasure
            val sig = erasure.signature()
            return sig
        }

        this.isUpperbound -> {
            TODO()
        }

        this.isWildcardType -> {
            TODO()
        }

        this.isTypeVariable -> {
            return this.erasure.signature()
        }

        this.isClass || this.isInterface || this.isEnum -> {

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
    if (this.methodDeclaration != this) {
        val declaration = this.methodDeclaration
        val dsig = declaration.signature()
        return dsig
    }

    val name = if (this.isConstructor) "<init>" else this.name
    val params = this.parameterTypes.map { it.signature() }.toTypedArray()
    val retn = this.returnType.signature()

    return MethodSignature(
        name,
        MethodTypeSignature(params, retn)
    )
}