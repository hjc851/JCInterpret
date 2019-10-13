package jcinterpret.core.source

import jcinterpret.core.descriptors.qualifiedSignature
import jcinterpret.core.descriptors.signature
import jcinterpret.signature.QualifiedMethodSignature
import jcinterpret.signature.TypeSignature
import org.eclipse.jdt.core.dom.*

object SourceLibraryFactory {
    fun build(compilationUnits: List<CompilationUnit>): SourceLibrary {
        val builder = Builder()
        val visitor = Visitor(builder)
        compilationUnits.forEach { it.accept(visitor) }
        return builder.build()
    }

    class Builder {
        private val typeDeclarations: MutableMap<TypeSignature, AbstractTypeDeclaration> = mutableMapOf()
        private val methodDeclarations: MutableMap<QualifiedMethodSignature, MethodDeclaration> = mutableMapOf()
        private val fieldDeclarations: MutableMap<String, VariableDeclarationFragment> = mutableMapOf()

        fun add(type: AbstractTypeDeclaration): Builder {
            val binding = type.resolveBinding()
            val sig = binding.signature()
            typeDeclarations[sig] = type
            return this
        }

        fun add(method: MethodDeclaration): Builder {
            val binding = method.resolveBinding()
            val sig = binding.qualifiedSignature()
            methodDeclarations[sig] = method
            return this
        }

        fun add(fielddec: VariableDeclarationFragment): Builder {
            val binding = fielddec.resolveBinding()
            val dclasssig = binding.declaringClass.signature()
            val name = binding.name
            val sig = "$dclasssig.$name"
            fieldDeclarations[sig] = fielddec
            return this
        }

        fun build(): SourceLibrary {
            val store = SourceLibrary (
                typeDeclarations.toMap(), methodDeclarations.toMap(), fieldDeclarations.toMutableMap()
            )

            return store
        }
    }

    class Visitor(val builder: Builder): ASTVisitor() {
        override fun visit(node: MethodDeclaration): Boolean {
            builder.add(node)
            return super.visit(node)
        }

        override fun visit(node: FieldDeclaration): Boolean {
            node.fragments().forEach {
                it as VariableDeclarationFragment

                builder.add(it)
            }

            return super.visit(node)
        }

        override fun visit(node: TypeDeclaration): Boolean {
            builder.add(node)
            return super.visit(node)
        }

        override fun visit(node: EnumDeclaration): Boolean {
            builder.add(node)
            return super.visit(node)
        }
    }
}