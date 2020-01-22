package jcinterpret.feature

object FeatureNameSanitiser {
    val ff = 12.toChar().toString()
    val nc = '\u0000'.toString()

    fun sanitise(str: String): String {
        return str.replace("<", "LESS")
            .replace(">", "GRTR")
            .replace("[", "LBRACE")
            .replace("]", "RBRACE")
            .replace("(", "LPAR")
            .replace(")", "RPAR")
            .replace("{", "LBRACKET")
            .replace("}", "RBRACKET")
            .replace("/", "DIV")
            .replace(".", "DOT")
            .replace(",", "COMA")
            .replace("#", "HASH")
            .replace(" ", "SPACE")
            .replace("%", "PERC")
            .replace("@", "AT")
            .replace("+", "PLUS")
            .replace("-", "MINUS")
            .replace("=", "EQUALS")
            .replace("*", "MULTIPLY")
            .replace("\"", "QUOTE")
            .replace("'", "SQUOTE")
            .replace("\n", "NL")
            .replace("\r", "CF")
            .replace("\t", "TAB")
            .replace(ff, "_ff")
            .replace(nc, "_null")
            .replace(":", "COLON")
            .replace(";", "SEMI")
    }
}

fun String.sanitise(): String = FeatureNameSanitiser.sanitise(this)