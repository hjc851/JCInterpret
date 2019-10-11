package jcinterpret.core.ctx

import jcinterpret.core.ctx.frame.synthetic.SyntheticExecutionFrame
import jcinterpret.core.signature.ClassTypeSignature

class ClassArea {
    fun isClassLoaded(sig: ClassTypeSignature): Boolean {
        TODO()
    }

    fun buildClassLoaderFrame(sigs: Set<ClassTypeSignature>): SyntheticExecutionFrame {
        TODO()
    }
}