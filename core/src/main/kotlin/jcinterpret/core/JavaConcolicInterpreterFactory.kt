package jcinterpret.core

import jcinterpret.core.interpreter.SourceLibrary
import jcinterpret.core.signature.QualifiedMethodSignature

object JavaConcolicInterpreterFactory {
    fun build(
        entryPoint: QualifiedMethodSignature,
        library: SourceLibrary
    ): JavaConcolicInterpreter {
        TODO()
    }
}