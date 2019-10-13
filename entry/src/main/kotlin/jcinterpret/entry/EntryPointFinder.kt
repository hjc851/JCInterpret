package jcinterpret.entry

import org.eclipse.jdt.core.dom.*
import kotlin.streams.toList

object EntryPointFinder {

    fun find(compilationUnits: List<CompilationUnit>, advice: List<String>): List<EntryPoint> {
        val entries = compilationUnits.parallelStream()
            .flatMap { cu ->
                val visitor = EntryPointVisitor(advice)
                cu.accept(visitor)
                return@flatMap visitor.entryPoints.stream()
            }
            .toList()
        return entries
    }

    fun find(compilationUnits: List<CompilationUnit>): List<EntryPoint> {
        val entries = compilationUnits.parallelStream()
            .flatMap { cu ->
                val visitor = EntryPointVisitor(emptyList())
                cu.accept(visitor)
                return@flatMap visitor.entryPoints.stream()
            }
            .toList()
        return entries
    }
}

class EntryPointVisitor(val advice: List<String>) : ASTVisitor() {
    private val _entryPoints = mutableListOf<EntryPoint>()
    val entryPoints: List<EntryPoint> get() = _entryPoints

    override fun visit(node: MethodDeclaration): Boolean {
        val key = node.resolveBinding().key

        if (advice.contains(key))
            _entryPoints.add(EntryPoint(node.resolveBinding()))
        else if (node.isMain() || node.isServlet() || node.isCallback() || node.isAndroid() || node.isJaxEntryPoint())
            _entryPoints.add(EntryPoint(node.resolveBinding()))

        return true
    }

//    override fun visit(node: LambdaExpression): Boolean {
//        if (node.isCallback() || node.isAndroid())
//            _entryPoints.add(LambdaEntryPoint(node))
//
//        return true
//    }

    private fun MethodDeclaration.isMain(): Boolean {
        val binding = this.resolveBinding()

        if (body == null)
            return false

        if (!Modifier.isStatic(modifiers))
            return false

        if (!Modifier.isPublic(modifiers))
            return false

        if (binding.returnType.qualifiedName != "void")
            return false

        if (binding.parameterTypes.size != 1)
            return false

        if (!binding.parameterTypes.first().isArray)
            return false

        if (binding.parameterTypes.first().typeDeclaration.componentType.qualifiedName != "java.lang.String")
            return false

        return true
    }

    private val servletMethodNames = listOf("doGet", "doPost", "doPut", "doDelete")
    private val httpServlet = ("javax.servlet.http.HttpServlet")
    private val httpServletRequest = ("javax.servlet.http.HttpServletRequest")
    private val httpServletResponse = ("javax.servlet.http.HttpServletResponse")

    private fun MethodDeclaration.isServlet(): Boolean {
        val binding = resolveBinding()

        if (body == null)
            return false

        if (Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers))
            return false

        if (!servletMethodNames.contains(binding.name))
            return false

        if (binding.returnType.name != "void")
            return false

        if (binding.declaringClass.superclass.qualifiedName != httpServlet)
            return false

        if (binding.parameterTypes.size != 2)
            return false

        if (binding.parameterTypes[0].qualifiedName != httpServletRequest)
            return false

        if (binding.parameterTypes[1].qualifiedName != httpServletResponse)
            return false

        return true
    }

    private val eventObject = "java.util.EventObject"

    private fun MethodDeclaration.isCallback(): Boolean {
        val binding = resolveBinding()

        if (body == null)
            return false

        if (Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers))
            return false

        if (binding.returnType.name != "void")
            return false

        if (binding.parameterTypes.size != 1)
            return false

        if (!binding.parameterTypes.first().implementsOrExtends(eventObject))
            return false

        return true
    }

    private val androidView = "android.view.View"

    private fun MethodDeclaration.isAndroid(): Boolean {
        val binding = resolveBinding()

        if (body == null)
            return false

        if (Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers))
            return false

        if (binding.returnType.name != "void")
            return false

        if (binding.parameterTypes.size != 1)
            return false

        if (!binding.parameterTypes.first().implementsOrExtends(androidView))
            return false

        return true
    }

    private val jaxPath = "javax.ws.rs.Path"
    private val jaxGet = "javax.ws.rs.GET"
    private val jaxPost = "javax.ws.rs.POST"
    private val jaxPut = "javax.ws.rs.PUT"
    private val jaxUpdate = "javax.ws.rs.UPDATE"
    private val jaxDelete = "javax.ws.rs.DELETE"

    private val jaxWebmethod = "javax.jws.WebMethod"
    private val jaxWebService = "javax.jws.WebService"

    private val springRequest = "org.springframework.web.bind.annotation.RequestMapping"

    private val annotations = listOf(jaxPath, jaxGet, jaxPost, jaxPost, jaxUpdate, jaxDelete, jaxWebService, jaxWebService, springRequest)

    // Type or method has a @Path and is public
    private fun MethodDeclaration.isJaxEntryPoint(): Boolean {
        val binding = resolveBinding()
        val declaringType = binding.declaringClass

        if (binding.isConstructor)
            return false

        if (Modifier.isPrivate(modifiers))
            return false

        if (binding.annotations.firstOrNull { annotations.contains(it.annotationType.qualifiedName) } != null)
            return true

        if (declaringType.annotations.firstOrNull { annotations.contains(it.annotationType.qualifiedName) } != null)
            return true

        return false
    }

    private fun LambdaExpression.isCallback(): Boolean {
        val binding = resolveMethodBinding()

        if (body == null)
            return false

        if (binding.returnType.name != "void")
            return false

        if (binding.parameterTypes.size != 1)
            return false

        if (!binding.parameterTypes.first().implementsOrExtends(eventObject))
            return false

        return true
    }

    private fun LambdaExpression.isAndroid(): Boolean {
        val binding = resolveMethodBinding()

        if (body == null)
            return false

        if (binding.returnType.name != "void")
            return false

        if (binding.parameterTypes.size != 1)
            return false

        if (!binding.parameterTypes.first().implementsOrExtends(androidView))
            return false

        return true
    }
}

fun ITypeBinding.implementsOrExtends(qualifiedName: String): Boolean {
    if (this.qualifiedName == qualifiedName)
        return true

    if (this.superclass != null) {
        val superExtendsOrImplements = this.superclass.implementsOrExtends(qualifiedName)
        if (superExtendsOrImplements)
            return true
    }

    for (implements in this.interfaces) {
        val interfaceImplements = implements.implementsOrExtends(qualifiedName)
        if (interfaceImplements)
            return true
    }

    return false
}
