package jcinterpret.testconsole.features.featureset

class StringFeature (
    override val name: String,
    override val value: String
): Feature<String>(StringFeatureDescriptor)

object StringFeatureDescriptor: FeatureDescriptor<String>() {
    override fun create(name: String, value: String): Feature<String> {
        return StringFeature(name, value)
    }

    override fun register(value: String) {}

    override fun descriptor(): String {
        return "string"
    }

    override fun defaultValue(): String {
        return ""
    }
}