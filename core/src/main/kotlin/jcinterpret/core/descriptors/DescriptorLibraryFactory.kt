package jcinterpret.core.descriptors

import org.eclipse.jdt.core.dom.CompilationUnit
import java.nio.file.Path

object DescriptorLibraryFactory {
    fun build (
        compilationUnits: List<CompilationUnit>,
        libs: List<Path>
    ): DescriptorLibrary {
        return DescriptorLibrary (
            listOf(
                CompilationUnitDescriptorResolver(compilationUnits),
                ClassFileDescriptorResolver(libs)
            )
        )
    }
}