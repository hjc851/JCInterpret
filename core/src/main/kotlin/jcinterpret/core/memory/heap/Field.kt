package jcinterpret.core.memory.heap

import jcinterpret.core.memory.stack.StackValue
import jcinterpret.core.signature.TypeSignature

data class Field (
    val name: String,
    val type: TypeSignature?,
    var value: StackValue
)