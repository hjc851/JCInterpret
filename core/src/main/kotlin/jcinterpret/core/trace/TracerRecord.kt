package jcinterpret.core.trace

import jcinterpret.core.memory.stack.StackReference
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.signature.QualifiedMethodSignature

abstract class TracerRecord {

    data class EntryMethod(val sig: QualifiedMethodSignature): TracerRecord()
    data class EntryScope(val ref: StackReference): TracerRecord()
    data class EntryParameter(val ref: StackValue): TracerRecord()

    class InstanceLibraryMethodCall(val method: QualifiedMethodSignature, val scope: StackReference, val params: Array<StackValue>, val result: StackValue?) : TracerRecord()
    class StaticLibraryMethodCall(val method: QualifiedMethodSignature, val params: Array<StackValue>, val result: StackValue?) : TracerRecord()

}