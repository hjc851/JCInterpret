//package jcinterpret.testconsole
//
//import jcinterpret.core.trace.EntryPointExecutionTraces
//import jcinterpret.document.DocumentUtils
//import jcinterpret.testconsole.utils.TraceComparator
//import jcinterpret.testconsole.utils.buildTraceModel
//import java.io.PrintWriter
//import java.nio.file.Files
//import java.nio.file.Paths
//import java.util.*
//import java.util.concurrent.CompletableFuture
//import kotlin.streams.toList
//
//fun main(args: Array<String>) {
//    val root = Paths.get(args[0])
//
//    val outdir = Paths.get(args[1]).resolve("${root.fileName}_${Date().time}")
//    Files.createDirectory(outdir)
//
//    val plaintiff = args[2]
//
//    val projects = Files.list(root)
//        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
//        .use { it.toList() }
//
//    val plaintiffProj = projects.first { it.fileName.toString() == plaintiff }
//    val defendents = projects.filter { it.fileName.toString() != plaintiff }
//
//
//    println("Loading $plaintiff")
//    val plaintifftraces = Files.list(plaintiffProj)
//        .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser")}
//        .use { it.toList() }
//        .parallelStream()
//        .map { DocumentUtils.readObject(it, EntryPointExecutionTraces::class) }
//        .toList()
//        .flatMap { it.executionTraces.toList() }
//        .mapIndexed { index, executionTrace -> CompletableFuture.supplyAsync {
//            buildTraceModel(
//                plaintiff,
//                index,
//                executionTrace
//            )
//        } }
//        .map { it.get() }
//
//    for (defendent in defendents) {
//        val did = defendent.fileName.toString()
//
//        val fout = outdir.resolve("${plaintiff}_${did}_${Date().time}.txt")
//        Files.createFile(fout)
//
//        val logg = PrintWriter(Files.newBufferedWriter(fout))
//
//        println("Loading $did")
//        val rtraces = Files.list(defendent)
//            .filter { !Files.isDirectory(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".ser")}
//            .use { it.toList() }
//            .parallelStream()
//            .map { DocumentUtils.readObject(it, EntryPointExecutionTraces::class) }
//            .toList()
//            .flatMap { it.executionTraces.toList() }
//            .mapIndexed { index, executionTrace -> CompletableFuture.supplyAsync {
//                buildTraceModel(
//                    plaintiff,
//                    index,
//                    executionTrace
//                )
//            } }
//            .map { it.get() }
//
//        logg.println("$plaintiff-${plaintifftraces.size}:$did-${rtraces.size}")
//        val comparisons = plaintifftraces.size * rtraces.size
//        logg.println(comparisons)
//        logg.println()
//
//        TraceComparator.compare(plaintifftraces, rtraces, logg)
//
//        println("Finished $plaintiff vs $did")
//
//        logg.print("--END--")
//        logg.flush()
//        logg.close()
//
//        System.gc()
//    }
//
//    println("Finished ....")
//    System.exit(0)
//}
//
