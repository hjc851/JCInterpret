package jcinterpret.signature

class SignatureParser(val str: String) {

    private var idx = 0

    //
    //  Parsing
    //

    fun parseQualifiedMethodSignature(): QualifiedMethodSignature {
        return QualifiedMethodSignature(parseClassTypeSignature(), parseMethodSignature())
    }

    fun parseMethodSignature(): MethodSignature {
        val name = StringBuilder()
        while (peek() != '(')
            name.append(consume())

        val typeSig = parseMethodTypeSignature()
        return MethodSignature(name.toString(), typeSig)
    }

    fun parseMethodTypeSignature(): MethodTypeSignature {
        val args = mutableListOf<TypeSignature>()

        consume('(')
        while (peek() != ')')
            args.add(parseTypeSignature())
        consume(')')

        val returnType = parseTypeSignature()
        return MethodTypeSignature(args.toTypedArray(), returnType)
    }

    fun parseTypeSignature(): TypeSignature {
        return when (peek()) {
            in arrayOf(
                'Z',
                'B',
                'C',
                'S',
                'I',
                'J',
                'F',
                'D',
                'V'
            ) -> parsePrimitiveTypeSignature()

            else -> parseReferenceTypeSignature()
        }
    }

    fun parseReferenceTypeSignature(): ReferenceTypeSignature {
        return when (peek()) {
            'L' -> parseClassTypeSignature()

            '[' -> parseArrayTypeSignature()

            else -> throw IllegalStateException("Cannot match ${peek()} to type signature")
        }
    }

    fun parsePrimitiveTypeSignature(): PrimitiveTypeSignature {
        val code = consume()

        return when (code) {

            'Z' -> PrimitiveTypeSignature.BOOLEAN
            'B' -> PrimitiveTypeSignature.BYTE
            'C' -> PrimitiveTypeSignature.CHAR
            'S' -> PrimitiveTypeSignature.SHORT
            'I' -> PrimitiveTypeSignature.INT
            'J' -> PrimitiveTypeSignature.LONG
            'F' -> PrimitiveTypeSignature.FLOAT
            'D' -> PrimitiveTypeSignature.DOUBLE
            'V' -> PrimitiveTypeSignature.VOID

            else -> throw IllegalStateException("Cannot match ${code} to primitive type signature")
        }
    }

    fun parseClassTypeSignature(): ClassTypeSignature {
        consume('L')
        val name = StringBuilder()
        while (peek() != ';') {
            name.append(consume())
        }
        consume(';')

        return ClassTypeSignature(name.toString())
    }

    fun parseArrayTypeSignature(): ArrayTypeSignature {
        consume('[')
        return ArrayTypeSignature(parseTypeSignature())
    }

    //
    //  Helpers
    //

    private fun peek(): Char = str[idx]
    private fun consume(): Char = str[idx++]

    private fun consume(c: Char): Char {
        if (peek() != c)
            throw IllegalStateException("Expecting $c, found ${peek()}")

        return consume()
    }

    private fun consumeIf(c: Char): Boolean {
        if (peek() == c) {
            consume()
            return true
        }

        return false
    }
}