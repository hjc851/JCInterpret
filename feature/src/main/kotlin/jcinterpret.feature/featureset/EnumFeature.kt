package jcinterpret.testconsole.features.featureset

class EnumFeature<T: Enum<T>> (
    override val name: String,
    override val value: T,
    descriptor: FeatureDescriptor<T>
): Feature<T>(descriptor)

class EnumFeatureDescriptor<T: Enum<T>> (
    private val type: Class<T>
): FeatureDescriptor<T>() {
    override fun create(name: String, value: T): Feature<T> {
        return EnumFeature(name, value, this)
    }

    override fun register(value: T) {}

    override fun descriptor(): String {
        return "{${type.enumConstants.joinToString(",")}}"
    }

    override fun defaultValue(): String {
        return "?"
    }
}