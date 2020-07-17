package ru.spbstu.sim

import kotlin.reflect.KProperty

sealed class Symbol {
}

data class Num(val number: Long): Symbol() {
    override fun toString() = number.toString()
}

operator fun Symbol.inc(): Symbol {
    check(this is Num)
    return Num(number + 1)
}
operator fun Symbol.plus(that: Symbol): Symbol {
    check(this is Num)
    check(that is Num)
    return Num(number + that.number)
}
operator fun Symbol.dec(): Symbol {
    check(this is Num)
    return Num(number - 1)
}
operator fun Symbol.minus(that: Symbol): Symbol {
    check(this is Num)
    check(that is Num)
    return Num(number - that.number)
}

operator fun Symbol.times(b: Symbol): Symbol {
    check(this is Num)
    check(b is Num)
    return Num(number * b.number)
}
operator fun Symbol.div(b: Symbol): Symbol {
    check(this is Num)
    check(b is Num)
    return Num(number / b.number)
}


data class Fun(val name: String?, val interp: (Symbol) -> Symbol): Symbol() {
    constructor(interp: (Symbol) -> Symbol): this(null, interp)
    override fun toString(): String = name ?: interp.toString()

    operator fun getValue(self: Any?, prop: KProperty<*>) = copy(name = prop.name)
}

object Nil : Symbol() {
    override fun toString(): String = "()"
}

data class Cons(val car: Symbol, val cdr: Symbol): Symbol() {
    fun iterator() = kotlin.sequences.iterator {
        yield(car)
        when(cdr) {
            Nil -> yield(Nil)
            is Cons -> yieldAll(cdr.iterator())
        }
    }

    override fun toString(): String = iterator().asSequence().joinToString()
}

data class Ap(val f: Symbol, val arg: Symbol): Symbol()

data class Var(val name: String): Symbol()

fun app(f: Symbol, arg: Symbol): Symbol {
    return when(f) {
        is Fun -> f.interp(arg)
        else -> Ap(f, arg)
    }
}

val inc by Fun { it + Num(1) }
val dec by Fun { it - Num(1) }
val add by Fun { a -> Fun { b -> a + b } }
val mul by Fun { a -> Fun { b -> a * b } }
val div by Fun { a -> Fun { b -> a / b } }

val t by Fun { a -> Fun { b -> a } }
//val f = s(t)
