package jcinterpret.testconsole.features.featureset

import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

class FeatureSet: AutoCloseable {

    private val featureSetIds = mutableSetOf<String>()
    private val featureSets = mutableMapOf<String, ProjectFeatureSet>()
    private val descriptors = mutableMapOf<String, FeatureDescriptor<*>>()

    private val tempDir = Files.createTempDirectory("FEATURESET")

    fun getFeatureSetIds(): Set<String> = featureSetIds

    fun getFeatureNames(): Set<String> {
        return descriptors.keys
    }

    fun getDescriptor(name: String): FeatureDescriptor<*> {
        return descriptors[name]!!
    }

    @Synchronized
    fun getFeatureSet(id: String): ProjectFeatureSet {
        if (featureSetIds.contains(id) && Files.exists(tempDir.resolve("$id.ser"))) {
            loadFeatureSet(id)
            return featureSets[id]!!
        }

        if (!featureSets.containsKey(id)) {
            val fs = ProjectFeatureSet().apply { this.fs = this@FeatureSet }
            featureSets[id] = fs
            featureSetIds.add(id)
        }

        return featureSets[id]!!
    }

    @Synchronized
    fun registerDescriptor(name: String, descriptor: FeatureDescriptor<*>) {
        if (descriptors.containsKey(name))
            if (descriptors[name] != descriptor)
                throw FeatureSetCollisionException(name, descriptors[name]!!, descriptor)

        descriptors[name] = descriptor
    }

    fun cacheFeatureSet(id: String) {
        val pfs = featureSets.remove(id)!!

        val file = tempDir.resolve("$id.ser")
        Files.newOutputStream(file).use {
            ObjectOutputStream(it).writeObject(pfs)
        }
    }

    val readLock = Any()
    private fun loadFeatureSet(id: String) {
        synchronized(readLock) {
            if (featureSets.containsKey(id)) return@synchronized featureSets[id]!!

            val file = tempDir.resolve("$id.ser")
            val pfs = Files.newInputStream(file).use {
                ObjectInputStream(it).readObject() as ProjectFeatureSet
            }
            pfs.fs = this

            val field = Feature::class.java.getDeclaredField("descriptor")
            field.isAccessible = true

            for ((key, feature) in pfs.features) {
                val desc = descriptors[key]!!
                field.set(feature, desc)
            }

            featureSets[id] = pfs
        }
    }

    override fun close() {
        Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .forEachOrdered(Files::delete)
    }

    class ProjectFeatureSet: Serializable {
        @Transient
        internal lateinit var fs: FeatureSet
        internal val features: MutableMap<String, Feature<*>> = ConcurrentHashMap()

        @Synchronized
        fun add(feature: Feature<*>) {
            if (features.containsKey(feature.name))
                throw FeatureNameCollisionException(feature.name, features[feature.name]!!, feature)
            fs.registerDescriptor(feature.name, feature.descriptor)
            features[feature.name] = feature
        }

        fun addAll(features: List<Feature<*>>) {
            for (feature in features) add(feature)
        }

        fun features(): Map<String, Feature<*>> {
            return features
        }
    }
}