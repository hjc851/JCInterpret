package jcinterpret.core.trace

import jcinterpret.core.memory.stack.StackReference
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.core.signature.QualifiedMethodSignature

abstract class TracerRecord {

    data class EntryMethod(val sig: QualifiedMethodSignature): TracerRecord()
    data class EntryScope(val ref: StackReference): TracerRecord()
    data class EntryParameter(val ref: StackValue): TracerRecord()

}