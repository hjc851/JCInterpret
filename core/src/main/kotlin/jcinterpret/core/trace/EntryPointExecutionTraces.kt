package jcinterpret.core.trace

import jcinterpret.signature.QualifiedMethodSignature
import java.io.Serializable
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement
data class EntryPointExecutionTraces (
    val entryPoint: QualifiedMethodSignature,
    val executionTraces: Array<ExecutionTrace>
): Serializable