package jcinterpret.core.memory.stack

//
//  Symbolic Value
//

data class SymbolicValue(val symbol: Int, override val type: StackType): StackValue()