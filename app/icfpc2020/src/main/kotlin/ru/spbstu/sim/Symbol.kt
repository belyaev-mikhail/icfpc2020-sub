package ru.spbstu.sim

import ru.spbstu.pow
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
operator fun Symbol.unaryMinus(): Symbol {
    check(this is Num)
    return Num(-this.number)
}

data class Fun(val name: String?, val interp: (Symbol) -> Symbol): Symbol() {
    constructor(interp: (Symbol) -> Symbol): this(null, interp)
    constructor(interp: (Symbol, Symbol) -> Symbol):
            this({ a -> Fun { b -> interp(a, b) }})
    constructor(interp: (Symbol, Symbol, Symbol) -> Symbol):
            this({ a -> Fun { b -> Fun { c -> interp(a, b, c) }}})
    constructor(interp: (Symbol, Symbol, Symbol, Symbol) -> Symbol):
            this({ a -> Fun { b -> Fun { c -> Fun { d -> interp(a, b, c, d) }}}})
    override fun toString(): String = name ?: interp.toString()

    operator fun getValue(self: Any?, prop: KProperty<*>) = copy(name = prop.name)
}

object Nil : Symbol() {
    override fun toString(): String = "()"
}

data class Cons(val car: Symbol, val cdr: Symbol): Symbol() {
    fun iterator(): Iterator<Symbol> = kotlin.sequences.iterator<Symbol> {
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

data class Picture(val ones: Set<Pair<Long, Long>>): Symbol()

fun app(f: Symbol, arg: Symbol): Symbol {
    return when(f) {
        is Fun -> f.interp(arg)
        else -> Ap(f, arg)
    }
}

val inc by Fun { it -> it + Num(1) }
val dec by Fun { it -> it - Num(1) }
val add by Fun { a , b -> a + b }
val mul by Fun { a, b -> a * b }
val div by Fun { a, b -> a / b }

val s by Fun { a, b, c -> app(app(a, c), app(b, c)) }

val t by Fun { a, b -> a }
val f = app(s, t)

val lt by Fun { a, b ->
    check(a is Num)
    check(b is Num)
    if(a.number < b.number) t else f
}
val neg by Fun { a -> -a }
val pwr2 by Fun { a ->
    check(a is Num)
    Num(2L.pow(a.number))
}

val cons by Fun { h, t -> Cons(h, t) }
val car by Fun { lst ->
    when(lst) {
        is Cons -> lst.car
        else -> app(lst, t)
    }
}
val cdr by Fun { lst ->
    when(lst) {
        is Cons -> lst.cdr
        else -> app(lst, f)
    }
}
val nil = Nil
val isnil by Fun { lst ->
    when(lst) {
        is Nil -> t
        is Cons -> f
        else -> TODO()
    }
}

fun consListOf(args: Iterator<Symbol>): Symbol = when {
    !args.hasNext() -> Nil
    else -> Cons(args.next(), consListOf(args))
}
fun consListOf(collection: Collection<Symbol>) = consListOf(collection.iterator())

val vec = cons

val draw by Fun { lst -> when(lst) {
    is Nil -> Picture(setOf())
    is Cons -> lst.iterator().asSequence().mapTo(mutableSetOf()) { cell ->
        check(cell is Cons)
        val (x, y) = cell
        check(x is Num)
        check(y is Num)
        x.number to y.number
    }.let { Picture(it) }
    else -> TODO()
}}

val multipledraw: Symbol by Fun {
    lst -> when(lst) {
    is Nil -> Nil
    is Cons -> {
        val (x0, x1) = lst
        app(app(cons, app(draw,x0)), app(multipledraw, x1))
    }
    else -> TODO()
}
}

val if0 by Fun { x -> when(x) {
    Num(0) -> t
    Num(1) -> f
    else -> TODO()
} }

val i by Fun { x -> x }
val k = f
val c by Fun { f, x, y -> app(app(f, y), x) }
val b by Fun { x0, x1, x2 -> app(x0, app(x1, x2)) }
