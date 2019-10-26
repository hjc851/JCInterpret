package jcinterpret.core.ctx

import jcinterpret.core.ctx.frame.ExecutionFrame
import jcinterpret.core.ctx.frame.interpreted.*
import jcinterpret.core.ctx.frame.synthetic.SyntheticExecutionFrame
import jcinterpret.core.ctx.frame.synthetic.SyntheticInstruction
import jcinterpret.core.ctx.meta.ClassArea
import jcinterpret.core.ctx.meta.ClassType
import jcinterpret.core.ctx.meta.HeapArea
import jcinterpret.core.memory.heap.*
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.signature.ArrayTypeSignature
import jcinterpret.signature.ClassTypeSignature
import java.util.*

object ExecutionContextCloner {
    fun clone(oldCtx: ExecutionContext): ExecutionContext {
        val records = oldCtx.records.toMutableList()
        val descriptorLibrary = oldCtx.descriptorLibrary
        val sourceList = oldCtx.sourceLibrary
        val heapArea = clone(oldCtx.heapArea)
        val classArea = clone(oldCtx.classArea)
        val nativeArea = oldCtx.nativeArea
        val frames = clone(oldCtx.frames)

        return ExecutionContext (
            oldCtx.interpreter,
            records,
            descriptorLibrary,
            sourceList,
            heapArea,
            classArea,
            nativeArea,
            frames
        )
    }

    fun clone(oldHeap: HeapArea): HeapArea {
        val counter = (oldHeap.currentId())
        val storage = oldHeap.storage.map { it.key to clone(it.value) }.toMap().toMutableMap()
        val literalRefs = oldHeap.literalRefs.toMutableMap()

        return HeapArea(counter, storage, literalRefs)
    }

    fun clone(value: HeapValue): HeapValue {

        val obj = when (value.javaClass) {
            ConcreteObject::class.java -> {
                (value as ConcreteObject)
                ConcreteObject (
                    value.id,
                    value.type as ClassTypeSignature,
                    value.fields.map {
                        it.key to Field(it.value.name, it.value.type, it.value.value)
                    }.toMap().toMutableMap()
                )
            }

            SymbolicObject::class.java -> {
                (value as SymbolicObject)
                SymbolicObject (
                    value.id,
                    value.type as ClassTypeSignature,
                    value.fields.map {
                        it.key to Field(it.value.name, it.value.type, it.value.value)
                    }.toMap().toMutableMap()
                )
            }

            SymbolicArray::class.java -> {
                (value as SymbolicArray)
                SymbolicArray (
                    value.id,
                    value.type as ArrayTypeSignature,
                    value.storage.toMutableMap(),
                    value.size
                )
            }

            BoxedStackValueObject::class.java -> {
                (value as BoxedStackValueObject)
                BoxedStackValueObject (
                    value.id,
                    value.type as ClassTypeSignature,
                    value.value
                )
            }

            BoxedStringObject::class.java -> {
                (value as BoxedStringObject)
                BoxedStringObject (
                    value.id,
                    value.type as ClassTypeSignature,
                    value.value
                )
            }

            ClassObject::class.java -> {
                (value as ClassObject)
                ClassObject (
                    value.id,
                    value.type as ClassTypeSignature,
                    value.value
                )
            }

            else -> throw IllegalArgumentException("Unknown heap value tyoe ${value.javaClass}")
        }

        return obj
    }

    fun clone(oldClassArea: ClassArea): ClassArea {
        val classes = mutableMapOf<String, ClassType>()
        val ca = ClassArea(classes)
        oldClassArea.classes.map { it.key to clone(it.value, ca) }.toMap(classes)

        return ca
    }

    fun clone(ct: ClassType, ca: ClassArea): ClassType {
        val desc = ct.descriptor
        val staticFields = ct.staticFields.map { it.key to it.value.copy() }.toMap().toMutableMap()
        val staticMethods = ct.staticMethods.toMutableMap()
        val virtualMethods = ct.virtualMethods.toMutableMap()

        return ClassType(ca, desc, staticFields, staticMethods, virtualMethods)
    }

    fun clone(oldFrames: Stack<ExecutionFrame>): Stack<ExecutionFrame> {
        val newStack = Stack<ExecutionFrame>()

        for (frame in oldFrames)
            newStack.push(clone(frame))

        return newStack
    }

    fun clone(oldFrame: ExecutionFrame): ExecutionFrame {
        val frame = when (oldFrame) {
            is SyntheticExecutionFrame -> {
                SyntheticExecutionFrame (
                    oldFrame.label,
                    oldFrame.instructions.clone() as Stack<SyntheticInstruction>,
                    oldFrame.operands.clone() as Stack<StackValue>
                )
            }

            is InterpretedExecutionFrame -> {
                InterpretedExecutionFrame (
                    oldFrame.instructions.clone() as Stack<InterpretedInstruction>,
                    oldFrame.operands.clone() as Stack<StackValue>,
                    clone(oldFrame.locals),
                    oldFrame.exceptions.clone() as Stack<ExceptionScope>,
                    oldFrame.breaks.clone() as Stack<BreakScope>,
                    oldFrame.continues.clone() as Stack<ContinueScope>,
                    oldFrame.method
                )
            }

            else -> throw IllegalArgumentException("Unknown oldFrame ${oldFrame.javaClass}")
        }

        return frame
    }

    fun clone(locals: Locals): Locals {
        val newScopes = Stack<Locals.Scope>()

        var previousScope: Locals.Scope? = null
        for (scope in locals.scopes) {
            val newScope = clone(scope, previousScope)
            newScopes.push(newScope)
            previousScope = newScope
        }

        return Locals(newScopes)
    }

    fun clone(oldScope: Locals.Scope, parent: Locals.Scope?): Locals.Scope {
        val scope = Locals.Scope(
                parent,
                oldScope.storage.map { it.key to Locals.Scope.Local(it.value.name, it.value.type, it.value.value) }.toMap().toMutableMap()
        )
        return scope
    }
}