package jcinterpret.core.ctx.frame.bytecode

import com.sun.tools.classfile.ConstantPool
import jcinterpret.core.control.ReturnException
import jcinterpret.core.ctx.ExecutionContext
import jcinterpret.core.memory.heap.SymbolicArray
import jcinterpret.core.memory.stack.*
import jcinterpret.core.trace.TraceRecord

object BytecodeExecutor {

    val handles = mapOf(
        "32" to ::aaload,
        "53" to ::aastore,
        "01" to ::aconst_null,
        "19" to ::aload,
        "2a" to ::aload_0,
        "2b" to ::aload_1,
        "2c" to ::aload_2,
        "2d" to ::aload_3,
        "bd" to ::anewarray,
        "b0" to ::areturn,
        "be" to ::arraylength,
        "3a" to ::astore,
        "4b" to ::astore_0,
        "4c" to ::astore_1,
        "4d" to ::astore_2,
        "4e" to ::astore_3,
        "bf" to ::athrow,
        "33" to ::baload,
        "54" to ::bastore,
        "10" to ::bipush,
        "ca" to ::breakpoint,
        "34" to ::caload,
        "55" to ::castore,
        "c0" to ::checkcast,
        "90" to ::d2f,
        "8e" to ::d2i,
        "8f" to ::d2l,
        "63" to ::dadd,
        "31" to ::daload,
        "52" to ::dastore,
        "98" to ::dcmpg,
        "97" to ::dcmpl,
        "0e" to ::dconst_0,
        "0f" to ::dconst_1,
        "6f" to ::ddiv,
        "18" to ::dload,
        "26" to ::dload_0,
        "27" to ::dload_1,
        "28" to ::dload_2,
        "29" to ::dload_3,
        "6b" to ::dmul,
        "77" to ::dneg,
        "73" to ::drem,
        "af" to ::dreturn,
        "39" to ::dstore,
        "47" to ::dstore_0,
        "48" to ::dstore_1,
        "49" to ::dstore_2,
        "4a" to ::dstore_3,
        "67" to ::dsub,
        "59" to ::dup,
        "5a" to ::dup_x1,
        "5b" to ::dup_x2,
        "5c" to ::dup2,
        "5d" to ::dup2_x1,
        "5e" to ::dup2_x2,
        "8d" to ::f2d,
        "8b" to ::f2i,
        "8c" to ::f2l,
        "62" to ::fadd,
        "30" to ::faload,
        "51" to ::fastore,
        "96" to ::fcmpg,
        "95" to ::fcmpl,
        "0b" to ::fconst_0,
        "0c" to ::fconst_1,
        "0d" to ::fconst_2,
        "6e" to ::fdiv,
        "17" to ::fload,
        "22" to ::fload_0,
        "23" to ::fload_1,
        "24" to ::fload_2,
        "25" to ::fload_3,
        "6a" to ::fmul,
        "76" to ::fneg,
        "72" to ::frem,
        "ae" to ::freturn,
        "38" to ::fstore,
        "43" to ::fstore_0,
        "44" to ::fstore_1,
        "45" to ::fstore_2,
        "46" to ::fstore_3,
        "66" to ::fsub,
        "b4" to ::getfield,
        "b2" to ::getstatic,
        "a7" to ::goto,
        "c8" to ::goto_w,
        "91" to ::i2b,
        "92" to ::i2c,
        "87" to ::i2d,
        "86" to ::i2f,
        "85" to ::i2l,
        "93" to ::i2s,
        "60" to ::iadd,
        "2e" to ::iaload,
        "7e" to ::iand,
        "4f" to ::iastore,
        "02" to ::iconst_m1,
        "03" to ::iconst_0,
        "04" to ::iconst_1,
        "05" to ::iconst_2,
        "06" to ::iconst_3,
        "07" to ::iconst_4,
        "08" to ::iconst_5,
        "6c" to ::idiv,
        "a5" to ::if_acmpeq,
        "a6" to ::if_acmpne,
        "9f" to ::if_icmpeq,
        "a2" to ::if_icmpge,
        "a3" to ::if_icmpgt,
        "a4" to ::if_icmple,
        "a1" to ::if_icmplt,
        "a0" to ::if_icmpne,
        "99" to ::ifeq,
        "9c" to ::ifge,
        "9d" to ::ifgt,
        "9e" to ::ifle,
        "9b" to ::iflt,
        "9a" to ::ifne,
        "c7" to ::ifnonnull,
        "c6" to ::ifnull,
        "84" to ::iinc,
        "15" to ::iload,
        "1a" to ::iload_0,
        "1b" to ::iload_1,
        "1c" to ::iload_2,
        "1d" to ::iload_3,
        "fe" to ::impdep1,
        "ff" to ::impdep2,
        "68" to ::imul,
        "74" to ::ineg,
        "c1" to ::instanceof,
        "ba" to ::invokedynamic,
        "b9" to ::invokeinterface,
        "b7" to ::invokespecial,
        "b8" to ::invokestatic,
        "b6" to ::invokevirtual,
        "80" to ::ior,
        "70" to ::irem,
        "ac" to ::ireturn,
        "78" to ::ishl,
        "7a" to ::ishr,
        "36" to ::istore,
        "3b" to ::istore_0,
        "3c" to ::istore_1,
        "3d" to ::istore_2,
        "3e" to ::istore_3,
        "64" to ::isub,
        "7c" to ::iushr,
        "82" to ::ixor,
        "8a" to ::l2d,
        "89" to ::l2f,
        "88" to ::l2i,
        "61" to ::ladd,
        "2f" to ::laload,
        "7f" to ::land,
        "50" to ::lastore,
        "94" to ::lcmp,
        "09" to ::lconst_0,
        "0a" to ::lconst_1,
        "12" to ::ldc,
        "13" to ::ldc_w,
        "14" to ::ldc2_w,
        "6d" to ::ldiv,
        "16" to ::lload,
        "1e" to ::lload_0,
        "1f" to ::lload_1,
        "20" to ::lload_2,
        "21" to ::lload_3,
        "69" to ::lmul,
        "75" to ::lneg,
        "ab" to ::lookupswitch,
        "81" to ::lor,
        "71" to ::lrem,
        "ad" to ::lreturn,
        "79" to ::lshl,
        "7b" to ::lshr,
        "37" to ::lstore,
        "3f" to ::lstore_0,
        "40" to ::lstore_1,
        "41" to ::lstore_2,
        "42" to ::lstore_3,
        "65" to ::lsub,
        "7d" to ::lushr,
        "83" to ::lxor,
        "c2" to ::monitorenter,
        "c3" to ::monitorexit,
        "c5" to ::multianewarray,
        "bb" to ::new,
        "bc" to ::newarray,
        "00" to ::nop,
        "57" to ::pop,
        "58" to ::pop2,
        "b5" to ::putfield,
        "b3" to ::putstatic,
        "a9" to ::ret,
        "b1" to ::`return`,
        "35" to ::saload,
        "56" to ::sastore,
        "11" to ::sipush,
        "5f" to ::swap,
        "aa" to ::tableswitch,
        "c4" to ::wide
    )

