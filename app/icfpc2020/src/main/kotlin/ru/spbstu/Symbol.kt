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

    data class Custom(
        override val key: Matrix,
        override val code: Long,
        override val command: String
    ) : Symbol()

    data class Number(
        override val key: Matrix,
        override val code: Long
    ) : Symbol() {
        override val command: String = "$code"
    }

    data class Variable(
        override val key: Matrix,
        override val code: Long,
        override val command: String
    ) : Symbol()

    data class Function(
        override val key: Matrix,
        override val code: Long,
        override val command: String
    ) : Symbol()
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
        set(function.copy(command = name))
    }

    init {
        custom("  .| ..|...| ..|  .", "(")
        custom(".  |.. |...|.. |.  ", ")")
        custom("..|..|..|..|..", "|")
        custom("""
            |  ...  
            |     . 
            | ...  .
            |. . . .
            |.  ... 
            | .     
            |  ...  """.trimMargin(), "universe")
    }
}
