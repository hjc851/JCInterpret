package jcinterpret.core.ctx.meta

import jcinterpret.core.descriptors.ClassTypeDescriptor
import jcinterpret.core.memory.heap.Field
import jcinterpret.signature.MethodSignature
import jcinterpret.signature.QualifiedMethodSignature
import jcinterpret.signature.TypeSignature

class ClassType(
    val classArea: ClassArea,
    val descriptor: ClassTypeDescriptor,
    val staticFields: MutableMap<String, Field>,
    val staticMethods: MutableMap<String, Method>,
    val virtualMethods: MutableMap<String, Method>
) {
    fun getStaticField(name: String, fieldType: TypeSignature): Field {
        return staticFields[name]!!
    }

    fun resolveStaticMethod(sig: MethodSignature): Method = tryResolveStaticMethod(sig)!!
    fun tryResolveStaticMethod(sig: MethodSignature): Method? {
        return staticMethods[sig.toString()]
    }

    fun resolveSpecialMethod(sig: QualifiedMethodSignature): Method {
        try {
            return virtualMethods[sig.methodSignature.toString()]!!
        } catch (e: Exception) {
            throw e
        }
    }

    fun resolveVirtualMethod(sig: MethodSignature): Method = try {
        tryResolveVirtualMethod(sig)!!
    } catch (e: Exception) {
        throw e
    }

    fun tryResolveVirtualMethod(sig: MethodSignature): Method? {
        val sigstr = sig.toString()

        if (virtualMethods.contains(sigstr)) return virtualMethods[sigstr]

        for (iface in descriptor.interfaces) {
            val ifacedesc = classArea.getClass(iface)
            if (ifacedesc.virtualMethods.containsKey(sigstr)) {
                val method = ifacedesc.virtualMethods[sigstr]!!
                return method
            }
        }

        val sclasssig = descriptor.superclass
        if (sclasssig != null) {
            val sclass = classArea.getClass(sclasssig)
            val method = sclass.tryResolveVirtualMethod(sig)
            if (method != null) return method
        }

        return null
    }

    fun isAssignableTo(type: ClassType): Boolean {
        if (this == type) return true

        val superCls = descriptor.superclass?.let { classArea.getClass(it) }
        if (superCls != null) {
            val assignableToSuper = superCls.isAssignableTo(type)
            if (assignableToSuper) return true
        }

        for (iface in descriptor.interfaces) {
            val ifaceCls = classArea.getClass(iface)
            val ifaceAssignable = ifaceCls.isAssignableTo(type)
            if (ifaceAssignable) return true
        }

        return false
    }
}