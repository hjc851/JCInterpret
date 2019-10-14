package jcinterpret.core.ctx.frame.interpreted

import jcinterpret.core.descriptors.qualifiedSignature
import jcinterpret.core.descriptors.signature
import jcinterpret.core.memory.stack.StackInt
import jcinterpret.core.memory.stack.StackValue
import jcinterpret.signature.ClassTypeSignature
import jcinterpret.signature.PrimitiveTypeSignature
import org.eclipse.jdt.core.dom.*

class ASTDecoder(val frame: InterpretedExecutionFrame): ASTVisitor() {

    //
    //  Helpers
    //

    fun add(stmt: Statement) = frame.instructions.push(decode_stmt(stmt))

    fun add(expr: Expression, store: Boolean = true) {
        if (!store) frame.instructions.push(pop)
        frame.instructions.push(decode_expr(expr))
    }

    fun push(instr: InterpretedInstruction) = frame.instructions.push(instr)

    //
    //  Statements
    //

    //  Blocks

    override fun visit(node: Block): Boolean {
        push(block_pop)
        node.statements()
            .reversed()
            .forEach { add(it as Statement) }
        push(block_push)

        return false
    }

    override fun visit(node: SynchronizedStatement): Boolean {
        TODO()
    }

    //  Variable

    override fun visit(node: VariableDeclarationStatement): Boolean {
        node.fragments().reversed().forEach { (it as ASTNode).accept(this) }
        return false
    }

    override fun visit(node: VariableDeclarationExpression): Boolean {
        node.fragments().reversed().forEach { (it as ASTNode).accept(this) }
        return false
    }

    override fun visit(node: SingleVariableDeclaration): Boolean {
        val type = node.resolveBinding().type.signature()

        if (node.initializer != null) {
            push(store(node.name.identifier, type))
            add(node.initializer)
        }

        push(allocate(node.name.identifier, type))

        return false
    }

    override fun visit(node: VariableDeclarationFragment): Boolean {
        val type = node.resolveBinding().type.signature()

        if (node.initializer != null) {
            push(store(node.name.identifier, type))
            add(node.initializer)
        }

        push(allocate(node.name.identifier, type))

        return false
    }

    //  Loops

    override fun visit(node: DoStatement): Boolean {
        TODO()
    }

    override fun visit(node: WhileStatement): Boolean {
        TODO()
    }

    override fun visit(node: ForStatement): Boolean {
        TODO()
    }

    override fun visit(node: EnhancedForStatement): Boolean {
        push(block_pop)
        push(iterate(node.parameter.name.identifier, node.parameter.resolveBinding().type.signature(), node.body))
        add(node.expression)
        push(block_push)

        return false
    }

    //  Conditional

    override fun visit(node: IfStatement): Boolean {
        push(conditional_if(node.thenStatement, node.elseStatement))
        add(node.expression)

        return false
    }

    override fun visit(node: SwitchStatement): Boolean {
        TODO()
    }

    override fun visit(node: SwitchCase): Boolean {
        TODO()
    }

    //  Try

    override fun visit(node: TryStatement): Boolean {

        node.finally?.accept(this)

        push(block_pop)
        push(excp_pop)

        add(node.body)

        node.resources().reversed().forEach { (it as ASTNode).accept(this) }

        val handles = node.catchClauses().map {
            (it as CatchClause)
            return@map ExceptionHandle (
                it.exception.name.identifier,
                it.exception.type.resolveBinding().signature() as ClassTypeSignature,
                it.body
            )
        }

        push(excp_push(handles))
        push(block_push)

        return false
    }

    //  Control

    override fun visit(node: LabeledStatement): Boolean {
        TODO()
    }

    override fun visit(node: ContinueStatement): Boolean {
        TODO()
    }

    override fun visit(node: BreakStatement): Boolean {
        TODO()
    }

    //  Linkage

    override fun visit(node: ReturnStatement): Boolean {
        if (node.expression != null) {
            push(return_value)
            add(node.expression)
        } else {
            push(return_void)
        }

        return false
    }

    override fun visit(node: ThrowStatement): Boolean {
        push(throw_exception)
        add(node.expression)

        return false
    }

    //  Expression

    override fun visit(node: ExpressionStatement): Boolean {
        var storeResult = node.parent is Expression

        if (node.expression is MethodInvocation)
            if ((node.expression as MethodInvocation).resolveMethodBinding().returnType.qualifiedName == "void")
                storeResult = true // i.e. don't pop a value off -> void doesn't return a value

        add(node.expression, storeResult)
        return false
    }

    //
    //  Expressions
    //

    override fun visit(node: ParenthesizedExpression): Boolean {
        add(node.expression)
        return false
    }

    //  Invocation

    override fun visit(node: ClassInstanceCreation): Boolean {
        if (node.anonymousClassDeclaration != null)
            throw IllegalArgumentException("Anonymous classes are not implemented")

        if (node.expression != null)
            throw IllegalArgumentException("Scoped constructor calls not implemented")

        push(invoke_special(node.resolveConstructorBinding().qualifiedSignature()))
        node.arguments().reversed().forEach { add(it as Expression) }

        push(dup)
        push(obj_allocate(node.resolveTypeBinding().signature() as ClassTypeSignature))

        return false
    }

