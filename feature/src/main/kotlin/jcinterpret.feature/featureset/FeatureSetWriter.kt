package jcinterpret.feature.featureset

import jcinterpret.testconsole.features.featureset.FeatureSet
import java.io.BufferedWriter

object FeatureSetWriter {
    fun write(out: BufferedWriter, fs: FeatureSet, clsProducer: ((String) -> String)? = null) {
        if (fs.getFeatureNames().contains("cls")) throw IllegalStateException("Feature set cannot contain cls attribute before writing")

        TODO()
    }
}