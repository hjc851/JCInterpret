package jcinterpret.core.descriptors

import com.sun.tools.classfile.ClassFile
import com.sun.tools.jdeps.ClassFileReader
import java.nio.file.Path

class ClassPath(val readers: List<ClassFileReader>) {

    companion object {
        fun build(cpItems: List<Path>): ClassPath {
            val readers = mutableListOf<ClassFileReader>()

            for (cpItem in cpItems)
                readers.add(ClassFileReader.newInstance(cpItem))

            return ClassPath(readers)
        }
    }

    private val cache = mutableMapOf<String, ClassFile>()

    fun tryResolve(className: String): ClassFile? {

        if (cache.containsKey(className))
            return cache[className]

        for (reader in readers) {
            val cls = reader.getClassFile(className)

            if (cls != null) {
                cache[className] = cls
                return cls
            }
        }

        return null
    }

    fun resolve(className: String): ClassFile {
        return tryResolve(className) ?: throw IllegalStateException("Failed resolving classfile $className")
    }
}