    override fun visit(node: MethodInvocation): Boolean {
        val binding = node.resolveMethodBinding()
        val isStatic = Modifier.isStatic(binding.modifiers)

        if (binding.isVarargs)
            TODO()

        if (node.typeArguments().isNotEmpty())
            TODO()

        if (isStatic) {
            push(invoke_static(binding.qualifiedSignature()))
        } else {
            push(invoke_virtual(binding.qualifiedSignature()))
        }

        node.arguments()
            .reversed()
            .forEach { add(it as Expression) }

        if (!isStatic) {
            if (node.expression != null) {
                add(node.expression)
            } else {
                push(load("this"))

//                val thisType = frame.locals.typeOf("this").signature
//                val declClass = binding.declaringClass.signature()
//                if (thisType == declClass) {
//                    push(load("this"))
//                } else {
//                    TODO()
//                }
            }
        }

        return false
    }

    override fun visit(node: SuperMethodInvocation): Boolean {
        TODO()
    }

    override fun visit(node: ConstructorInvocation): Boolean {
        TODO()
    }

    override fun visit(node: SuperConstructorInvocation): Boolean {
        TODO()
    }

    override fun visit(node: ArrayCreation): Boolean {
        TODO()
    }

    override fun visit(node: ArrayInitializer): Boolean {
        TODO()
    }

    //  Conditional

    override fun visit(node: ConditionalExpression): Boolean {
        TODO()
    }

    //  Assignment & Operators

    var isAssignmentTarget = false
    fun assigning(handle: () -> Unit) {
        isAssignmentTarget = true
        handle()
        isAssignmentTarget = false
    }

    override fun visit(node: Assignment): Boolean {

        // Assignment are expressions - returns the lhs
        add(node.leftHandSide)

        // Put the store/put instruction in
        assigning {
            node.leftHandSide.accept(this)
        }

        when (node.operator) {
            Assignment.Operator.ASSIGN -> {
                add(node.rightHandSide)
            }

            Assignment.Operator.PLUS_ASSIGN -> {
                push(add)
                add(node.rightHandSide)
                add(node.leftHandSide)
            }

            Assignment.Operator.MINUS_ASSIGN -> {
                push(sub)
                add(node.rightHandSide)
                add(node.leftHandSide)
            }

            Assignment.Operator.TIMES_ASSIGN -> {
                push(mul)
                add(node.rightHandSide)
                add(node.leftHandSide)
            }

            Assignment.Operator.DIVIDE_ASSIGN -> {
                push(div)
                add(node.rightHandSide)
                add(node.leftHandSide)
            }

            Assignment.Operator.REMAINDER_ASSIGN -> {
                push(mod)
                add(node.rightHandSide)
                add(node.leftHandSide)
            }

            else -> TODO()
        }

        return false
    }

    override fun visit(node: PrefixExpression): Boolean {
        when (node.operator) {
            PrefixExpression.Operator.INCREMENT -> {
                add(node.operand)
                assigning { node.operand.accept(this) }
                push(add)
                push(push(StackInt(1)))
                add(node.operand)
            }

            PrefixExpression.Operator.DECREMENT -> {
                add(node.operand)
                assigning { node.operand.accept(this) }
                push(sub)
                push(push(StackInt(1)))
                add(node.operand)
            }

            PrefixExpression.Operator.PLUS -> {
                TODO()  // Converts to int
            }

            PrefixExpression.Operator.MINUS -> {
                TODO()
            }

            PrefixExpression.Operator.COMPLEMENT -> {
                TODO() // Bitwise complement
            }

            PrefixExpression.Operator.NOT -> {
                push(not)
                add(node.operand)
            }

            else -> throw IllegalArgumentException("Unknown prefix operator ${node.operator}")
        }

        return false
    }

    override fun visit(node: PostfixExpression): Boolean {
        assigning {
            node.operand.accept(this)
        }

        when (node.operator) {
            PostfixExpression.Operator.INCREMENT -> {
                push(add)
                push(push(StackInt(1)))
                add(node.operand)
                add(node.operand)
            }

            PostfixExpression.Operator.DECREMENT -> {
                push(sub)
                push(push(StackInt(1)))
                add(node.operand)
                add(node.operand)
            }

            else -> throw IllegalArgumentException("Unknown PostfixExpression.Operator ${node.operator}")
        }

        return false
    }

