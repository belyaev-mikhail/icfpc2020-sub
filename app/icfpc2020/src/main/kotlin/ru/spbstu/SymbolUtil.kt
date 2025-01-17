package ru.spbstu


fun Symbols.custom(data: String, name: String): Symbol.Custom {
    val matrix = parseMatrix(data)
    return Symbol.Custom(matrix, matrix.toCode(), name).also(::set)
}

fun Symbols.symbol(data: String): Symbol {
    return get(parseMatrix(data))
}

fun Symbols.custom(data: String): Symbol.Custom {
    return symbol(data) as Symbol.Custom
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
    return toNumber() ?: toVariable() ?: toFunction() ?: toUndefined()
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
    return Symbol.Variable(this, number.code, "x${number.code}")
}

fun Matrix.toFunction(): Symbol.Function? {
    if (data[0].any { !it }) return null
    if (data.any { !it[0] }) return null
    val code = drop().toCode()
    return Symbol.Function(this, code, ":$code")
}

fun Matrix.toUndefined(): Symbol.Custom {
    val code = toCode()
    return Symbol.Custom(this, code, "u$code")
}

fun Long.pow(power: Long): Long = when {
    power == 0L -> 1
    power == 1L -> this
    power % 2 == 1L -> this * pow(power - 1)
    else -> pow(power / 2).let { it * it }
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
