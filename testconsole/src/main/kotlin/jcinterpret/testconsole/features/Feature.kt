package jcinterpret.testconsole.features

import java.io.Serializable
import java.util.*

abstract class Feature<T>(
    val name: String,
    val value: T
): Serializable

class NumericFeature(name: String, value: Number): Feature<Number>(name, value)
class StringFeature(name: String, value: String): Feature<String>(name, value)
class DateFeature(name: String, value: Date): Feature<Date>(name, value)

class EnumeratedFeature<T: Enum<T>>(name: String, value: Enum<T>): Feature<Enum<T>>(name, value)
class RelationalFeature(name: String, value: Array<Feature<*>>): Feature<Array<Feature<*>>>(name, value)