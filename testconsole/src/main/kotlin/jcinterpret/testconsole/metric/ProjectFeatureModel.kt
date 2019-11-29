package jcinterpret.testconsole.metric

import java.io.Serializable

data class ProjectFeatureModel (
    val title: String,
    val conditionalFeatures: Map<String, Map<String, Map<FeatureType, List<Feature<*>>>>>,
    val traceFeatures: Map<String, Map<String, List<Map<FeatureType, List<Feature<*>>>>>>,
    val graphFeatures: Map<String, Map<String, List<Map<FeatureType, List<Feature<*>>>>>>
): Serializable