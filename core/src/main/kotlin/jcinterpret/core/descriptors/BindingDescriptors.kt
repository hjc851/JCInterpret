package jcinterpret.core.descriptors

import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.QualifiedMethodSignature
import jcinterpret.signature.TypeSignature
import org.eclipse.jdt.core.dom.IMethodBinding
import org.eclipse.jdt.core.dom.ITypeBinding
import org.eclipse.jdt.core.dom.IVariableBinding
import org.eclipse.jdt.core.dom.Modifier

class TypeBindingClassTypeDescriptor (
    val binding: ITypeBinding
): ClassTypeDescriptor() {
    override val signature: ClassTypeSignature

    override val superclass: ClassTypeSignature?
    override val interfaces: List<ClassTypeSignature>

    override val outerclass: ClassTypeSignature?
    override val innerclasses: List<ClassTypeSignature>
    override val enclosingMethod: QualifiedMethodSignature?

    override val fields: Map<String, FieldDescriptor>
    override val methods: Map<String, MethodDescriptor>

    init {
        if (!binding.isClass) throw IllegalArgumentException("${binding.qualifiedName} is not a class type")

        this.signature = binding.signature() as ClassTypeSignature

        this.superclass = binding.superclass?.signature() as? ClassTypeSignature
        this.interfaces = binding.interfaces.map { it.signature() as ClassTypeSignature }

        this.outerclass = binding.declaringClass?.signature() as? ClassTypeSignature
        this.innerclasses = binding.declaredTypes.map { it.signature() as ClassTypeSignature }
        this.enclosingMethod = binding.declaringMethod?.qualifiedSignature()

        this.fields = binding.declaredFields.map { it.name to VariableBindingFieldDescriptor(it) }.toMap()
        this.methods = binding.declaredMethods.map { it.signature().toString() to MethodBindingMethodDescriptor(it) }.toMap()
    }
}

class MethodBindingMethodDescriptor (
    val binding: IMethodBinding
): MethodDescriptor() {
    override val qualifiedSignature: QualifiedMethodSignature

    override val exceptions: List<ClassTypeSignature>

    override val isStatic: Boolean
    override val isAbstract: Boolean
    override val isVararg: Boolean

    init {
        this.qualifiedSignature = binding.qualifiedSignature()

        this.exceptions = binding.exceptionTypes.map { it.signature() as ClassTypeSignature }

        this.isStatic = Modifier.isStatic(binding.modifiers)
        this.isAbstract = Modifier.isAbstract(binding.modifiers)
        this.isVararg = binding.isVarargs
    }
}

class VariableBindingFieldDescriptor (
    val binding: IVariableBinding
): FieldDescriptor() {
    override val name: String
    override val type: TypeSignature

    override val isStatic: Boolean

    init {
        this.name = binding.name
        this.type = binding.type.signature()

        this.isStatic = Modifier.isStatic(binding.modifiers)
    }
}