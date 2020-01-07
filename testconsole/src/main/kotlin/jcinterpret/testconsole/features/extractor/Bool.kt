package jcinterpret.testconsole.features.extractor

import jcinterpret.testconsole.features.featureset.EnumFeatureDescriptor
import java.beans.FeatureDescriptor

enum class Bool {
    TRUE,
    FALSE;

    companion object {
        val descriptor = EnumFeatureDescriptor(Bool::class.java)
    }
}