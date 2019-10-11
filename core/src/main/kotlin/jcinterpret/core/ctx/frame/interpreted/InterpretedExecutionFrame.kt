package jcinterpret.core.ctx.frame.interpreted

import jcinterpret.core.ctx.frame.MethodBoundExecutionFrame
import jcinterpret.core.signature.QualifiedMethodSignature

class InterpretedExecutionFrame (
    override val method: QualifiedMethodSignature
): MethodBoundExecutionFrame() {

}