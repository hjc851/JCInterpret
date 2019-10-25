package jcinterpret.testconsole

import jcinterpret.core.trace.EntryPointExecutionTraces
import jcinterpret.document.DocumentUtils
import jcinterpret.testconsole.utils.buildTraceModel
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.streams.toList

//fun main(args: Array<String>) {
//    val root = Paths.get(args[0])
//
//    val fout = Paths.get(args[1]).resolve("${root.fileName}_${Date().time}.txt")
//    Files.createFile(fout)
//    val logg = PrintWriter(Files.newBufferedWriter(fout))
//
//    val projects = Files.list(root)
//        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
//        .use { it.toList() }
//
//    for (l in 0 until projects.size) {
//        val lproj = projects[l]
//        val lid = lproj.fileName.toString()
//
//        val ltraces = Files.list(lproj)
//            .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser")}
//            .use { it.toList() }
//            .parallelStream()
//            .map { DocumentUtils.readObject(it, EntryPointExecutionTraces::class) }
//            .toList()
//            .flatMap { it.executionTraces.toList() }
//            .mapIndexed { index, executionTrace ->
//                buildTraceModel(
//                    lid,
//                    index,
//                    executionTrace
//                )
//            }
//
//        for (r in l+1 until projects.size) {
//            val rproj = projects[r]
//            val rid = rproj.fileName.toString()
//
//            val rtraces = Files.list(rproj)
//                .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser")}
//                .use { it.toList() }
//                .parallelStream()
//                .map { DocumentUtils.readObject(it, EntryPointExecutionTraces::class) }
//                .toList()
//                .flatMap { it.executionTraces.toList() }
//                .mapIndexed { index, executionTrace ->
//                    buildTraceModel(
//                        rid,
//                        index,
//                        executionTrace
//                    )
//                }
//
//            logg.println("$lid-${ltraces.size}:$rid-${rtraces.size}")
//            val comparisons = ltraces.size * rtraces.size
//            logg.println(comparisons)
//            logg.println()
//
//            TraceComparator.compare(ltraces, rtraces, logg)
//
//            println("Finished $lid vs $rid")
//            logg.flush()
//
//            System.gc()
//        }
//    }
//
//    logg.print("--END--")
//    logg.flush()
//    logg.close()
//
//    println("Finished ....")
//    System.exit(0)
//}


