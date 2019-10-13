package jcinterpret.config.document

data class ConfigDocument (
    val title: String,
    val projectsRoot: String,

    val globalLibraries: List<String>,
    val projectLibraries: Map<String, List<String>>?,

    val threshold: Double
)