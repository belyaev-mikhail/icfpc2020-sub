package ru.spbstu.sim

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import ru.spbstu.antlr4.LanguageLexer
import ru.spbstu.antlr4.LanguageParser
import java.io.File

fun parse(file: String): List<Symbol> {
    val lexer = LanguageLexer(CharStreams.fromString(file))
    val tokens = CommonTokenStream(lexer)
    val parser = LanguageParser(tokens)
    val tree = parser.fileContent()
    return parseStatements(tree)
}

fun parseStatements(tree: LanguageParser.FileContentContext): List<Symbol> {
    return tree.statements().statement().map { parseStatement(it) }
}

fun parseStatement(statement: LanguageParser.StatementContext): Symbol {
    val lhs = when {
        statement.identifier() != null -> Var(statement.identifier().text)
        else -> Var(statement.Name().text)
    }
    val rhs = parseExpression(statement.expression())
    return Binding(lhs, rhs)
}

fun parseExpression(expression: LanguageParser.ExpressionContext): Symbol = when {
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

fun parseApplication(application: LanguageParser.ApplicationContext): Symbol {
    val (first, second) = application.expression()
    return Ap(parseExpression(first), parseExpression(second))
}


fun parseFunction(function: LanguageParser.FunctionContext): Symbol = when {
    function.add_rule() != null -> add
    function.mul_rule() != null -> mul
    function.div_rule() != null -> div

    function.inc_rule() != null -> inc
    function.dec_rule() != null -> dec

    function.dem_rule() != null -> TODO("demodulate")
    function.mod_rule() != null -> TODO("modulate")

    function.eq_rule() != null -> TODO("equality")

    function.lt_rule() != null -> lt
    function.neg_rule() != null -> neg
    function.isnil_rule() != null -> isnil

    function.list_rule() != null -> parseList(function.list_rule())
    function.combinator() != null -> parseCombinator(function.combinator())
    else -> TODO()
}

fun parseList(listRule: LanguageParser.List_ruleContext): Symbol = when {
    listRule.cons_rule() != null -> cons
    listRule.car_rule() != null -> car
    listRule.cdr_rule() != null -> cdr
    else -> TODO()
}

fun parseCombinator(combinator: LanguageParser.CombinatorContext): Symbol = when {
    combinator.b_comb_rule() != null -> b
    combinator.c_comb_rule() != null -> c
    combinator.i_comb_rule() != null -> i
    combinator.s_comb_rule() != null -> s
    else -> TODO()
}

fun main() {
    val txt = File("C:\\Users\\napst\\IdeaProjects\\icfpc2020-sub\\app\\icfpc2020\\data\\galaxy.txt").readText()
    parse(txt)
}