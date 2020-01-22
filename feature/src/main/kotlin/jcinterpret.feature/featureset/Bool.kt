package jcinterpret.testconsole.features.featureset

enum class Bool {
    TRUE,
    FALSE;

    companion object {
        val descriptor = EnumFeatureDescriptor(Bool::class.java)
    }
}