package ru.spbstu.sim

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import ru.spbstu.antlr4.LanguageLexer
import ru.spbstu.antlr4.LanguageParser
import java.util.*


private class Parser {
    private val actions = ArrayDeque<() -> Unit>()
    private val symbols = ArrayDeque<Symbol>()

    fun parse(file: String): List<Symbol> {
        val lexer = LanguageLexer(CharStreams.fromString(file))
        val tokens = CommonTokenStream(lexer)
        val parser = LanguageParser(tokens)
        val tree = parser.fileContent()
        return parseStatements(tree)
    }

    private fun symbol(): Symbol {
        return symbols.pop()
    }

    private fun later(action: () -> Unit) {
        actions.addFirst(action)
    }

    private fun result(action: () -> Symbol) {
        later { symbols.addFirst(action()) }
    }

    private fun parseStatements(tree: LanguageParser.FileContentContext): List<Symbol> {
        return tree.statements().statement().map { a ->
            later { parseStatement(a) }
            while (actions.size > 0) {
                val action = actions.pop()
                action()
            }
            symbol()
        }
    }

    private fun parseStatement(statement: LanguageParser.StatementContext) {
        result { Binding(symbol(), symbol()) }
        later { parseStatementVar(statement) }
        later { parseExpression(statement.expression()) }
    }

    private fun parseStatementVar(statement: LanguageParser.StatementContext) = when {
        statement.identifier() != null -> result { Var(statement.identifier().text) }
        statement.Name() != null -> result { Var(statement.Name().text) }
        else -> TODO()
    }

    private fun parseExpression(expression: LanguageParser.ExpressionContext) = when {
        expression.identifier() != null -> result { Var(expression.identifier().text) }
        expression.number() != null -> result { Num(expression.number().text.toLong()) }
        expression.bools() != null -> when {
            expression.bools().true_rule() != null -> result { t }
            expression.bools().false_rule() != null -> result { f }
            else -> TODO()
        }
        expression.nil() != null -> result { nil }
        expression.function() != null -> later { parseFunction(expression.function()) }
        expression.application() != null -> later { parseApplication(expression.application()) }
        else -> TODO()
    }

    private fun parseApplication(application: LanguageParser.ApplicationContext) {
        result { Ap(symbol(), symbol()) }
        later { parseExpression(application.expression()[0]) }
        later { parseExpression(application.expression()[1]) }
    }

    private fun parseFunction(function: LanguageParser.FunctionContext) = when {
        function.add_rule() != null -> result { add }
        function.mul_rule() != null -> result { mul }
        function.div_rule() != null -> result { div }

        function.inc_rule() != null -> result { inc }
        function.dec_rule() != null -> result { dec }

        function.dem_rule() != null -> TODO("demodulate")
        function.mod_rule() != null -> TODO("modulate")

        function.eq_rule() != null -> result { eq }

        function.lt_rule() != null -> result { lt }
        function.neg_rule() != null -> result { neg }
        function.isnil_rule() != null -> result { isnil }

        function.list_rule() != null -> later { parseList(function.list_rule()) }
        function.combinator() != null -> later { parseCombinator(function.combinator()) }
        else -> TODO()
    }

    private fun parseList(listRule: LanguageParser.List_ruleContext) = when {
        listRule.cons_rule() != null -> result { cons }
        listRule.car_rule() != null -> result { car }
        listRule.cdr_rule() != null -> result { cdr }
        else -> TODO()
    }

    private fun parseCombinator(combinator: LanguageParser.CombinatorContext) = when {
        combinator.b_comb_rule() != null -> result { b }
        combinator.c_comb_rule() != null -> result { c }
        combinator.i_comb_rule() != null -> result { i }
        combinator.s_comb_rule() != null -> result { s }
        else -> TODO()
    }
}

fun parse(file: String) = Parser().parse(file)
