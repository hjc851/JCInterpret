package jcinterpret.core.memory.stack

//
//  Reference Values
//

abstract class ReferenceValue(): StackValue() {
    abstract val id: Int

    override val type: StackType
        get() = StackType.REFERENCE
}

data class StackReference(override val id: Int): ReferenceValue()

object StackNil: ReferenceValue() {
    override val id: Int
        get() = 0
}