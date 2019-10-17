package jcinterpret.core.ctx

import jcinterpret.core.ExecutionConfig
import jcinterpret.core.JavaConcolicInterpreter
import jcinterpret.core.control.*
import jcinterpret.core.ctx.frame.ExecutionFrame
import jcinterpret.core.ctx.frame.MethodBoundExecutionFrame
import jcinterpret.core.ctx.frame.interpreted.*
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
                if (ExecutionConfig.loggingEnabled) println("Frame: $currentFrame")
                lastFrame = currentFrame
            }

            try {
                currentFrame.executeNextInstruction(this)

            } catch (e: ReturnException) {
                if (ExecutionConfig.loggingEnabled) println("Returning from $currentFrame")
                val ret = e.value
                frames.pop()
                if (ret != null && frames.isNotEmpty())
                    frames.peek().push(ret)

            } catch (e: ThrowException) {
                val eobj = heapArea.dereference(e.ref)
                val exceptCT = classArea.getClass(eobj.type as ClassTypeSignature)

                ehandle@while (frames.isNotEmpty()) {
                    val frame = frames.peek()

                    if (frame is InterpretedExecutionFrame) {
                        while (frame.exceptions.isNotEmpty()) {
                            val escope = frame.exceptions.pop()

                            while (frame.instructions.size > escope.instructionSize) frame.instructions.pop()
                            while (frame.operands.size > escope.operandsSize) frame.operands.pop()
                            while (frame.locals.scopes.size > escope.localDepth) frame.locals.scopes.pop()

                            for (handle in escope.handles) {
                                val handleCT = classArea.getClass(handle.type)
                                if (exceptCT.isAssignableTo(handleCT)) {
                                    frame.instructions.push(block_pop)
                                    frame.instructions.push(decode_stmt(handle.handle))
                                    frame.instructions.push(store(handle.name, handle.type))
                                    frame.instructions.push(push(e.ref))
                                    frame.instructions.push(allocate(handle.name, handle.type))
                                    frame.instructions.push(block_push)
                                    break@ehandle
                                }
                            }
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
                if (ExecutionConfig.loggingEnabled) println("Class area fault ${e.sigs}")

                val frame = classArea.buildClassLoaderFrame(e.sigs)
                frames.push(frame)
            }
        }

        if (ExecutionConfig.loggingEnabled) println("TERMINATE")
        return ExecutionTrace(records, heapArea)
    }

    //
    //  Cloning
    //

    fun fork(handle: (ExecutionContext) -> Unit) {
        val newContext = ExecutionContextCloner.clone(this)
        interpreter.addContext(newContext)
        handle(newContext)
        if (ExecutionConfig.loggingEnabled) println("FORK")
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

    fun countMethodOccurenceInCallStack(method: QualifiedMethodSignature): Int {
        var counter = 0

        for (frame in frames)
            if (frame is MethodBoundExecutionFrame)
                if (frame.method == method)
                    counter++

        return counter
    }
}