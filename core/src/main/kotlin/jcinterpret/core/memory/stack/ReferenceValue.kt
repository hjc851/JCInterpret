package jcinterpret.core.memory.stack

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

//
//  Reference Values
//

@JsonSubTypes (
    JsonSubTypes.Type(StackReference::class),
    JsonSubTypes.Type(StackNil::class)
)
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
abstract class ReferenceValue: StackValue() {
    abstract val id: Int

    override val type: StackType
        get() = StackType.REFERENCE
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
data class StackReference(override val id: Int): ReferenceValue() {
    override fun label(): String {
        return "@$id"
    }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
object StackNil: ReferenceValue() {
    override val id: Int
        get() = 0

    override fun label(): String {
        return "null"
    }
}