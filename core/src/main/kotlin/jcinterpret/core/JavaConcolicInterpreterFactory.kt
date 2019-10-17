package jcinterpret.core

import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.ctx.frame.ExecutionFrame
import jcinterpret.core.ctx.frame.synthetic.*
import jcinterpret.core.ctx.meta.ClassArea
import jcinterpret.core.ctx.meta.HeapArea
import jcinterpret.core.ctx.meta.NativeArea
import jcinterpret.core.descriptors.DescriptorLibrary
import jcinterpret.core.memory.stack.StackReference
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.core.source.SourceLibrary
import jcinterpret.core.trace.TracerRecord
import jcinterpret.signature.QualifiedMethodSignature
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

object JavaConcolicInterpreterFactory {
    fun build (
        entryPoint: QualifiedMethodSignature,
        descLibrary: DescriptorLibrary,
        srcLibrary: SourceLibrary
    ): JavaConcolicInterpreter {

        val contexts = mutableListOf<ExecutionContext>()
        val interpreter = JavaConcolicInterpreter(contexts)

        val ctx: ExecutionContext = buildExecutionContext(entryPoint, descLibrary, srcLibrary, interpreter)
        contexts.add(ctx)

        return interpreter
    }

    private fun buildExecutionContext (
        entryPoint: QualifiedMethodSignature,
        descLibrary: DescriptorLibrary,
        srcLibrary: SourceLibrary,
        interpreter: JavaConcolicInterpreter
    ): ExecutionContext {

        val methodDescriptor = descLibrary.getDescriptor(entryPoint)

        val instructions = Stack<SyntheticInstruction>()
        val operands = Stack<StackValue>()

        instructions.push(Tracehook { TracerRecord.EntryMethod(entryPoint) })
        if (methodDescriptor.isStatic) {
            instructions.push(InvokeStatic(entryPoint))

            for (ptype in methodDescriptor.parameters.reversed()) {
                instructions.push(Tracehook { TracerRecord.EntryParameter(it.currentFrame.peek()) })
                instructions.push(AllocateSymbolic(ptype))
            }

        } else {
            instructions.push(InvokeVirtual(entryPoint.methodSignature))

            for (ptype in methodDescriptor.parameters.reversed()) {
                instructions.push(Tracehook { TracerRecord.EntryParameter(it.currentFrame.peek()) })
                instructions.push(AllocateSymbolic(ptype))
            }

            instructions. push(Tracehook { TracerRecord.EntryScope((it.currentFrame.peek()) as StackReference) })
            instructions.push(AllocateSymbolic(entryPoint.declaringClassSignature))
        }

        val frame = SyntheticExecutionFrame("BOOTSTRAP $entryPoint", instructions, operands)
        val frames = Stack<ExecutionFrame>()
        frames.push(frame)

        return ExecutionContext (
            interpreter,
            mutableListOf(),
            descLibrary,
            srcLibrary,
            HeapArea((1), mutableMapOf(), mutableMapOf()),
            ClassArea(mutableMapOf()),
            NativeArea(),
            frames
        )
    }
}