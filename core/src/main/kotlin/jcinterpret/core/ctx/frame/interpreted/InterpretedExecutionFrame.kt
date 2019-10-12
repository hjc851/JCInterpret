package jcinterpret.core.ctx.frame.interpreted

import jcinterpret.core.ctx.frame.MethodBoundExecutionFrame
import jcinterpret.signature.QualifiedMethodSignature

abstract class InterpretedExecutionFrame (
    override val method: QualifiedMethodSignature
): MethodBoundExecutionFrame() {

}