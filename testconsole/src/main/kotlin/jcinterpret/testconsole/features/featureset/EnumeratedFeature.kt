package jcinterpret.testconsole.features.featureset

import java.io.Serializable

class EnumeratedFeature<T: Serializable> (
    override val name: String,
    override val value: T,
    descriptor: EnumeratedFeatureDescriptor<T>
): Feature<T>(descriptor)

abstract class EnumeratedFeatureDescriptor<T: Serializable>: FeatureDescriptor<T>() {
    abstract val values: Set<T>

    override fun create(name: String, value: T): Feature<T> {
        return EnumeratedFeature(name, value, this)
    }

    override fun descriptor(): String {
        return "{" + values.joinToString(",") + "}"
    }

    override fun defaultValue(): String {
        return "?"
    }

    class Immutable<T: Serializable> (
        override val values: Set<T>
    ): EnumeratedFeatureDescriptor<T>() {
        override fun register(value: T) {
            if (!values.contains(value)) throw IllegalFeatureValueException(
                this,
                value
            )
        }

        class IllegalFeatureValueException(val descriptor: EnumeratedFeatureDescriptor<*>, val value: Any): Exception()
    }

    class Mutable<T: Serializable> (
        override val values: MutableSet<T> = mutableSetOf()
    ): EnumeratedFeatureDescriptor<T>() {
        override fun register(value: T) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}



