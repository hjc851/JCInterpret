package jcinterpret.document

data class ConfigDocument (
    val title: String,
    val projectsRoot: String,
    val output: String,

    val globalLibraries: List<String>,
    val projectLibraries: Map<String, Array<String>>?,

    val entryPoints: Map<String, Array<String>>?,

    val loggingEnabled: Boolean,
    val maxRecursiveCalls: Int,
    val maxLoopExecutions: Int
)