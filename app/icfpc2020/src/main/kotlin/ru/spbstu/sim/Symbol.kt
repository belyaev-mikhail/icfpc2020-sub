package ru.spbstu.sim

import ru.spbstu.pow
import java.io.File
import kotlin.reflect.KProperty

sealed class Symbol {
    open fun subst(mapping: Map<Symbol, Symbol>) = this
    open fun eval(mapping: Map<Symbol, Symbol>): Symbol = this
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
    override fun subst(mapping: Map<Symbol, Symbol>): Symbol =
            Cons(car.subst(mapping), cdr.subst(mapping))

    override fun eval(mapping: Map<Symbol, Symbol>): Symbol =
        copy(car.eval(mapping), cdr.eval(mapping))

    fun iterator(): Iterator<Symbol> = kotlin.sequences.iterator<Symbol> {
        yield(car)
        when(cdr) {
            is Cons -> yieldAll(cdr.iterator())
            else -> yield(cdr)
        }
    }

    override fun toString(): String =
        iterator().asSequence().joinToString(prefix = "(", postfix = ")")
}

data class Ap(val f: Symbol, val arg: Symbol): Symbol() {
    override fun subst(mapping: Map<Symbol, Symbol>): Symbol =
            app(f.subst(mapping), arg.subst(mapping))

    override fun eval(mapping: Map<Symbol, Symbol>): Symbol {
        val ef = f.eval(mapping)
        val earg = arg.eval(mapping)
        return when (ef) {
            is Fun -> ef.interp(earg).eval(mapping)
            else -> Ap(ef, earg)
        }
    }
}

data class Var(val name: String): Symbol() {
    override fun subst(mapping: Map<Symbol, Symbol>): Symbol {
        if(this in mapping) return mapping.getValue(this)
        else return this
    }

    override fun eval(mapping: Map<Symbol, Symbol>): Symbol {
        if(this in mapping) return mapping.getValue(this)
        else return this
    }
}

data class Picture(val ones: Set<Pair<Long, Long>>): Symbol() {
    override fun toString(): String {
        val width = ones.map { it.first }.max()?.plus(2) ?: 5
        val height = ones.map { it.second }.max()?.plus(2) ?: 5
        val canvas = List(height.toInt()) {
            StringBuilder("□".repeat(width.toInt()))
        }
        for((x, y) in ones) {
            canvas[y.toInt()][x.toInt()] = '■'
        }
        return canvas.joinToString("\n")
    }
}

data class Binding(val lhs: Symbol, val rhs: Symbol): Symbol()

fun app(f: Symbol, arg: Symbol): Symbol {
    return when(f) {
        //is Fun -> f.interp(arg)
        else -> Ap(f, arg)
    }
}

operator fun Symbol.invoke(that: Symbol) = app(this, that)

val inc by Fun { it -> it + Num(1) }
val dec by Fun { it -> it - Num(1) }
val add by Fun { a , b -> a + b }
val mul by Fun { a, b -> a * b }
val div by Fun { a, b -> a / b }

val s by Fun { a, b, c -> (a(c))(b(c)) }

val t by Fun { a, b -> a }
val f = app(s, t)

val lt by Fun { a, b ->
    check(a is Num)
    check(b is Num)
    if(a.number < b.number) t else f
}
val eq by Fun { a, b ->
    check(a is Num) { a }
    check(b is Num) { b }
    if(a.number == b.number) t else f
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
        else -> lst(t)
    }
}
val cdr by Fun { lst ->
    when(lst) {
        is Cons -> lst.cdr
        else -> lst(f)
    }
}
val nil = Nil
val isnil by Fun { lst ->
    when(lst) {
        is Nil -> t
        is Cons -> f
        else -> throw IllegalStateException("$lst")
    }
}

fun consListOf(args: Iterator<Symbol>): Symbol = when {
    !args.hasNext() -> Nil
    else -> Cons(args.next(), consListOf(args))
}
fun consListOf(collection: Collection<Symbol>) = consListOf(collection.iterator())
fun consListOf(vararg symbols: Symbol) = consListOf(symbols.iterator())

val vec = cons

val draw by Fun { lst -> when(lst) {
    is Nil -> Picture(setOf())
    is Cons -> lst.iterator().asSequence().mapNotNullTo(mutableSetOf<Pair<Long, Long>>()) { cell ->
        if(cell !is Cons) return@mapNotNullTo null
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
val c by Fun { f, x, y -> app(f(y), x) }
val b by Fun { x0, x1, x2 -> x0(x1(x2)) }

fun eval(bindings: List<Symbol>) {
    val bindingContext = mutableMapOf<Symbol, Symbol>()
    for(binding in bindings) {
        check(binding is Binding)
        bindingContext[binding.lhs] = binding.rhs
    }
    for(binding in bindings) {
        check(binding is Binding)
        println("" + binding.lhs + " :=" + binding.rhs)
    }
    val galaxy = bindings.last()
    galaxy as Binding
    println(galaxy)
    val task = galaxy.rhs(Nil)(vec(Num(0))(Num(0)))
    var answer = task.eval(bindingContext)
    println(answer)
    answer = answer.eval(bindingContext)
    println(answer)
    answer = answer.eval(bindingContext)
    println(answer)
    answer = answer.eval(bindingContext)
    println(answer)
    answer = answer.eval(bindingContext)
    println(answer)
    answer = answer.eval(bindingContext)
    println(answer)

}

fun main() {
    val ps =
        consListOf(
            vec(Num(3))(Num(1)),
            vec(Num(4))(Num(2))
        )
    println(draw(ps))
    println(draw(ps).eval(mapOf()))

    println(s(i)(i)(i)(Num(2)))
    println((s(i)(i)(i)(Num(2))).eval(mapOf()))

    val aa = parse(File("app/icfpc2020/data/galaxy.txt").readText())
    eval(aa)
    println(aa)
}