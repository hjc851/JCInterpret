package jcinterpret.core.control

import jcinterpret.core.memory.stack.StackReference
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.core.signature.ClassTypeSignature
import jcinterpret.core.signature.QualifiedMethodSignature
import jcinterpret.core.signature.TypeSignature

abstract class JCException: Exception()

class ReturnException(val value: StackValue?, val method: QualifiedMethodSignature?): JCException()

class ThrowException(val ref: StackReference): JCException()

class BreakException(val label: String?): JCException()

class HaltException(val msg: String): JCException()

class ClassAreaFault(val sigs: Set<ClassTypeSignature>): JCException()