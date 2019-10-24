package jcinterpret.testconsole.pipeline.comparison

import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

data class ProjectModel (
    val root: Path,
    val entryPoints: List<Path>,
    val traces: Map<Path, Map<String, List<Path>>>
)

fun buildProjectModel(path: Path): ProjectModel {
    val eps = Files.list(path)
        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
        .use { it.toList() }

    val traces = eps.map { ep ->
        ep to Files.list(ep)
            .filter { Files.isRegularFile(it) && !Files.isHidden(it) }
            .use { it.toList() }
            .groupBy { it.fileName.toString().split("-")[0] }
    }.toMap()

    return ProjectModel(
        path,
        eps,
        traces
    )
}
