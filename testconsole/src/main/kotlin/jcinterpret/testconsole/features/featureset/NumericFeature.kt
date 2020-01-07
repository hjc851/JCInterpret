package jcinterpret.testconsole.features.featureset

class NumericFeature (
    override val name: String,
    override val value: Number
): Feature<Number>(NumericFeatureDescriptor)

object NumericFeatureDescriptor: FeatureDescriptor<Number>() {
    override fun create(name: String, value: Number): Feature<Number> {
        return NumericFeature(name, value)
    }

    override fun register(value: Number) {}

    override fun descriptor(): String {
        return "numeric"
    }

    override fun defaultValue(): String {
        return "0"
    }
}