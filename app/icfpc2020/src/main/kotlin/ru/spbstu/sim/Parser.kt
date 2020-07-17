package ru.spbstu.sim

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import ru.spbstu.antlr4.LanguageLexer
import ru.spbstu.antlr4.LanguageParser

fun parse(file: String) {
    val lexer = LanguageLexer(CharStreams.fromString(file))
    val tokens = CommonTokenStream(lexer)
    val parser = LanguageParser(tokens)
    val tree = parser.fileContent()

    val a = 3

}

fun main() {
    val content = """
:1029 = ap ap cons 7 ap ap cons 123229502148636 nil
    """.trimIndent()
    parse(content)
}