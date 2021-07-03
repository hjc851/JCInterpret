package jcinterpret.testconsole.JA3.comparator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jcinterpret.testconsole.pipeline.comparison.ProcessedProjectComparator
import jcinterpret.testconsole.utils.ProjectModelBuilder
import java.nio.file.Files
import java.nio.file.Paths

object NoiseFilteredSingleProjectComparator {

    val mapper = ObjectMapper().registerModule(KotlinModule())

    @JvmStatic
    fun main(args: Array<String>) {
        val lhs = Paths.get(args[0])
        val rhs = Paths.get(args[1])

        val output = Paths.get(args[2])

        val lproj = ProjectModelBuilder.build(lhs)
        val rproj = ProjectModelBuilder.build(rhs)

        ProcessedProjectComparator.NOISE_COMPONENT_THRESHOLD = 2
        val result = ProcessedProjectComparator.compareExecutionGraphsRemovingNoiseWithSubsetCoefficient(lproj, rproj)
        val resultBean =
            SingleComparisonResult(
                result.l.projectId,
                result.r.projectId,
                result.lsim,
                result.rsim
            )

        Files.newBufferedWriter(output).use { writer ->
            mapper.writeValue(writer, resultBean)
        }

        System.exit(0)
    }

    data class SingleComparisonResult (
        val lhs: String,
        val rhs: String,
        val lsim: Double,
        val rsim: Double
    )
}