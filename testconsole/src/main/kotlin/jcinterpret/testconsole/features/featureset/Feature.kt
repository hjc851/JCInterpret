package jcinterpret.testconsole.features.featureset

import java.io.Serializable

abstract class Feature<T: Serializable> (
    @Transient var descriptor: FeatureDescriptor<T>
): Serializable {
    abstract val name: String
    abstract val value: T

    fun rename(name: String): Feature<*> {
        return descriptor.allocate(name, value)
    }

    override fun toString(): String {
        return "Feature(name='$name', value=$value)"
    }


}

abstract class FeatureDescriptor<T: Serializable> {
    protected abstract fun create(name: String, value: T): Feature<T>

    abstract fun register(value: T)
    fun allocate(name: String, value: T): Feature<T> {
        register(value)
        return create(name, value)
    }

    abstract fun descriptor(): String
    abstract fun defaultValue(): String
}