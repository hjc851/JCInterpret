package jcinterpret.core.ctx.frame

import jcinterpret.core.control.ClassAreaFault
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.signature.*

abstract class Instruction {
    fun validateSignatures(ctx: ExecutionContext, vararg signatures: Signature) {
        val unloaded = mutableSetOf<ClassTypeSignature>()

        fun validate(sig: Signature) {
            when (sig) {
                is ClassTypeSignature -> if (!ctx.classArea.isClassLoaded(sig))
                    unloaded.add(sig)

                is ArrayTypeSignature -> validate(sig.componentType)

                is QualifiedMethodSignature -> {
                    validate(sig.declaringClassSignature)
                    validate(sig.methodSignature)
                }

                is MethodSignature -> validate(sig.typeSignature)

                is MethodTypeSignature -> {
                    validate(sig.returnType)
                    for (arg in sig.argumentTypes)
                        validate(arg)
                }
            }
        }

        for (sig in signatures)
            validate(sig)

        if (unloaded.isNotEmpty())
            throw ClassAreaFault(unloaded)
    }
}