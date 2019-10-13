package jcinterpret.core.descriptors

import com.sun.tools.classfile.*
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.QualifiedMethodSignature
import jcinterpret.signature.SignatureParser
import jcinterpret.signature.TypeSignature

class ClassFileClassTypeDescriptor (
    val cf: ClassFile
): ClassTypeDescriptor() {
    override val signature: ClassTypeSignature

    override val superclass: ClassTypeSignature?
    override val interfaces: List<ClassTypeSignature>

    override val innerclasses: List<ClassTypeSignature>
    override val outerclass: ClassTypeSignature?
    override val enclosingMethod: QualifiedMethodSignature?

    override val fields: Map<String, FieldDescriptor>
    override val methods: Map<String, MethodDescriptor>

    init {
        this.signature = ClassTypeSignature(cf.name)

        this.superclass = if (cf.super_class > 0) ClassTypeSignature(cf.superclassName) else null
        this.interfaces = cf.interfaces.map { ClassTypeSignature(cf.constant_pool.getClassInfo(it).name) }

        val ica = cf.attributes["InnerClasses"] as? InnerClasses_attribute
        if (ica != null) {
            this.innerclasses = ica.classes.map { it.getInnerClassInfo(cf.constant_pool) }
                .map { ClassTypeSignature(it.name) }
        } else {
            this.innerclasses = emptyList()
        }

        if (signature.className.contains("$")) {
            val name = signature.className
            val lastSep = name.lastIndexOf('$')
            val outerName = name.substring(0, lastSep)
            this.outerclass = ClassTypeSignature(outerName)
        } else {
            this.outerclass = null
        }

        val ema = cf.attributes["EnclosingMethod"] as? EnclosingMethod_attribute
        if (ema != null && ema.method_index > 0) {
            val className = ema.getClassName(cf.constant_pool)
            val nameAndType = cf.constant_pool.getNameAndTypeInfo(ema.method_index)
            val methodName = nameAndType.name
            val methodType = nameAndType.type

            val sigStr = "L$className;$methodName$methodType"
            val sig = SignatureParser(sigStr).parseQualifiedMethodSignature()

            this.enclosingMethod = sig
        } else {
            this.enclosingMethod = null
        }

        this.fields = cf.fields.map { ClassFileFieldDescriptor(cf, it) }
            .map { it.name to it }
            .toMap()

        this.methods = cf.methods.map { ClassFileMethodDescriptor(cf, it) }
            .map { it.signature.toString() to it }
            .toMap()
    }
}

class ClassFileMethodDescriptor (
    val cf: ClassFile,
    val method: Method
): MethodDescriptor() {
    override val qualifiedSignature: QualifiedMethodSignature

    override val exceptions: List<ClassTypeSignature>

    override val isStatic: Boolean
    override val isAbstract: Boolean
    override val isVararg: Boolean

    init {
        val mname = method.getName(cf.constant_pool)
        val mdesc = method.descriptor.getValue(cf.constant_pool)
        val signatureString = mname + mdesc
        val qualifiedSignatureString = "L${cf.name};$signatureString"
        this.qualifiedSignature = SignatureParser(qualifiedSignatureString).parseQualifiedMethodSignature()

        this.isStatic = method.access_flags.`is`(AccessFlags.ACC_STATIC)
        this.isAbstract = method.access_flags.`is`(AccessFlags.ACC_ABSTRACT)
        this.isVararg = method.access_flags.`is`(AccessFlags.ACC_VARARGS)

        val attr = method.attributes["Exceptions"] as Exceptions_attribute?
        if (attr != null) {
            this.exceptions = attr.exception_index_table
                .filter { it > 0 }
                .map { cf.constant_pool.getClassInfo(it) }
                .map { ClassTypeSignature(it.name) }
        } else {
            this.exceptions = emptyList()
        }
    }
}

class ClassFileFieldDescriptor (
    val cf: ClassFile,
    val field: Field
): FieldDescriptor() {
    override val name: String
    override val type: TypeSignature

    override val isStatic: Boolean

    init {
        this.name = field.getName(cf.constant_pool)
        this.type = SignatureParser(field.descriptor.getValue(cf.constant_pool)).parseTypeSignature()
        this.isStatic = field.access_flags.`is`(AccessFlags.ACC_STATIC)    }
}