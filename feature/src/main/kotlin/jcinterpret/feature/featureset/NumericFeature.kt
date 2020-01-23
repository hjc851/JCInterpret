package jcinterpret.testconsole.features.featureset

class NumericFeature (
    override val name: String,
    override var value: Number
): Feature<Number>(NumericFeatureDescriptor) {
    fun scale(factor: Double) {
        this.value = this.value.toDouble() * factor
    }
}

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