package jcinterpret.core.ctx.meta

import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.ctx.frame.synthetic.AllocateClassType
import jcinterpret.core.ctx.frame.synthetic.SyntheticExecutionFrame
import jcinterpret.core.ctx.frame.synthetic.SyntheticInstruction
import jcinterpret.core.ctx.frame.synthetic.ValidateClassDependencies
import jcinterpret.core.descriptors.ClassTypeDescriptor
import jcinterpret.core.memory.heap.Field
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.TypeSignature
import java.util.*

class ClassArea (
    val classes: MutableMap<String, ClassType>
) {
    fun isClassLoaded(sig: ClassTypeSignature): Boolean = isClassLoaded(sig.toString())
    fun isClassLoaded(sig: String): Boolean = classes.containsKey(sig)

    fun getClass(sig: ClassTypeSignature): ClassType = getClass(sig.toString())
    fun getClass(lookupType: String): ClassType = classes[lookupType]!!

    fun buildClassLoaderFrame(sigs: Set<ClassTypeSignature>): SyntheticExecutionFrame {
        val instructions = Stack<SyntheticInstruction>()

        for (sig in sigs)
            instructions.push(ValidateClassDependencies(sig))

        for (sig in sigs)
            instructions.push(AllocateClassType(sig))

        val frame = SyntheticExecutionFrame("LOADER ${sigs.joinToString(",")}", instructions, Stack())
        return frame
    }

    fun allocateClassType(ctx: ExecutionContext, cls: ClassTypeDescriptor) {
        if (isClassLoaded(cls.signature))
            return

        val staticFields = cls.fields.values
            .filter { it.isStatic }
            .map { it.name to Field(it.name, it.type, ctx.library.getDescriptor(it.type).defaultValue) }
            .toMap().toMutableMap()

        val staticMethods = cls.methods.values
            .filter { it.isStatic }
            .map { it.signature.toString() to MethodFactory.build(ctx, it) }
            .toMap().toMutableMap()

        val instanceMethods = cls.methods.values
            .filter { !it.isStatic }
            .map { it.signature.toString() to MethodFactory.build(ctx, it) }
            .toMap().toMutableMap()

        val classType = ClassType(this, cls, staticFields, staticMethods, instanceMethods)
        classes[cls.signature.toString()] = classType
    }

    fun castWillSucceed(from: TypeSignature, to: TypeSignature): Boolean {
        TODO()
    }
}