package jcinterpret.testconsole.metric

enum class FeatureType {
    //  Trace Records
    HEAP,
    EXEC_ENV_INTERACTION,
    ENTRYPOINT,
    FIELDS,
    ARRAYS,
    STACK_TRANSFORMS,
    STRING_TRANSFORM,
    ASSERTIONS,
    HALTS,
    UNCAUGHT_EXCEPTIONS,

    //  Conditional Graph
    CONDITIONAL_GRAPH,

    // Graph
    EXECUTION_GRAPH,
    TAINT_GRAPH,
    CONCERN_GRAPH
}