    fun executeNextInstruction(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val instr = frame.nextByte()
        val hexinstr = "%02X".format(instr).toLowerCase()
        val handle = handles[hexinstr] ?: throw Exception("Unimplemented instruction hex ${hexinstr}")
        println(handle)
        handle.invoke(ctx, frame)
    }

    //  //  //  //  //  //
    //  Instructions    //
    //  //  //  //  //  //

    // load onto the stack a reference from an array
    // arrayref, index → value
    fun aaload(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val idx = frame.pop()
        val aarrref = frame.pop() as ReferenceValue

        val aarr = ctx.heapArea.dereference(aarrref) as SymbolicArray
        val value = aarr.get(idx, jcinterpret.signature.ClassTypeSignature.OBJECT, ctx)

        frame.push(value)
    }

    // store a reference in an array
    // arrayref, index, value →
    fun aastore(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // push a null reference onto the stack
    // → null
    fun aconst_null(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        frame.push(StackNil)
    }

    // load a reference onto the stack from a local variable #index
    // → objectref
    fun aload(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val ref = frame.nextByte().toInt()
        val value = frame.locals[ref] as StackReference
        frame.push(value)
    }

    // load a reference onto the stack from local variable 0
    // → objectref
    fun aload_0(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.locals[0] as StackReference
        frame.push(value)
    }

    // load a reference onto the stack from local variable 1
    // → objectref
    fun aload_1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.locals[1] as StackReference
        frame.push(value)
    }

