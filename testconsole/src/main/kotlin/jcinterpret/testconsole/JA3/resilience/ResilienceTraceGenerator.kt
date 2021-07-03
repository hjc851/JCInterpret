package jcinterpret.testconsole.JA3.resilience

import jcinterpret.testconsole.JA3.ForkingTraceGenerator
import java.nio.file.Paths

object ResilienceTraceGenerator {
    val configFiles = listOf(
//        Paths.get("/media/haydencheers/Data/SymbExec/algorithms_20.json"),
//        Paths.get("/media/haydencheers/Data/SymbExec/algorithms_40.json"),
//        Paths.get("/media/haydencheers/Data/SymbExec/algorithms_60.json")

        Paths.get("/media/haydencheers/Data/SymbExec/collected_20.json"),
        Paths.get("/media/haydencheers/Data/SymbExec/collected_40.json"),
        Paths.get("/media/haydencheers/Data/SymbExec/collected_60.json")
    )

    @JvmStatic
    fun main(args: Array<String>) {
        for (config in configFiles)
            ForkingTraceGenerator.main(
                arrayOf(
                    config.toAbsolutePath().toString()
                )
            )
    }
}