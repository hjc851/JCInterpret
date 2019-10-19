package jcinterpret.core.descriptors

import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.QualifiedMethodSignature
import org.eclipse.jdt.core.dom.*
import java.lang.Exception

class BindingDescriptorResolver (
    compilationUnits: List<CompilationUnit>
): DescriptorResolver() {

    private val classes = mutableMapOf<ClassTypeSignature, TypeBindingClassTypeDescriptor>()

    init {
        val visitor = Visitor()
        for (cu in compilationUnits) {
            cu.accept(visitor)
        }
    }

    private inner class Visitor : ASTVisitor() {
        override fun visit(type: TypeDeclaration): Boolean {
            val desc = TypeBindingClassTypeDescriptor(type.resolveBinding())
            classes[desc.signature] = desc
            return true
        }

        override fun visit(type: EnumDeclaration): Boolean {
            val desc = TypeBindingClassTypeDescriptor(type.resolveBinding())
            classes[desc.signature] = desc
            return true
        }

        override fun visit(type: AnnotationTypeDeclaration): Boolean {
            val desc = TypeBindingClassTypeDescriptor(type.resolveBinding())
            classes[desc.signature] = desc
            return true
        }
    }

    override fun tryResolveDescriptor(sig: ClassTypeSignature): ClassTypeDescriptor? {
        return classes[sig]
    }

    override fun tryResolveDescriptor(sig: QualifiedMethodSignature): MethodDescriptor? {
        val cls = tryResolveDescriptor(sig.declaringClassSignature) ?: return null
        return cls.methods[sig.methodSignature.toString()]
    }
}