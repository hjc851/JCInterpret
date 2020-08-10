package jcinterpret.testconsole.utils

import java.io.IOException
import java.nio.file.*
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

    fun copyDir(from: Path, to: Path) {
        if (!Files.isDirectory(from)) throw IllegalArgumentException("File $from does not exist")

        Files.walk(from)
            .forEachOrdered { file ->
                Files.copy(file, to.resolve(from.relativize(file)), StandardCopyOption.REPLACE_EXISTING)
            }
    }

    fun deleteDirectory(dir: Path) {
        Files.walk(dir)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::delete)
    }
}