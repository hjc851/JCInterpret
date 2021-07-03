import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object ScoreAnalyser {


    val scoref = Paths.get("/home/haydencheers/Documents/externally_sourced_experiments/bplag/scores-allmethods/SENG2050_A2_2021 - SENG2050_A2_GITHUB.txt")

    val sus = listOf(
        "3168517" to "kurtis-github-seng2050-a2-main",
        "3204936" to "kurtis-github-seng2050-a2-main",
        "3331609" to "kurtis-github-seng2050-a2-main",
        "3356085" to "kurtis-github-seng2050-a2-main",

        "3309266" to "amandaCrowley_Java_JSP_Game"
    )

    @JvmStatic
    fun main(args: Array<String>) {
        Files.readAllLines(scoref)
            .map { it.split("\t") }
            .map { Triple(it[0], it[1], it[2].toDoubleOrNull()) }
            .filter { it.third != null && it.first != it.second }
            .sortedByDescending { it.third }
            .forEach { println(String.format("%s - %s : %.2f", it.first, it.second, it.third)) }
//            .forEach { printTriple(it as Triple<String, String, Double>) }
    }

    private fun printTriple(triple: Triple<String, String, Double>) {

        val lstd = if (triple.first.startsWith("c")) triple.first.substring(1, 8)
        else triple.first

        val rstd = if (triple.second.startsWith("c")) triple.second.substring(1, 8)
        else triple.second

        val prefix = if (sus.contains(lstd to rstd) || sus.contains(rstd to lstd)) "* "
        else ""

        println(String.format("%s%s - %s : %.2f", prefix, lstd, rstd, triple.third))
    }
}