package jcinterpret.core.memory.heap

import jcinterpret.core.memory.stack.StackValue
import jcinterpret.signature.TypeSignature
import java.io.Serializable

data class Field (
    val name: String,
    val type: TypeSignature?,
    var value: StackValue
): Serializable