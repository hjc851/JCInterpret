package jcinterpret.testconsole.features.extractor

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

        projects.forEachIndexed { index, project ->
            val id = project.fileName.toString()
            val pfs = fs.getFeatureSet(id)
            println("\t$id - ${index + 1} of ${projects.count()}")

            val traceSetFiles = Files.list(project)
                .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser") }
                .use { it.toList() }

            val graphCount = NumericFeature("CONDITIONALGRAPH_COUNT", traceSetFiles.count())
            pfs.add(graphCount)

            val graphStats = traceSetFiles.parallelStream()
                .map { traceSetPath ->
                    val eptraces = DocumentUtils.readObject(traceSetPath, EntryPointExecutionTraces::class)
                    val epsig = eptraces.entryPoint.toString().replace("/", ".")
                        .replace("\"", ".")

                    val records = eptraces.executionTraces.map { it.records }
                    val cgraph = ConditionalGraphBuilder.build(records)

                    return@map featuresForGraph(cgraph)
                }.toList()

            val groupedFeatures = graphStats.flatten()
                .groupBy { it.name }

            for ((key, features) in groupedFeatures) {
                val values = features.map { it.value.toDouble() }
                val min = values.min() ?: 0.0
                val max = values.max() ?: 0.0
                val avg = values.average()

                pfs.add(NumericFeature(key + "_MIN", min))
                pfs.add(NumericFeature(key + "_MAX", max))
                pfs.add(NumericFeature(key + "_AVG", avg))
            }

            fs.cacheFeatureSet(id)
            System.gc()
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