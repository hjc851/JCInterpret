package jcinterpret.core

import jcinterpret.core.ctx.ClassArea
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.ctx.HeapArea
import jcinterpret.core.ctx.NativeArea
import jcinterpret.core.ctx.frame.ExecutionFrame
import jcinterpret.core.ctx.frame.synthetic.*
import jcinterpret.core.memory.stack.StackReference
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.core.signature.QualifiedMethodSignature
import jcinterpret.core.trace.TracerRecord
import java.util.*

object JavaConcolicInterpreterFactory {
    fun build (
        entryPoint: QualifiedMethodSignature,
        library: DescriptorLibrary
    ): JavaConcolicInterpreter {

        val contexts = mutableListOf<ExecutionContext>()
        val interpreter = JavaConcolicInterpreter(contexts)

        val ctx: ExecutionContext = buildExecutionContext(entryPoint, library, interpreter)
        contexts.add(ctx)

        return interpreter
    }

    private fun buildExecutionContext (
        entryPoint: QualifiedMethodSignature,
        library: DescriptorLibrary,
        interpreter: JavaConcolicInterpreter
    ): ExecutionContext {

        val methodDescriptor = library.getDescriptor(entryPoint)

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

        val frame = SyntheticExecutionFrame("Bootstrap", instructions, operands)
        val frames = Stack<ExecutionFrame>()
        frames.push(frame)

        return ExecutionContext(
            interpreter,
            library,
            HeapArea(),
            ClassArea(),
            NativeArea(),
            frames
        )
    }
}