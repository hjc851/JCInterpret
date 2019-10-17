package jcinterpret.testconsole

import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.document.DocumentUtils
import java.nio.file.Paths

fun main(args: Array<String>) {
    val path = Paths.get("/Users/haydencheers/Desktop/JS1/JS1-Traces/2019-10-16T23:14:44.980Z/QUICKSORT_1/LQuickSort;sort([III)V")

    val document = DocumentUtils.read(path, EntryPointExecutionTraces::class)

    Unit
}