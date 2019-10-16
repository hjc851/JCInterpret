package jcinterpret.core.control

import jcinterpret.core.memory.stack.StackReference
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.QualifiedMethodSignature

abstract class JCException: Exception()

class ReturnException(val value: StackValue?, val method: QualifiedMethodSignature?): JCException()

class ThrowException(val ref: StackReference): JCException()

//class BreakException(val label: String?): JCException()
//class ContinueException(val label: String?): JCException()

class HaltException(val msg: String): JCException()

class ClassAreaFault(val sigs: Set<ClassTypeSignature>): JCException()