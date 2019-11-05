package jcinterpret.testconsole.utils

import jcinterpret.testconsole.pipeline.GraphManifest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

data class ProjectModel (
    val projectId: String,
    val rootPath: Path,
    val entryPoints: Set<String>,
    val manifests: Map<String, Path>,
    val entryPointTraces: Map<String, List<EntryPointTrace>>,
    val totalTraceCount: Int
)

data class EntryPointTrace (
    val projectId: String,
    val entryPoint: String,
    val traceId: String,

    val executionGraphPath: Path,
    val taintGraphPath: Path,
    val secondaryConcernGraphPath: Path,
    val heapPath: Path,
    val assertionsPath: Path
)

object ProjectModelBuilder {
    fun build(path: Path): ProjectModel {
        val projectId = path.fileName.toString()

        // Identify entry point paths
        val entryPointPaths = Files.list(path)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }

        val traces = entryPointPaths.map { ep ->
            val epid = ep.fileName.toString()

            return@map epid to Files.list(ep)
                .filter { Files.isRegularFile(it) && !Files.isHidden(it) }
                .use { it.toList() }
                .filterNot { it.fileName.toString() == "manifest.ser" }
                .groupBy { it.fileName.toString().split("-")[0] }
                .map { (key, files) ->
                    val executionGraphPath = files.first { it.fileName.toString() == "$key-execgraph.ser" }
                    val taintGraphPath = files.first { it.fileName.toString() == "$key-taint.ser" }
                    val secondaryConcernGraphPath = files.first { it.fileName.toString() == "$key-scs.ser" }
                    val heapPath = files.first { it.fileName.toString() == "$key-heap.ser" }
                    val assertionsPath = files.first { it.fileName.toString() == "$key-assertions.ser" }

                    EntryPointTrace (
                        projectId, epid, key,
                        executionGraphPath,
                        taintGraphPath,
                        secondaryConcernGraphPath,
                        heapPath,
                        assertionsPath
                    )
                }
        }.toMap()

        val manifests = entryPointPaths.map { it.fileName.toString() to it.resolve("manifest.ser") }.toMap()
        val totalTraces = traces.values.map { it.size }.sum()

        return ProjectModel(
            projectId,
            path,
            traces.keys,
            manifests,
            traces,
            totalTraces
        )
    }
}

//data class ProjectModel (
//    val root: Path,
//    val entryPoints: List<Path>,
//    val traces: Map<Path, Map<String, List<Path>>>
//)

//fun buildProjectModel(path: Path): ProjectModel {
//    val eps = Files.list(path)
//        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
//        .use { it.toList() }
//
//    val traces = eps.map { ep ->
//        ep to Files.list(ep)
//            .filter { Files.isRegularFile(it) && !Files.isHidden(it) }
//            .use { it.toList() }
//            .groupBy { it.fileName.toString().split("-")[0] }
//    }.toMap()
//
//    return ProjectModel(
//        path,
//        eps,
//        traces
//    )
//}
