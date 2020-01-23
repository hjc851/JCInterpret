package jcinterpret.feature.featureset

import jcinterpret.feature.sanitise
import jcinterpret.testconsole.features.featureset.FeatureSet
import java.io.BufferedWriter
import java.io.PrintWriter

object FeatureSetWriter {
    fun write(out: BufferedWriter, fs: FeatureSet, clsProducer: ((String) -> String)) {
        if (fs.getFeatureNames().contains("id")) throw IllegalStateException("Feature set cannot contain id attribute")
        if (fs.getFeatureNames().contains("cls")) throw IllegalStateException("Feature set cannot contain cls attribute")

        val ids = fs.getFeatureSetIds()
        val classes = ids.map { it to clsProducer(it) }.toMap()

        val featureNames = fs.getFeatureNames().toList()
        val fout = PrintWriter(out)

        println("\tWriting headers")
        fout.println("@RELATION plagiarismmetrics")
        fout.println()
        fout.printf("@ATTRIBUTE id {%s}\n", ids.joinToString(","))
        featureNames.forEach { name ->
            val desc = fs.getDescriptor(name)
            fout.println("@ATTRIBUTE ${name.sanitise()} ${desc.descriptor()}")
        }
        fout.printf("@ATTRIBUTE cls {%s}\n", classes.values.distinct().joinToString(","))
        fout.println()

        println("\tWriting data")
        fout.println("@DATA")
        ids.forEachIndexed { index, id ->
            println("\t\t$id - ${index+1} of ${fs.getFeatureSetIds().count()}")
            val pfs = fs.getFeatureSet(id)
            val features = pfs.features()

            fout.append("{0 $id,")

            for (i in 0 until featureNames.count()) {
                val fname = featureNames[i]

                if (pfs.features().containsKey(fname)) {
                    val feature = pfs.features()[fname]!!
                    fout.append("${i+1} ${feature.value},")
                }
            }

            fout.append("${featureNames.size+1} ${classes.getValue(id)}}")
            fout.appendln()

            fs.cacheFeatureSet(id)
        }

        println("Finished writing data")
    }
}