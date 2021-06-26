package jcinterpret.core.bytecode

import jcinterpret.core.descriptors.ClassFileClassTypeDescriptor
import jcinterpret.core.descriptors.ClassFileFieldDescriptor
import jcinterpret.core.descriptors.ClassFileMethodDescriptor
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.QualifiedMethodSignature
import jcinterpret.signature.TypeSignature

class BytecodeLibrary(
    private val typeDeclarations: Map<TypeSignature, ClassFileClassTypeDescriptor>,
    private val methodDeclarations: Map<QualifiedMethodSignature, ClassFileMethodDescriptor>,
    private val fieldDeclarations: Map<String, ClassFileFieldDescriptor>
) {
    fun getDeclaration(type: ClassTypeSignature): ClassFileClassTypeDescriptor? {
        return typeDeclarations[type]
    }

    fun getDeclaration(method: QualifiedMethodSignature): ClassFileMethodDescriptor? {
        return methodDeclarations[method]
    }

    fun getDeclaration(type: ClassTypeSignature, name: String): ClassFileFieldDescriptor? {
        val key = "$type.$name"
        return fieldDeclarations[key]
    }
}