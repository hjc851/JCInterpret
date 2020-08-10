package jcinterpret.testconsole.automator

import org.apache.commons.io.FileUtils
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

object GraphGrouper {
    val root = Paths.get("/media/haydencheers/Data/SymbExec/graphs/Algorithms_60")

//    val bases = listOf(
//        "P1",
//        "P2",
//        "P3",
//        "P4",
//        "P5"
//    )

    val bases = listOf(
        "CHANGEMAKING_DP_1",
        "MST_KRUSKALS_2",
        "SORT_BUBBLE_1",
        "SORT_QUICKSORT_4",
        "CHANGEMAKING_DP_2",
        "MST_KRUSKALS_3",
        "SORT_BUBBLE_2",
        "STRINGMATCH_BM_1",
        "CHANGEMAKING_ITER_1",
        "MST_PRIMMS_1",
        "SORT_MERGE_1",
        "STRINGMATCH_BM_2",
        "CHANGEMAKING_ITER_2",
        "MST_PRIMMS_2",
        "SORT_MERGE_2",
        "STRINGMATCH_KMP_1",
        "CHANGEMAKING_REC_1",
        "MST_PRIMMS_3",
        "SORT_QUICKSORT_1",
        "STRINGMATCH_KMP_4",
        "CHANGEMAKING_REC_2",
        "MST_REVERSEDELETE_1",
        "SORT_QUICKSORT_2",
        "STRINGMATCH_RK_1",
        "MST_KRUSKALS_1",
        "MST_REVERSEDELETE_2",
        "SORT_QUICKSORT_3",
        "STRINGMATCH_RK_2"
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val results = Files.list(root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .toList()

        for (base in bases) {
            val dirs = results.filter { it.fileName.toString().startsWith(base) }

            val tmptarget = Files.createDirectory(root.resolve("_" + base))

            for (dir in dirs) {
                FileUtils.moveDirectory(dir.toFile(), tmptarget.resolve(dir.fileName).toFile())
            }

            val endTarget = root.resolve(base)
            Files.move(tmptarget, endTarget)
        }

    }
}