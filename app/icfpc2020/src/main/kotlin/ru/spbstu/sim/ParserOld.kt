package ru.spbstu.sim

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import ru.spbstu.antlr4.LanguageLexer
import ru.spbstu.antlr4.LanguageParser

fun parseOld(file: String): List<Symbol> {
    val lexer = LanguageLexer(CharStreams.fromString(file))
    val tokens = CommonTokenStream(lexer)
    val parser = LanguageParser(tokens)
    val tree = parser.fileContent()
    return ParserOld.parseStatements(tree)
}

object ParserOld {

    fun parseStatements(tree: LanguageParser.FileContentContext): List<Symbol> {
        return tree.statements().statement().map { parseStatement(it) }
    }

    private fun parseStatement(statement: LanguageParser.StatementContext): Symbol {
        val lhs = when {
            statement.identifier() != null -> Var(statement.identifier().text)
            else -> Var(statement.Name().text)
        }
        val rhs = parseExpression(statement.expression())
        return Binding(lhs, rhs)
    }

    private fun parseExpression(expression: LanguageParser.ExpressionContext): Symbol = when {
        expression.identifier() != null -> Var(expression.identifier().text)
        expression.number() != null -> Num(expression.number().text.toLong())
        expression.bools() != null -> when {
            expression.bools().true_rule() != null -> t
            expression.bools().false_rule() != null -> f
            else -> TODO()
        }
        expression.nil() != null -> nil
        expression.list_expression() != null -> parseListExpression(expression.list_expression())
        expression.function() != null -> parseFunction(expression.function())
        expression.application() != null -> parseApplication(expression.application())
        else -> TODO()
    }

    private fun parseListExpression(listExpression: LanguageParser.List_expressionContext) = when {
        listExpression.empty_list() != null -> nil
        listExpression.empty_list_with_comma() != null -> nil
        listExpression.non_empty_list() != null -> parseListItems(listExpression.non_empty_list())
        else -> TODO()
    }

    private fun parseListItems(items: LanguageParser.Non_empty_listContext): Symbol =
        items.expression().fold(nil as Symbol) { acc, item -> app(app(cons, parseExpression(item)), acc) }

    private fun parseApplication(application: LanguageParser.ApplicationContext): Symbol {
        val (first, second) = application.expression()
        return Ap(parseExpression(first), parseExpression(second))
    }

    private fun parseFunction(function: LanguageParser.FunctionContext): Symbol = when {
        function.add_rule() != null -> add
        function.mul_rule() != null -> mul
        function.div_rule() != null -> div
        function.inc_rule() != null -> inc
        function.dec_rule() != null -> dec
        function.eq_rule() != null -> eq
        function.lt_rule() != null -> lt
        function.neg_rule() != null -> neg
        function.power_of_2_rule() != null -> TODO("pwr2")

        function.isnil_rule() != null -> isnil
        function.if0_rule() != null -> if0

        function.list_rule() != null -> parseList(function.list_rule())
        function.combinator() != null -> parseCombinator(function.combinator())
        function.interactions() != null -> parseInteractions(function.interactions())
        else -> TODO()
    }

    private fun parseInteractions(interactions: LanguageParser.InteractionsContext) = when {
        interactions.send_rule() != null -> TODO("send")
        interactions.mod_rule() != null -> TODO("mod")
        interactions.dem_rule() != null -> TODO("dem")
        interactions.interact_rule() != null -> TODO("interact")
        interactions.draw_rule() != null -> draw
        interactions.multipledraw_rule() != null -> multipledraw
        interactions.checkerboard_rule() != null -> TODO("checkerboard")
        else -> TODO()
    }

    private fun parseList(listRule: LanguageParser.List_ruleContext): Symbol = when {
        listRule.cons_rule() != null -> cons
        listRule.car_rule() != null -> car
        listRule.cdr_rule() != null -> cdr
        listRule.vec_rule() != null -> vec
        else -> TODO()
    }

    private fun parseCombinator(combinator: LanguageParser.CombinatorContext): Symbol = when {
        combinator.b_comb_rule() != null -> b
        combinator.c_comb_rule() != null -> c
        combinator.i_comb_rule() != null -> i
        combinator.s_comb_rule() != null -> s
        else -> TODO()
    }
}