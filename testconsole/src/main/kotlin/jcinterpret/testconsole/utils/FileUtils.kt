package jcinterpret.testconsole.utils

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

object FileUtils {
    fun listFiles(path: Path, extension: String): List<Path> {
        val visitor = GenericFileVisitor(extension)
        Files.walkFileTree(path, visitor)
        return visitor.files
    }

    fun listDirectories(path: Path): List<Path> {
        val visitor = DirectoryVisitor()
        Files.walkFileTree(path, visitor)
        return visitor.directories
    }

    private class GenericFileVisitor(val extension: String) : FileVisitor<Path> {
        private val _files = mutableListOf<Path>()
        val files: List<Path> get() = _files

        override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult = FileVisitResult.CONTINUE
        override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult = FileVisitResult.CONTINUE
        override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult = FileVisitResult.CONTINUE

        override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {

            if (!file.fileName.toString().startsWith("._") && file.fileName.toString().endsWith(extension))
                _files.add(file)

            return FileVisitResult.CONTINUE
        }
    }

    private class DirectoryVisitor : FileVisitor<Path> {
        val directories = mutableListOf<Path>()

        override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult = FileVisitResult.CONTINUE
        override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult = FileVisitResult.CONTINUE
        override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult = FileVisitResult.CONTINUE

        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes?): FileVisitResult {
            directories.add(dir)
            return FileVisitResult.CONTINUE
        }
    }
}