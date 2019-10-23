package jcinterpret.testconsole.utils

import jcinterpret.core.descriptors.DescriptorLibrary
import jcinterpret.core.source.SourceLibrary
import jcinterpret.entry.EntryPoint
import org.eclipse.jdt.core.dom.CompilationUnit
import java.nio.file.Path

data class Project (
    val id: String,
    val path: Path,
    val compilationUnits: List<CompilationUnit>,
    val descriptorLibrary: DescriptorLibrary,
    val sourceLibrary: SourceLibrary,
    val entries: List<EntryPoint>
)