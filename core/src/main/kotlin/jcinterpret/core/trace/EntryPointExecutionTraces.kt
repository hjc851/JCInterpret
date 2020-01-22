package jcinterpret.core.trace

import jcinterpret.signature.QualifiedMethodSignature
import java.io.Serializable

data class EntryPointExecutionTraces (
    val entryPoint: QualifiedMethodSignature,
    val executionTraces: Array<ExecutionTrace>
): Serializable