package jcinterpret.feature.extractor

import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.document.DocumentUtils
import jcinterpret.graph.condition.ConditionalGraphBuilder
import jcinterpret.graph.condition.RootBranchGraphNode
import jcinterpret.testconsole.features.featureset.FeatureSet
import jcinterpret.testconsole.features.featureset.NumericFeature
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

object ConditionalFeatureExtractor {
    fun extract(root: Path, fs: FeatureSet) {
        // List the projects
        val projects = Files.list(root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }
            .sortedBy { it.fileName.toString() }

        projects
            .parallelStream()
            .forEach { project ->
            val id = project.fileName.toString()
            println("\t$id")

            val traceSetFiles = Files.list(project)
                .filter { !Files.isDirectory(it) && !Files.isHidden(it) }
                .use { it.toList() }

            if (traceSetFiles.isEmpty())
                Unit

            for (traceSetPath in traceSetFiles) {
                val eptraces = DocumentUtils.readObject(traceSetPath, EntryPointExecutionTraces::class)
                val epsig = eptraces.entryPoint.toString().replace("/", ".")
                    .replace("\"", ".")

                val records = eptraces.executionTraces.map { it.records }
                val cgraph = ConditionalGraphBuilder.build(records)

                val features = featuresForGraph(cgraph)

                val epid = "$id-$epsig"
                val epfs = fs.getFeatureSet(epid)

                features.forEach { epfs.add(it) }

                fs.cacheFeatureSet(epid)
                System.gc()
            }
        }
    }

    private fun featuresForGraph(root: RootBranchGraphNode): List<NumericFeature> {
        return listOf(
            NumericFeature("CONDITION_GRAPH_TREE_HEIGHT", root.height()),
            NumericFeature("CONDITION_GRAPH_TREE_NODE_COUNT", root.size()),
            NumericFeature("CONDITION_GRAPH_TREE_TERMINAL_COUNT", root.terminals().count()),
            NumericFeature("CONDITION_GRAPH_TREE_INTERNAL_COUNT", root.internals().count()),
            NumericFeature("CONDITION_GRAPH_CONDITION_COUNT", root.uniqueConditions().count())
        )
    }
}