    // load a reference onto the stack from local variable 2
    // → objectref
    fun aload_2(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.locals[2] as StackReference
        frame.push(value)
    }

    // load a reference onto the stack from local variable 3
    // → objectref
    fun aload_3(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.locals[3] as StackReference
        frame.push(value)
    }

    // create a new array of references of length count and component type identified by the class reference index(indexbyte1 << 8 | indexbyte2) in the constant pool
    // count → arrayref
    fun anewarray(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // return a reference from a method
// objectref → [empty]
    fun areturn(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // get the length of an array
    // arrayref → length
    fun arraylength(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val ref = frame.pop() as StackReference
        val arr = ctx.heapArea.dereference(ref) as SymbolicArray
        val size = arr.size
        frame.push(size)
    }

    // store a reference into a local variable #index
    // objectref →
    fun astore(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val idx = frame.nextByte().toInt()
        val ref = frame.pop() as StackReference
        frame.locals[idx] = ref
    }

    // store a reference into local variable 0
    // objectref →
    fun astore_0(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val ref = frame.pop() as StackReference
        frame.locals[0] = ref
    }

    // store a reference into local variable 1
    // objectref →
    fun astore_1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val ref = frame.pop() as StackReference
        frame.locals[1] = ref
    }

    // store a reference into local variable 2
    // objectref →
    fun astore_2(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val ref = frame.pop() as StackReference
        frame.locals[2] = ref
    }

    // store a reference into local variable 3
    // objectref →
    fun astore_3(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val ref = frame.pop() as StackReference
        frame.locals[3] = ref
    }

    // throws an error or exception (notice that the rest of the stack is cleared, leaving only a reference to the Throwable)
// objectref → [empty], objectref
    fun athrow(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a byte or Boolean value from an array
// arrayref, index → value
    fun baload(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a byte or Boolean value into an array
// arrayref, index, value →
    fun bastore(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // push a byte onto the stack as an integer value
// → value
    fun bipush(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // reserved for breakpoints in Java debuggers; should not appear in any class file
//
    fun breakpoint(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a char from an array
// arrayref, index → value
    fun caload(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a char into an array
// arrayref, index, value →
    fun castore(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // checks whether an objectref is of a certain type, the class reference of which is in the constant pool at index(indexbyte1 << 8 | indexbyte2)
// objectref → objectref
    fun checkcast(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // convert a double to a float
// value → result
    fun d2f(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // convert a double to an int
// value → result
    fun d2i(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // convert a double to a long
// value → result
    fun d2l(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // add two doubles
// value1, value2 → result
    fun dadd(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a double from an array
// arrayref, index → value
    fun daload(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a double into an array
// arrayref, index, value →
    fun dastore(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // compare two doubles, 1 on NaN
// value1, value2 → result
    fun dcmpg(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // compare two doubles, -1 on NaN
// value1, value2 → result
    fun dcmpl(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // push the constant 0.0 (a double) onto the stack
// → 0.0
    fun dconst_0(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // push the constant 1.0 (a double) onto the stack
// → 1.0
    fun dconst_1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // divide two doubles
// value1, value2 → result
    fun ddiv(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a double value from a local variable #index
// → value
    fun dload(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val idx = frame.nextByte()
        val value = frame.locals[idx.toInt()]
        frame.push(value)
    }

    // load a double from local variable 0
    // → value
    fun dload_0(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a double from local variable 1
// → value
    fun dload_1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a double from local variable 2
// → value
    fun dload_2(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a double from local variable 3
// → value
    fun dload_3(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // multiply two doubles
// value1, value2 → result
    fun dmul(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // negate a double
// value → result
    fun dneg(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // get the remainder from a division between two doubles
// value1, value2 → result
    fun drem(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // return a double from a method
// value → [empty]
    fun dreturn(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a double value into a local variable #index
// value →
    fun dstore(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a double into local variable 0
// value →
    fun dstore_0(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a double into local variable 1
// value →
    fun dstore_1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a double into local variable 2
// value →
    fun dstore_2(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a double into local variable 3
// value →
    fun dstore_3(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // subtract a double from another
// value1, value2 → result
    fun dsub(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // duplicate the value on top of the stack
// value → value, value
    fun dup(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // insert a copy of the top value into the stack two values from the top. value1 and value2 must not be of the type double or long.
// value2, value1 → value1, value2, value1
    fun dup_x1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // insert a copy of the top value into the stack two (if value2 is double or long it takes up the entry of value3, too) or three values (if value2 is neither double nor long) from the top
// value3, value2, value1 → value1, value3, value2, value1
    fun dup_x2(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // duplicate top two stack words (two values, if value1 is not double nor long; a single value, if value1 is double or long)
// {value2, value1} → {value2, value1}, {value2, value1}
    fun dup2(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // duplicate two words and insert beneath third word (see explanation above)
// value3, {value2, value1} → {value2, value1}, value3, {value2, value1}
    fun dup2_x1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // duplicate two words and insert beneath fourth word
// {value4, value3}, {value2, value1} → {value2, value1}, {value4, value3}, {value2, value1}
    fun dup2_x2(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // convert a float to a double
// value → result
    fun f2d(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // convert a float to an int
// value → result
    fun f2i(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // convert a float to a long
// value → result
    fun f2l(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // add two floats
// value1, value2 → result
    fun fadd(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a float from an array
// arrayref, index → value
    fun faload(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a float in an array
// arrayref, index, value →
    fun fastore(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // compare two floats, 1 on NaN
// value1, value2 → result
    fun fcmpg(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // compare two floats, -1 on NaN
// value1, value2 → result
    fun fcmpl(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // push 0.0f on the stack
// → 0.0f
    fun fconst_0(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // push 1.0f on the stack
// → 1.0f
    fun fconst_1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // push 2.0f on the stack
// → 2.0f
    fun fconst_2(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // divide two floats
// value1, value2 → result
    fun fdiv(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a float value from a local variable #index
// → value
    fun fload(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a float value from local variable 0
// → value
    fun fload_0(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a float value from local variable 1
// → value
    fun fload_1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a float value from local variable 2
// → value
    fun fload_2(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a float value from local variable 3
// → value
    fun fload_3(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // multiply two floats
// value1, value2 → result
    fun fmul(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // negate a float
// value → result
    fun fneg(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // get the remainder from a division between two floats
// value1, value2 → result
    fun frem(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // return a float
// value → [empty]
    fun freturn(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a float value into a local variable #index
// value →
    fun fstore(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a float value into local variable 0
// value →
    fun fstore_0(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a float value into local variable 1
// value →
    fun fstore_1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a float value into local variable 2
// value →
    fun fstore_2(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a float value into local variable 3
// value →
    fun fstore_3(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // subtract two floats
// value1, value2 → result
    fun fsub(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // get a field value of an object objectref, where the field is identified by field reference in the constant pool index(indexbyte1 << 8 | indexbyte2)
// objectref → value
    fun getfield(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // get a static field value of a class, where the field is identified by field reference in the constant pool index(indexbyte1 << 8 | indexbyte2)
// → value
    fun getstatic(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // goes to another instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)
// [no change]
    fun goto(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val offset = frame.nextShort()
        frame.branch(offset)
    }

    // goes to another instruction at branchoffset (signed int constructed from unsigned bytes branchbyte1 << 24 | branchbyte2 << 16 | branchbyte3 << 8 | branchbyte4)
// [no change]
    fun goto_w(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // convert an int into a byte
// value → result
    fun i2b(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // convert an int into a character
// value → result
    fun i2c(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // convert an int into a double
// value → result
    fun i2d(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // convert an int into a float
// value → result
    fun i2f(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // convert an int into a long
// value → result
    fun i2l(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // convert an int into a short
// value → result
    fun i2s(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // add two ints
// value1, value2 → result
    fun iadd(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load an int from an array
// arrayref, index → value
    fun iaload(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // perform a bitwise AND on two integers
// value1, value2 → result
    fun iand(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store an int into an array
// arrayref, index, value →
    fun iastore(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load the int value −1 onto the stack
// → -1
    fun iconst_m1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        frame.push(StackInt(-1))
    }

    // load the int value 0 onto the stack
    // → 0
    fun iconst_0(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        frame.push(StackInt(0))
    }

    // load the int value 1 onto the stack
// → 1
    fun iconst_1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        frame.push(StackInt(1))
    }

    // load the int value 2 onto the stack
// → 2
    fun iconst_2(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        frame.push(StackInt(2))
    }

    // load the int value 3 onto the stack
// → 3
    fun iconst_3(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        frame.push(StackInt(3))
    }

    // load the int value 4 onto the stack
// → 4
    fun iconst_4(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        frame.push(StackInt(4))
    }

    // load the int value 5 onto the stack
// → 5
    fun iconst_5(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        frame.push(StackInt(5))
    }

    // divide two integers
// value1, value2 → result
    fun idiv(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // if references are equal, branch to instruction at branchoffset(signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)
// value1, value2 →
    fun if_acmpeq(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // if references are not equal, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)
// value1, value2 →
    fun if_acmpne(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // if ints are equal, branch to instruction at branchoffset(signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)
// value1, value2 →
    fun if_icmpeq(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // if value1 is greater than or equal to value2, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)
    // value1, value2 →
    fun if_icmpge(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        frame.markLoop()
        val exceeded = frame.exceededLoopLimit()

        val v1 = frame.pop()
        val v2 = frame.pop()
        val offset = frame.nextShort()

        if (exceeded) {
            frame.branch(offset)
            return
        }

        if (v1 is ConcreteValue<*> && v2 is ConcreteValue<*>) {
            val truth = (v1 as StackInt).value >= (v2 as StackInt).value

            if (truth) {
                frame.branch(offset)
            }

        } else {
            // Assume v1 >= v2 in branch
            ctx.fork {
                val fframe = it.currentFrame as  BytecodeExecutionFrame
                fframe.branch(offset)

                it.records.add(TraceRecord.Assertion(BinaryOperationValue(v1, v2, StackType.INT, BinaryOperator.GREATEREQUALS), true))
            }

            // Here, v1 < v2
            ctx.records.add(TraceRecord.Assertion(BinaryOperationValue(v1, v2, StackType.INT, BinaryOperator.GREATEREQUALS), false))
        }
    }

    // if value1 is greater than value2, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)
// value1, value2 →
    fun if_icmpgt(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // if value1 is less than or equal to value2, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)
// value1, value2 →
    fun if_icmple(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // if value1 is less than value2, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)
// value1, value2 →
    fun if_icmplt(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // if ints are not equal, branch to instruction at branchoffset(signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)
// value1, value2 →
    fun if_icmpne(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // if value is 0, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)
    // value →
    fun ifeq(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.pop()
        TODO()
    }

    // if value is greater than or equal to 0, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)
    // value →
    fun ifge(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.pop()
        TODO()
    }

    // if value is greater than 0, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)
    // value →
    fun ifgt(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.pop()
        TODO()
    }

    // if value is less than or equal to 0, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)
    // value →
    fun ifle(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.pop()
        val offset = frame.nextShort()

        if (value is ConcreteValue<*>) {
            val truth = (value as StackInt).value <= 0

            if (truth) {
                frame.branch(offset)
            }

        } else {
            // Assume value is <= 0 in branch
            ctx.fork {
                val fframe = it.currentFrame as  BytecodeExecutionFrame
                fframe.branch(offset)

                it.records.add(TraceRecord.Assertion(BinaryOperationValue(value, StackInt(0), StackType.INT, BinaryOperator.LESSEQUALS), true))
            }

            // Here, value is > 0
            ctx.records.add(TraceRecord.Assertion(BinaryOperationValue(value, StackInt(0), StackType.INT, BinaryOperator.LESSEQUALS), false))
        }
    }

    // if value is less than 0, branch to instruction at branchoffset(signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)
    // value →
    fun iflt(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.pop()
        TODO()
    }

    // if value is not 0, branch to instruction at branchoffset(signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)
    // value →
    fun ifne(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.pop()
        TODO()
    }

    // if value is not null, branch to instruction at branchoffset(signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)
    // value →
    fun ifnonnull(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.pop() as ReferenceValue
        TODO()
    }

    // if value is null, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 | branchbyte2)
    // value →
    fun ifnull(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.pop() as ReferenceValue
        TODO()
    }

    // increment local variable #index by signed byte const
    // [No change]
    fun iinc(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val idx = frame.nextByte().toInt()

        val value = frame.locals[idx]

        if (value is ConcreteValue<*>) {
            var incValue = (value as StackInt).value + 1
            frame.locals[idx] = StackInt(incValue)
        } else {
            TODO("Not implemented")
        }
    }

    // load an int value from a local variable #index
    // → value
    fun iload(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val idx = frame.nextByte().toInt()
        val value = frame.locals[idx]

        frame.push(value)
    }

    // load an int value from local variable 0
    // → value
    fun iload_0(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.locals[0]
        frame.push(value)
    }

    // load an int value from local variable 1
// → value
    fun iload_1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.locals[1]
        frame.push(value)
    }

    // load an int value from local variable 2
// → value
    fun iload_2(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.locals[2]
        frame.push(value)
    }

    // load an int value from local variable 3
// → value
    fun iload_3(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.locals[3]
        frame.push(value)
    }

    // reserved for implementation-dependent operations within debuggers; should not appear in any class file
//
    fun impdep1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // reserved for implementation-dependent operations within debuggers; should not appear in any class file
//
    fun impdep2(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // multiply two integers
// value1, value2 → result
    fun imul(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // negate int
// value → result
    fun ineg(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // determines if an object objectref is of a given type, identified by class reference index in constant pool (indexbyte1 << 8 | indexbyte2)
// objectref → result
    fun instanceof(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // invokes a dynamic method and puts the result on the stack (might be void); the method is identified by method reference index in constant pool (indexbyte1 << 8 | indexbyte2)
// [arg1, arg2, ...] → result
    fun invokedynamic(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // invokes an interface method on object objectref and puts the result on the stack (might be void); the interface method is identified by method reference index in constant pool (indexbyte1 << 8 | indexbyte2)
// objectref, [arg1, arg2, ...] → result
    fun invokeinterface(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // invoke instance method on object objectref and puts the result on the stack (might be void); the method is identified by method reference index in constant pool (indexbyte1 << 8 | indexbyte2)
// objectref, [arg1, arg2, ...] → result
    fun invokespecial(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // invoke a static method and puts the result on the stack (might be void); the method is identified by method reference index in constant pool (indexbyte1 << 8 | indexbyte2)
// [arg1, arg2, ...] → result
    fun invokestatic(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // invoke virtual method on object objectref and puts the result on the stack (might be void); the method is identified by method reference index in constant pool (indexbyte1 << 8 | indexbyte2)
// objectref, [arg1, arg2, ...] → result
    fun invokevirtual(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // bitwise int OR
// value1, value2 → result
    fun ior(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // logical int remainder
// value1, value2 → result
    fun irem(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // return an integer from a method
// value → [empty]
    fun ireturn(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // int shift left
// value1, value2 → result
    fun ishl(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // int arithmetic shift right
// value1, value2 → result
    fun ishr(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store int value into variable #index
// value →
    fun istore(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.pop()
        val idx = frame.nextByte().toInt()

        frame.locals[idx] = value
    }

    // store int value into variable 0
// value →
    fun istore_0(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.pop()
        frame.locals[0] = value
    }

    // store int value into variable 1
// value →
    fun istore_1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.pop()
        frame.locals[1] = value
    }

    // store int value into variable 2
// value →
    fun istore_2(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.pop()
        frame.locals[2] = value
    }

    // store int value into variable 3
// value →
    fun istore_3(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val value = frame.pop()
        frame.locals[3] = value
    }

    // int subtract
// value1, value2 → result
    fun isub(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // int logical shift right
// value1, value2 → result
    fun iushr(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // int xor
// value1, value2 → result
    fun ixor(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // convert a long to a double
// value → result
    fun l2d(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // convert a long to a float
// value → result
    fun l2f(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // convert a long to a int
// value → result
    fun l2i(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // add two longs
// value1, value2 → result
    fun ladd(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a long from an array
// arrayref, index → value
    fun laload(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // bitwise AND of two longs
// value1, value2 → result
    fun land(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a long to an array
// arrayref, index, value →
    fun lastore(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // push 0 if the two longs are the same, 1 if value1 is greater than value2, -1 otherwise
// value1, value2 → result
    fun lcmp(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // push 0L (the number zero with type long) onto the stack
// → 0L
    fun lconst_0(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // push 1L (the number one with type long) onto the stack
// → 1L
    fun lconst_1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // push a constant #index from a constant pool (String, int, float, Class, java.lang.invoke.MethodType, java.lang.invoke.MethodHandle, or a dynamically-computed constant) onto the stack
// → value
    private fun ldc(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        val idx = frame.nextByte().toInt()
        val constant = frame.cp.get(idx)

        constant.accept(object : ConstantPool.Visitor<Any, Any?> {
            override fun visitClass(info: ConstantPool.CONSTANT_Class_info, p: Any?): Any {
                TODO("Not yet implemented")
            }

            override fun visitDouble(info: ConstantPool.CONSTANT_Double_info, p: Any?): Any {
                TODO("Not yet implemented")
            }

            override fun visitFieldref(info: ConstantPool.CONSTANT_Fieldref_info, p: Any?): Any {
                TODO("Not yet implemented")
            }

            override fun visitFloat(info: ConstantPool.CONSTANT_Float_info, p: Any?): Any {
                TODO("Not yet implemented")
            }

            override fun visitInteger(info: ConstantPool.CONSTANT_Integer_info, p: Any?): Any {
                TODO("Not yet implemented")
            }

            override fun visitInterfaceMethodref(info: ConstantPool.CONSTANT_InterfaceMethodref_info, p: Any?): Any {
                TODO("Not yet implemented")
            }

            override fun visitInvokeDynamic(info: ConstantPool.CONSTANT_InvokeDynamic_info, p: Any?): Any {
                TODO("Not yet implemented")
            }

//            override fun visitDynamicConstant(info: ConstantPool.CONSTANT_Dynamic_info, p: Any?): Any {
//                TODO("Not yet implemented")
//            }

            override fun visitLong(info: ConstantPool.CONSTANT_Long_info, p: Any?): Any {
                TODO("Not yet implemented")
            }

            override fun visitMethodref(info: ConstantPool.CONSTANT_Methodref_info, p: Any?): Any {
                TODO("Not yet implemented")
            }

            override fun visitMethodHandle(info: ConstantPool.CONSTANT_MethodHandle_info, p: Any?): Any {
                TODO("Not yet implemented")
            }

            override fun visitMethodType(info: ConstantPool.CONSTANT_MethodType_info, p: Any?): Any {
                TODO("Not yet implemented")
            }

//            override fun visitModule(info: ConstantPool.CONSTANT_Module_info, p: Any?): Any {
//                TODO("Not yet implemented")
//            }

            override fun visitNameAndType(info: ConstantPool.CONSTANT_NameAndType_info, p: Any?): Any {
                TODO("Not yet implemented")
            }

//            override fun visitPackage(info: ConstantPool.CONSTANT_Package_info, p: Any?): Any {
//                TODO("Not yet implemented")
//            }

            override fun visitString(info: ConstantPool.CONSTANT_String_info, p: Any?): Any {
                val str = info.string
                val strObject = ctx.heapArea.getOrAllocateString(str)
                val strRef = strObject.ref()
                frame.push(strRef)
                return this
            }

            override fun visitUtf8(info: ConstantPool.CONSTANT_Utf8_info, p: Any?): Any {
                TODO("Not yet implemented")
            }
        }, null)
    }

    // push a constant #index from a constant pool (String, int, float, Class, java.lang.invoke.MethodType, java.lang.invoke.MethodHandle, or a dynamically-computed constant) onto the stack (wide index is constructed as indexbyte1 << 8 | indexbyte2)
// → value
    fun ldc_w(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // push a constant #index from a constant pool (double, long, or a dynamically-computed constant) onto the stack (wide index is constructed as indexbyte1 << 8 | indexbyte2)
// → value
    fun ldc2_w(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // divide two longs
// value1, value2 → result
    fun ldiv(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a long value from a local variable #index
// → value
    fun lload(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a long value from a local variable 0
// → value
    fun lload_0(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a long value from a local variable 1
// → value
    fun lload_1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a long value from a local variable 2
// → value
    fun lload_2(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // load a long value from a local variable 3
// → value
    fun lload_3(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // multiply two longs
// value1, value2 → result
    fun lmul(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // negate a long
// value → result
    fun lneg(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // a target address is looked up from a table using a key and execution continues from the instruction at that address
// key →
    fun lookupswitch(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // bitwise OR of two longs
// value1, value2 → result
    fun lor(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // remainder of division of two longs
// value1, value2 → result
    fun lrem(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // return a long value
// value → [empty]
    fun lreturn(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // bitwise shift left of a long value1 by int value2 positions
// value1, value2 → result
    fun lshl(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // bitwise shift right of a long value1 by int value2 positions
// value1, value2 → result
    fun lshr(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a long value in a local variable #index
// value →
    fun lstore(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a long value in a local variable 0
// value →
    fun lstore_0(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a long value in a local variable 1
// value →
    fun lstore_1(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a long value in a local variable 2
// value →
    fun lstore_2(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store a long value in a local variable 3
// value →
    fun lstore_3(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // subtract two longs
// value1, value2 → result
    fun lsub(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // bitwise shift right of a long value1 by int value2 positions, unsigned
// value1, value2 → result
    fun lushr(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // bitwise XOR of two longs
// value1, value2 → result
    fun lxor(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // enter monitor for object ("grab the lock" – start of synchronized() section)
// objectref →
    fun monitorenter(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // exit monitor for object ("release the lock" – end of synchronized() section)
// objectref →
    fun monitorexit(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // create a new array of dimensions dimensions of type identified by class reference in constant pool index(indexbyte1 << 8 | indexbyte2); the sizes of each dimension is identified by count1, [count2, etc.]
// count1, [count2,...] → arrayref
    fun multianewarray(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // create new object of type identified by class reference in constant pool index (indexbyte1 << 8 | indexbyte2)
// → objectref
    fun new(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // create new array with count elements of primitive type identified by atype
// count → arrayref
    fun newarray(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // perform no operation
    // [No change]
    fun nop(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {

    }

    // discard the top value on the stack
// value →
    fun pop(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        frame.pop()
    }

    // discard the top two values on the stack (or one value, if it is a double or long)
// {value2, value1} →
    fun pop2(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        frame.pop()
        frame.pop()
    }

    // set field to value in an object objectref, where the field is identified by a field reference index in constant pool (indexbyte1 << 8 | indexbyte2)
// objectref, value →
    fun putfield(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // set static field to value in a class, where the field is identified by a field reference index in constant pool (indexbyte1 << 8 | indexbyte2)
// value →
    fun putstatic(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // continue execution from address taken from a local variable #index (the asymmetry with jsr is intentional)
// [No change]
    fun ret(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // return void from method
// → [empty]
    fun `return`(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        throw ReturnException(null, frame.method)
    }

    // load short from array
// arrayref, index → value
    fun saload(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // store short to array
// arrayref, index, value →
    fun sastore(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // push a short onto the stack as an integer value
// → value
    fun sipush(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // swaps two top words on the stack (note that value1 and value2 must not be double or long)
// value2, value1 → value1, value2
    fun swap(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // continue execution from an address in the table at offset index
// index →
    fun tableswitch(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }

    // execute opcode, where opcode is either iload, fload, aload, lload, dload, istore, fstore, astore, lstore, dstore, or ret, but assume the index is 16 bit; or execute iinc, where the indexis 16 bits and the constant to increment by is a signed 16 bit short
// [same as for corresponding instructions]
    fun wide(ctx: ExecutionContext, frame: BytecodeExecutionFrame) {
        TODO("Not implemented")
    }
}