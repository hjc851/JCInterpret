package jcinterpret.testconsole

import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.dom.CompilationUnit
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlin.streams.toList

fun main(args: Array<String>) {
    val path = Paths.get("/Users/haydencheers/Desktop/JS1/src")

    val dirs = Files.list(path)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }

    for (dir in dirs) {
        val files = Files.list(dir)
            .filter { !Files.isDirectory(it) && !Files.isHidden(it) }
            .filter { it.fileName.toString().endsWith(".java") }
            .use { it.toList() }

        for (file in files) {

            val parser = ASTParser.newParser(AST.JLS10)
            parser.setSource(String(Files.readAllBytes(file), Charset.defaultCharset()).toCharArray())

            val ast = try {
                parser.createAST(null) as CompilationUnit
            } catch (e: Exception) {
                continue
            }

            if (ast.`package` != null) {
                val packageName = ast.`package`.name.fullyQualifiedName
                    .replace(".", "/")

                val movePath = dir.resolve(packageName)
                    .resolve(file.fileName)
                Files.createDirectories(movePath)
                Files.move(file, movePath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}