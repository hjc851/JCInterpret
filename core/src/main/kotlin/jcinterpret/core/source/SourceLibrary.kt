package jcinterpret.core.source

import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.QualifiedMethodSignature
import jcinterpret.signature.TypeSignature
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration
import org.eclipse.jdt.core.dom.MethodDeclaration
import org.eclipse.jdt.core.dom.VariableDeclarationFragment

class SourceLibrary (
    private val typeDeclarations: Map<TypeSignature, AbstractTypeDeclaration>,
    private val methodDeclarations: Map<QualifiedMethodSignature, MethodDeclaration>,
    private val fieldDeclarations: Map<String, VariableDeclarationFragment>
) {
    fun getDeclaration(type: ClassTypeSignature): AbstractTypeDeclaration? {
        return typeDeclarations[type]
    }

    fun getDeclaration(method: QualifiedMethodSignature): MethodDeclaration? {
        return methodDeclarations[method]
    }

    fun getDeclaration(type: ClassTypeSignature, name: String): VariableDeclarationFragment? {
        val key = "$type.$name"
        return fieldDeclarations[key]
    }
}