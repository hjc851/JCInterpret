package jcinterpret.core.ctx

import jcinterpret.core.ExecutionTrace
import jcinterpret.core.JavaConcolicInterpreter
import jcinterpret.core.DescriptorLibrary
import jcinterpret.core.control.*
import jcinterpret.core.ctx.frame.ExecutionFrame
import jcinterpret.core.ctx.frame.MethodBoundExecutionFrame
import jcinterpret.core.signature.QualifiedMethodSignature
import java.util.*

class ExecutionContext (
    val interpreter: JavaConcolicInterpreter,
    val library: DescriptorLibrary,
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

        while (frames.isNotEmpty()) {
            val currentFrame = this.currentFrame

            if (lastFrame != currentFrame) {
                println("Frame: $currentFrame")
                lastFrame = currentFrame
            }

            try {
                currentFrame.executeNextInstruction(this)

            } catch (e: ReturnException) {
                println("Returning from $currentFrame")
                val ret = e.value
                frames.pop()
                if (ret != null && frames.isNotEmpty())
                    frames.peek().push(ret)

            } catch (e: BreakException) {
                TODO()
            } catch (e: ThrowException) {
                TODO()

            } catch (e: HaltException) {
                TODO()

            } catch (e: ClassAreaFault) {
                println("Class area fault ${e.sigs}")

                val frame = classArea.buildClassLoaderFrame(e.sigs)
                frames.push(frame)
            }
        }

        TODO()
    }

    //
    //  Cloning
    //

    fun fork(handle: (ExecutionContext) -> Unit) {
        TODO()
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
}