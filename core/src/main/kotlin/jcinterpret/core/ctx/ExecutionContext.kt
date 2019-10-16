package jcinterpret.core.ctx

import jcinterpret.core.ExecutionLogging
import jcinterpret.core.JavaConcolicInterpreter
import jcinterpret.core.control.*
import jcinterpret.core.ctx.frame.ExecutionFrame
import jcinterpret.core.ctx.frame.MethodBoundExecutionFrame
import jcinterpret.core.ctx.frame.interpreted.InterpretedExecutionFrame
import jcinterpret.core.ctx.meta.ClassArea
import jcinterpret.core.ctx.meta.HeapArea
import jcinterpret.core.ctx.meta.NativeArea
import jcinterpret.core.descriptors.DescriptorLibrary
import jcinterpret.core.source.SourceLibrary
import jcinterpret.core.trace.ExecutionTrace
import jcinterpret.core.trace.TracerRecord
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.QualifiedMethodSignature
import java.util.*

class ExecutionContext (
    val interpreter: JavaConcolicInterpreter,
    val records: MutableList<TracerRecord>,
    val descriptorLibrary: DescriptorLibrary,
    val sourceLibrary: SourceLibrary,
    val heapArea: HeapArea,
    val classArea: ClassArea,
    val nativeArea: NativeArea,
    val frames: Stack<ExecutionFrame>
) {
    val currentFrame: ExecutionFrame
        get() = frames.peek()

    //
    //  Execution Loop
    //

    fun execute(): ExecutionTrace {
        var lastFrame: ExecutionFrame? = null

        execution@ while (frames.isNotEmpty()) {
            val currentFrame = this.currentFrame

            if (lastFrame != currentFrame) {
                if (ExecutionLogging.isEnabled) println("Frame: $currentFrame")
                lastFrame = currentFrame
            }

            try {
                currentFrame.executeNextInstruction(this)

            } catch (e: ReturnException) {
                if (ExecutionLogging.isEnabled) println("Returning from $currentFrame")
                val ret = e.value
                frames.pop()
                if (ret != null && frames.isNotEmpty())
                    frames.peek().push(ret)

            } catch (e: ThrowException) {
                val eobj = heapArea.dereference(e.ref)

                while (frames.isNotEmpty()) {
                    val frame = frames.peek()

                    if (frame is InterpretedExecutionFrame) {
                        while (frame.exceptions.isNotEmpty()) {
                            val escope = frame.exceptions.pop()

                            TODO()
                        }
                    }

                    frames.pop()
                }

                if (frames.isEmpty()) {
                    records.add(TracerRecord.UncaughtException(eobj.type as ClassTypeSignature))
                    break@execution
                }

            } catch (e: HaltException) {
                records.add(TracerRecord.Halt(e.msg))
                break@execution

            } catch (e: ClassAreaFault) {
                if (ExecutionLogging.isEnabled) println("Class area fault ${e.sigs}")

                val frame = classArea.buildClassLoaderFrame(e.sigs)
                frames.push(frame)
            }
        }

        if (ExecutionLogging.isEnabled)println("TERMINATE")
        return ExecutionTrace(records, descriptorLibrary, sourceLibrary, heapArea, classArea)
    }

    //
    //  Cloning
    //

    fun fork(handle: (ExecutionContext) -> Unit) {
        val newContext = ExecutionContextCloner.clone(this)
        interpreter.addContext(newContext)
        handle(newContext)
        if (ExecutionLogging.isEnabled) println("FORK")
    }

    //
    //  Introspection
    //

    fun methodIsInCallStack(method: QualifiedMethodSignature): Boolean {
        val sig = method.toString()
        for (frame in frames)
            if (frame is MethodBoundExecutionFrame)
                if (frame.method == method)
                    return true

        return false
    }

    fun countMethodOccuranceInCallStack(method: QualifiedMethodSignature): Int {
        var counter = 0

        for (frame in frames)
            if (frame is MethodBoundExecutionFrame)
                if (frame.method == method)
                    counter++

        return counter
    }
}