    override fun visit(node: InfixExpression): Boolean {
        val operator = when (node.operator) {
            InfixExpression.Operator.TIMES -> mul
            InfixExpression.Operator.DIVIDE -> div
            InfixExpression.Operator.REMAINDER -> mod
            InfixExpression.Operator.PLUS -> add
            InfixExpression.Operator.MINUS -> sub
            InfixExpression.Operator.LEFT_SHIFT -> shl
            InfixExpression.Operator.RIGHT_SHIFT_SIGNED -> shr
            InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED -> ushr
            InfixExpression.Operator.LESS -> less
            InfixExpression.Operator.GREATER -> greater
            InfixExpression.Operator.LESS_EQUALS -> lessequals
            InfixExpression.Operator.GREATER_EQUALS -> greaterequals
            InfixExpression.Operator.EQUALS -> equals
            InfixExpression.Operator.NOT_EQUALS -> notequals
            InfixExpression.Operator.XOR -> xor
            InfixExpression.Operator.OR -> or
            InfixExpression.Operator.AND -> and
            InfixExpression.Operator.CONDITIONAL_OR -> or
            InfixExpression.Operator.CONDITIONAL_AND -> and

            else -> throw IllegalArgumentException("Unknown InfixExpression.Operator")
        }

        val expressions = (listOf(node.leftOperand, node.rightOperand) + node.extendedOperands()).map { it as Expression }

        for (i in 0 until expressions.size - 1) {
            push(operator)
            add(expressions[i], true)
        }

        add(expressions.last(), true)

        return false
    }

    //  Accessors

    override fun visit(node: ArrayAccess): Boolean {
        TODO()
    }

    override fun visit(node: FieldAccess): Boolean {
        TODO()
    }

    override fun visit(node: SuperFieldAccess): Boolean {
        TODO()
    }

    override fun visit(node: SimpleName): Boolean {
        val binding = node.resolveBinding()
        val type = node.resolveTypeBinding()

        if (isAssignmentTarget) {

            if (binding is IVariableBinding) {
                if (binding.isParameter) {
                    push(store(node.identifier, type.signature()))
                } else if (binding.isField) {
                    if (Modifier.isStatic(binding.modifiers)) {
                        push(stat_put(binding.declaringClass.signature(), node.identifier, type.signature()))
                    } else {
                        val typeOfThis = frame.locals.typeOf("this").signature
                        val decClass = binding.declaringClass.signature()
                        if (typeOfThis == decClass) {
                            push(obj_put(node.identifier, type.signature()))
                            push(load("this"))
                        } else {
                            TODO()
                        }
                    }
                } else if (binding.isEnumConstant) {
                    TODO()
                } else /* Probably a local */ {
                    push(store(node.identifier, type.signature()))
                }
            } else {
                TODO()
            }

        } else {

            if (binding is IVariableBinding) {
                if (binding.isParameter) {
                    push(load(node.identifier, type.signature()))
                } else if (binding.isField) {
                    if (Modifier.isStatic(binding.modifiers)) {
                        push(stat_get(binding.declaringClass.signature(), node.identifier, type.signature()))
                    } else {
                        val typeOfThis = frame.locals.typeOf("this").signature
                        val decClass = binding.declaringClass.signature()
                        if (typeOfThis == decClass) {
                            push(obj_get(node.identifier, type.signature()))
                            push(load("this"))
                        } else {
                            TODO()
                        }
                    }
                } else if (binding.isEnumConstant) {
                    TODO()
                } else /* Probably a local */ {
                    push(load(node.identifier, type.signature()))
                }
            } else {
                TODO()
            }

        }

        return false
    }

    override fun visit(node: QualifiedName): Boolean {
        TODO()
    }

    //  Literals

    override fun visit(node: BooleanLiteral): Boolean {
        push(ldc_boolean(node.booleanValue()))
        return false
    }

    override fun visit(node: CharacterLiteral): Boolean {
        push(ldc_char(node.charValue()))
        return false
    }

    override fun visit(node: NullLiteral): Boolean {
        push(ldc_null)
        return false
    }

    override fun visit(node: NumberLiteral): Boolean {
        push(ldc_number(node.token, node.resolveTypeBinding().signature() as PrimitiveTypeSignature))
        return false
    }

    override fun visit(node: StringLiteral): Boolean {
        push(ldc_string(node.literalValue))
        return false
    }

    override fun visit(node: TypeLiteral): Boolean {
        push(ldc_type(node.type.resolveBinding().signature()))
        return false
    }

    //  Type Based

    override fun visit(node: ThisExpression): Boolean {
        if (node.qualifier != null)
            throw IllegalArgumentException("Scoped this not implemented")

        push(load("this"))
        return false
    }

    override fun visit(node: CastExpression): Boolean {
        push(cast(node.type.resolveBinding().signature()))
        add(node.expression)

        return false
    }

    override fun visit(node: InstanceofExpression): Boolean {
        push(instanceof(node.rightOperand.resolveBinding().signature()))
        add(node.leftOperand)

        return false
    }

    //  Functional

    override fun visit(node: LambdaExpression): Boolean {
        TODO()
    }

    override fun visit(node: CreationReference): Boolean {
        TODO()
    }

    override fun visit(node: ExpressionMethodReference): Boolean {
        TODO()
    }

    override fun visit(node: SuperMethodReference): Boolean {
        TODO()
    }

    override fun visit(node: TypeMethodReference): Boolean {
        TODO()
    }
}