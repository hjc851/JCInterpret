package jcinterpret.core.ctx.frame.interpreted

import jcinterpret.core.descriptors.TypeDescriptor
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.signature.TypeSignature
import java.util.*

class Locals (
    val scopes: Stack<Scope> = Stack()
) {

    init {
        if (scopes.isEmpty()) scopes.push(Scope(null))
    }

    val currentScope: Scope
        get() = scopes.peek()

    fun allocate(name: String, type: TypeDescriptor) {
        currentScope.allocate(name, type)
    }

    fun assign(name: String, value: StackValue) {
        currentScope.assign(name, value)
    }

    fun resolve(name: String): StackValue {
        try {
            return currentScope.resolve(name)
        } catch (e: Exception) {
            throw e
        }
    }

    fun typeOf(name: String): TypeDescriptor {
        for (scope in scopes.reversed()) {
            if (scope.storage.containsKey(name))
                return scope.storage[name]!!.type
        }

        throw IllegalArgumentException("Field $name not allocated in scope")
    }

    fun push() {
        val newScope = Scope(currentScope)
        scopes.push(newScope)
    }

    fun pop() {
        scopes.pop()
    }

    class Scope(
        val parent: Scope?,
        val storage: MutableMap<String, Local> = mutableMapOf()
    ) {
        data class Local(val name: String, val type: TypeDescriptor, var value: StackValue)

        fun allocate(name: String, type: TypeDescriptor) {
            storage[name] = Local(name, type, type.defaultValue)
        }

        fun assign(name: String, value: StackValue) {
            if (storage.containsKey(name)) {
                storage[name]!!.value = value
            } else if (parent != null) {
                parent.assign(name, value)
            } else {
                throw IllegalArgumentException("Field $name not allocated in scope")
            }
        }

        fun resolve(name: String): StackValue {
            try {
                return storage[name]?.value ?: parent?.resolve(name)
                ?: throw IllegalStateException("Unallocated Reference: $name")
            } catch (e: Exception) {
                throw e
            }
        }
    }
}
