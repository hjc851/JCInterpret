package jcinterpret.graph.execution

object NodeAttributeKeys {
    val ENTRYPOINT = "entrypoint"

    val UICLASS = "ui.class"
    val UILABEL = "ui.label"

    val TYPE = "type"
    val EDGETYPE = "edgetype"
    val NODETYPE = "nodetype"
    val OPERATOR = "operator"
    val CASTTYPE = "casttype"
    val METHODSIGNATURE = "methodsignature"
    val VALUE = "value"
    val STRING = "string"
    val REPRESENTING = "representing"

    val STATIC = "static"
    val LITERAL = "literal"
    val SYMBOLIC = "symbolic"
    val CONCRETE = "concrete"
    val SYNTHETIC = "synthetic"
    val STATICFIELD = "staticfield"
    val ENTRYPARAMETER = "entryparameter"

    val CLASS_VALUE = "value"
    val CLASS_OBJECT = "object"
    val CLASS_OPERATOR = "operator"
    val CLASS_ENTRYPOINT = "entrypoint"
    val CLASS_METHODCALL = "methodcall"

    val CLASS_SCOPE = "scope"
    val CLASS_SUPPLIES = "supplies"
    val CLASS_PARAMETER = "parameter"
    val CLASS_AGGREGATION = "aggregation"
    val CLASS_TRANSFORMATION = "transformation"
}

enum class NodeType {
    VALUE,
    OBJECT,
    OPERATOR,
    ENTRYPOINT,
    METHODCALL
}

enum class EdgeType {
    SCOPE,
    SUPPLIES,
    PARAMETER,
    AGGREGATION,
    TRANSFORMATION
}