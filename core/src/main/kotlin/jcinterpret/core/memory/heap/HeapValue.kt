package jcinterpret.core.memory.heap

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jcinterpret.core.memory.stack.StackReference
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.ReferenceTypeSignature
import java.io.Serializable
import javax.xml.bind.annotation.XmlSeeAlso

@JsonSubTypes (
    JsonSubTypes.Type(ObjectType::class)
)
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
abstract class HeapValue: Serializable {
    abstract val id: Int
    abstract val type: ReferenceTypeSignature

    abstract val lookupType: ClassTypeSignature

    fun ref() = StackReference(id)
}

