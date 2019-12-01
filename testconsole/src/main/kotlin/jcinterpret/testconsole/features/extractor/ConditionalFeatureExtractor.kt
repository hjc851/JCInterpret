package jcinterpret.testconsole.features.extractor

import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.document.DocumentUtils
import jcinterpret.graph.condition.ConditionalGraphBuilder
import jcinterpret.graph.condition.RootBranchGraphNode
import jcinterpret.testconsole.features.Feature
import jcinterpret.testconsole.features.FeatureType
import jcinterpret.testconsole.features.NumericFeature
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

object ConditionalFeatureExtractor {
    fun extract(root: Path, outRoot: Path) {

        // List the projects
        val projects = Files.list(root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }
            .sortedBy { it.fileName.toString() }

        // Iterate through the projects + process
        projects.parallelStream()
            .forEach { project ->
                val id = project.fileName.toString()

                // Out root for this project
                val projOut = outRoot.resolve(id)
                Files.createDirectory(projOut)

                // Trace sets for this project
                val traceSetFiles = Files.list(project)
                    .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser")}
                    .use { it.toList() }

                // Create the branching graphs, extract features + save to disk
                traceSetFiles.parallelStream()
                    .forEach { traceSetPath ->
                        val eptraces = DocumentUtils.readObject(traceSetPath, EntryPointExecutionTraces::class)
                        val epsig = eptraces.entryPoint.toString().replace("/", ".")
                            .replace("\"", ".")

                        val records = eptraces.executionTraces.map { it.records }
                        val cgraph = ConditionalGraphBuilder.build(records)
                        val cfeatures =
                            graphFeatures(cgraph)

                        val outfile = projOut.resolve("$epsig.ser")
                        DocumentUtils.writeObject(outfile, HashMap(cfeatures))
                    }
            }
    }

    fun graphFeatures(root: RootBranchGraphNode): Map<FeatureType, List<Feature<*>>> {
        val features = mutableMapOf<FeatureType, List<Feature<*>>>()

        features[FeatureType.CONDITIONAL_GRAPH] = listOf(
            NumericFeature("CONDITION_GRAPH_TREE_HEIGHT", root.height()),
            NumericFeature("CONDITION_GRAPH_TREE_NODE_COUNT", root.size()),
            NumericFeature(
                "CONDITION_GRAPH_TREE_TERMINAL_COUNT",
                root.terminals().count()
            ),
            NumericFeature(
                "CONDITION_GRAPH_TREE_INTERNAL_COUNT",
                root.internals().count()
            ),
            NumericFeature(
                "CONDITION_GRAPH_CONDITION_COUNT",
                root.uniqueConditions().count()
            )
        )

        return features
    }
}