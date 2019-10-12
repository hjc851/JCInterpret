package jcinterpret.core.descriptors

import jcinterpret.signature.*

class DescriptorLibrary (
    val resolvers: List<DescriptorResolver>
) {
    private val cache = mutableMapOf<Signature, Descriptor>()

    @Synchronized
    fun getDescriptor(sig: TypeSignature): TypeDescriptor {
        return when (sig) {
            is PrimitiveTypeSignature -> getDescriptor(sig)
            is ClassTypeSignature -> getDescriptor(sig)
            is ArrayTypeSignature -> getDescriptor(sig)

            else -> throw IllegalStateException("Unknown signature type ${sig.javaClass}")
        }
    }

    fun getDescriptor(sig: PrimitiveTypeSignature): PrimitiveTypeDescriptor {
        return when (sig.code) {
            'Z' -> PrimitiveTypeDescriptor.BOOLEAN
            'B' -> PrimitiveTypeDescriptor.BYTE
            'C' -> PrimitiveTypeDescriptor.CHAR
            'S' -> PrimitiveTypeDescriptor.SHORT
            'I' -> PrimitiveTypeDescriptor.INT
            'J' -> PrimitiveTypeDescriptor.LONG
            'F' -> PrimitiveTypeDescriptor.FLOAT
            'D' -> PrimitiveTypeDescriptor.DOUBLE
            'V' -> PrimitiveTypeDescriptor.VOID

            else -> throw IllegalStateException("Unrecognised primitive type code ${sig.code}")
        }
    }

    @Synchronized
    fun getDescriptor(sig: ClassTypeSignature): ClassTypeDescriptor {
        if (cache.containsKey(sig)) return cache[sig] as ClassTypeDescriptor

        for (resolver in resolvers) {
            val desc = resolver.tryResolveDescriptor(sig)
            if (desc != null) {
                cache[sig] = desc
                return desc
            }
        }

        throw UnresolvableDescriptorException(sig)
    }

    @Synchronized
    fun getDescriptor(sig: ArrayTypeSignature): ArrayTypeDescriptor {
        if (cache.containsKey(sig)) return cache[sig] as ArrayTypeDescriptor

        val component = getDescriptor(sig.componentType)
        val desc = ArrayTypeDescriptor(sig, component)
        cache[sig] = desc

        return desc
    }

    @Synchronized
    fun getDescriptor(sig: QualifiedMethodSignature): MethodDescriptor {
        if (cache.containsKey(sig)) return cache[sig] as MethodDescriptor

        for (resolver in resolvers) {
            val desc = resolver.tryResolveDescriptor(sig)
            if (desc != null) {
                cache[sig] = desc
                return desc
            }
        }

        throw UnresolvableDescriptorException(sig)
    }
}

