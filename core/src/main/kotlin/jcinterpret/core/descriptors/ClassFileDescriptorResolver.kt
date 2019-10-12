package jcinterpret.core.descriptors

import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.QualifiedMethodSignature
import java.nio.file.Path

class ClassFileDescriptorResolver (
    libs: List<Path>
): DescriptorResolver() {

    private val classpath = ClassPath.build(libs)
    private val cache = mutableMapOf<ClassTypeSignature, ClassTypeDescriptor>()

    override fun tryResolveDescriptor(sig: ClassTypeSignature): ClassTypeDescriptor? {
        if (cache.containsKey(sig)) return cache[sig]

        val cf = classpath.tryResolve(sig.className) ?: return null
        val desc = ClassFileClassTypeDescriptor(cf)
        cache[sig] = desc
        return desc
    }

    override fun tryResolveDescriptor(sig: QualifiedMethodSignature): MethodDescriptor? {
        val cls = tryResolveDescriptor(sig.declaringClassSignature) ?: return null
        return cls.methods[sig.methodSignature.toString()]
    }
}