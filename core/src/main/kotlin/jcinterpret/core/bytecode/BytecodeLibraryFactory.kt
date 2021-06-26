package jcinterpret.core.bytecode

import jcinterpret.core.descriptors.ClassFileClassTypeDescriptor
import jcinterpret.core.descriptors.ClassFileFieldDescriptor
import jcinterpret.core.descriptors.ClassFileMethodDescriptor
import jcinterpret.core.descriptors.ClassPath
import jcinterpret.signature.QualifiedMethodSignature
import jcinterpret.signature.SignatureParser
import jcinterpret.signature.TypeSignature
import java.nio.file.Path

object BytecodeLibraryFactory {
    fun build(root: Path): BytecodeLibrary {
        val cp = ClassPath.build(listOf(root))

        val typeDeclarations = mutableMapOf<TypeSignature, ClassFileClassTypeDescriptor>()
        val methodDeclarations = mutableMapOf<QualifiedMethodSignature, ClassFileMethodDescriptor>()
        val fieldDeclarations =  mutableMapOf<String, ClassFileFieldDescriptor>()

        for (reader in cp.readers) {
            for (classfile in reader.classFiles) {
                val typedesc = ClassFileClassTypeDescriptor(classfile)
                typeDeclarations[typedesc.signature] = typedesc

                classfile.methods.forEach { method ->
                    val methoddesc = ClassFileMethodDescriptor(classfile, method)
                    methodDeclarations[methoddesc.qualifiedSignature] = methoddesc
                }

                classfile.fields.forEach { field ->
                    val fielddesc = ClassFileFieldDescriptor(classfile, field)
                    val id = "${typedesc.signature}.${fielddesc.name}"
                    fieldDeclarations[id] = fielddesc
                }
            }
        }

        return BytecodeLibrary(typeDeclarations, methodDeclarations, fieldDeclarations)
    }
}