package jcinterpret.parser

import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.dom.CompilationUnit
import org.eclipse.jdt.core.dom.FileASTRequestor
import java.nio.file.Path

object Parser {
    val fileEncoding = "UTF-8"

    fun parse(sources: List<Path>, libraries: List<Path>, sourceDirectories: List<Path>): List<CompilationUnit> {
        val parser = ASTParser.newParser(AST.JLS10)
        parser.setCompilerOptions(mutableMapOf(JavaCore.COMPILER_SOURCE to "1.8"))
        parser.setKind(ASTParser.K_COMPILATION_UNIT)
        parser.setResolveBindings(true)
        parser.setBindingsRecovery(true)
        parser.setEnvironment(
                libraries.map { it.toAbsolutePath().toString() }.toTypedArray(),
                sourceDirectories.map { it.toAbsolutePath().toString() }.toTypedArray(),
                Array(sourceDirectories.size) { fileEncoding },
                false
        )

        val requestor = AstRequestor()
        parser.createASTs(sources.map { it.toAbsolutePath().toString() }.toTypedArray(), Array(sources.size) { fileEncoding }, arrayOf(), requestor, null)

        return requestor.compilationUnits
    }

    private class AstRequestor : FileASTRequestor() {
        private val _compilationUnits = mutableListOf<CompilationUnit>()
        val compilationUnits get() = _compilationUnits

        override fun acceptAST(sourceFilePath: String, ast: CompilationUnit) {
            _compilationUnits.add(ast)
        }
    }
}