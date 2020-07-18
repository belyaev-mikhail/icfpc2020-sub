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
    return parseStatements(tree)
}

private fun parseStatements(tree: LanguageParser.FileContentContext): List<Symbol> {
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
    expression.function() != null -> parseFunction(expression.function())
    expression.application() != null -> parseApplication(expression.application())
    else -> TODO()
}

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

    function.dem_rule() != null -> TODO("demodulate")
    function.mod_rule() != null -> TODO("modulate")

    function.eq_rule() != null -> eq

    function.lt_rule() != null -> lt
    function.neg_rule() != null -> neg
    function.isnil_rule() != null -> isnil

    function.list_rule() != null -> parseList(function.list_rule())
    function.combinator() != null -> parseCombinator(function.combinator())
    else -> TODO()
}

private fun parseList(listRule: LanguageParser.List_ruleContext): Symbol = when {
    listRule.cons_rule() != null -> cons
    listRule.car_rule() != null -> car
    listRule.cdr_rule() != null -> cdr
    else -> TODO()
}

private fun parseCombinator(combinator: LanguageParser.CombinatorContext): Symbol = when {
    combinator.b_comb_rule() != null -> b
    combinator.c_comb_rule() != null -> c
    combinator.i_comb_rule() != null -> i
    combinator.s_comb_rule() != null -> s
    else -> TODO()
}