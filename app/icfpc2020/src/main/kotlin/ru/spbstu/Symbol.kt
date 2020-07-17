package ru.spbstu

data class Matrix(
    val width: Int,
    val height: Int,
    val data: List<List<Boolean>>
)

sealed class Symbol {
    abstract val key: Matrix
    abstract val code: Long
    abstract val command: String

    data class Number(
        override val key: Matrix,
        override val code: Long
    ) : Symbol() {
        override val command: String = "$code"
    }

    data class Variable(
        override val key: Matrix,
        override val code: Long
    ) : Symbol() {
        override val command: String = "x$code"
    }

    data class Function(
        override val key: Matrix,
        override val code: Long,
        val name: String = ":$code"
    ) : Symbol() {
        override val command: String = ":$code"
    }
}

object Symbols {
    private val cache = HashMap<Matrix, Symbol>()

    fun get(key: Matrix): Symbol {
        return cache.getOrPut(key) { key.toSymbol() }
    }

    fun set(symbol: Symbol) {
        cache[symbol.key] = symbol
    }

    fun setFunctionName(key: Matrix, name: String) {
        val function = get(key) as Symbol.Function
        set(function.copy(name = name))
    }
}

fun Symbols.symbol(data: String): Symbol {
    return get(parseMatrix(data))
}

fun Symbols.number(data: String): Symbol.Number {
    return symbol(data) as Symbol.Number
}

fun Symbols.variable(data: String): Symbol.Variable {
    return symbol(data) as Symbol.Variable
}

fun Symbols.function(data: String): Symbol.Function {
    return symbol(data) as Symbol.Function
}

fun parseMatrix(data: String): Matrix {
    return parseMatrix(data.split('|', '\n').map { row -> row.map { it == '.' } })
}

fun parseMatrix(data: List<List<Boolean>>): Matrix {
    val height = data.size
    val width = data.map { it.size }.max()
    requireNotNull(width)
    require(data.map { it.size }.min() == width)
    require(height > 0)
    require(width > 0)
    return Matrix(width, height, data)
}

fun Matrix.toSymbol(): Symbol {
    return requireNotNull(toNumber() ?: toVariable() ?: toFunction())
}

fun Matrix.toNumber(): Symbol.Number? {
    if (data[0][0]) return null
    if (data[0].drop(1).any { !it }) return null
    if (data.drop(1).any { !it[0] }) return null
    val isPositive = height - width == 0
    val code = drop().toCode()
    val number = if (isPositive) code else -code
    return Symbol.Number(this, number)
}

fun Matrix.toVariable(): Symbol.Variable? {
    if (data[0].any { !it }) return null
    if (data.any { !it[0] }) return null
    if (data.last().any { !it }) return null
    if (data.any { !it.last() }) return null
    val number = drop().dropLast().invert().toNumber() ?: return null
    return Symbol.Variable(this, number.code)
}

fun Matrix.toFunction(): Symbol.Function? {
    if (data[0].any { !it }) return null
    if (data.any { !it[0] }) return null
    val code = drop().toCode()
    return Symbol.Function(this, code)
}

fun Long.pow(power: Long): Long {
    return (0 until power).fold(1L) { acc, _ -> acc * this }
}

fun Matrix.toCode(): Long {
    return data.flatten().foldIndexed(0L) { i, acc, it -> acc + if (it) 2L.pow(i.toLong()) else 0 }
}

fun Matrix.invert(): Matrix {
    val data = data.map { row -> row.map { !it } }
    return Matrix(width, height, data)
}

fun Matrix.drop(): Matrix {
    require(width > 1)
    require(height > 1)
    val data = data.drop(1).map { it.drop(1) }
    return Matrix(width - 1, height - 1, data)
}

fun Matrix.dropLast(): Matrix {
    require(width > 1)
    require(height > 1)
    val data = data.dropLast(1).map { it.dropLast(1) }
    return Matrix(width - 1, height - 1, data)
}
