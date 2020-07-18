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
        expression.list_expression() != null -> later { parseListExpression(expression.list_expression()) }
        expression.function() != null -> later { parseFunction(expression.function()) }
        expression.application() != null -> later { parseApplication(expression.application()) }
        else -> TODO()
    }

    private fun parseListExpression(listExpression: LanguageParser.List_expressionContext) = when {
        listExpression.empty_list() != null -> result { nil }
        listExpression.empty_list_with_comma() != null -> result { nil }
        listExpression.non_empty_list() != null -> later { parseListItems(listExpression.non_empty_list()) }
        else -> TODO()
    }

    private fun parseListItems(items: LanguageParser.Non_empty_listContext) {
        result { items.expression().fold(nil as Symbol) { acc, item -> app(app(cons, symbol()), acc) } }
        items.expression().forEach { expr -> later { parseExpression(expr) } }
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
        function.eq_rule() != null -> result { eq }

        function.lt_rule() != null -> result { lt }
        function.neg_rule() != null -> result { neg }
        function.isnil_rule() != null -> result { isnil }
        function.power_of_2_rule() != null -> result { TODO("pwr2") }
        function.if0_rule() != null -> result { if0 }

        function.list_rule() != null -> later { parseList(function.list_rule()) }
        function.combinator() != null -> later { parseCombinator(function.combinator()) }
        function.interactions() != null -> later { parseInteractions(function.interactions()) }
        else -> TODO()
    }

    private fun parseInteractions(interactions: LanguageParser.InteractionsContext) = when {
        interactions.send_rule() != null -> TODO("send")
        interactions.mod_rule() != null -> TODO("mod")
        interactions.dem_rule() != null -> TODO("dem")
        interactions.interact_rule() != null -> TODO("interact")
        interactions.draw_rule() != null -> result { draw }
        interactions.multipledraw_rule() != null -> result { multipledraw }
        interactions.checkerboard_rule() != null -> TODO("checkerboard")
        else -> TODO()
    }

    private fun parseList(listRule: LanguageParser.List_ruleContext) = when {
        listRule.cons_rule() != null -> result { cons }
        listRule.car_rule() != null -> result { car }
        listRule.cdr_rule() != null -> result { cdr }
        listRule.vec_rule() != null -> result { vec }
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
