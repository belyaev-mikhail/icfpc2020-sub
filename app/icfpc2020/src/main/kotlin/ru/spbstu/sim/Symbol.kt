package ru.spbstu.sim

import ru.spbstu.pow
import ru.spbstu.protocol.Protocol
import java.io.File
import kotlin.reflect.KProperty

sealed class Symbol {
    open fun subst(mapping: Map<Symbol, Symbol>) = this
    open fun eval(mapping: MutableMap<Symbol, Symbol>): Symbol = this
}

data class Num(val number: Long) : Symbol() {
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

data class Fun(val name: String?, val interp: EvaluationContext.(Symbol) -> Symbol) : Symbol() {
    constructor(interp: EvaluationContext.(Symbol) -> Symbol) : this(null, interp)
    constructor(interp: EvaluationContext.(Symbol, Symbol) -> Symbol) :
            this({ a -> Fun { b -> interp(a, b) } })

    constructor(interp: EvaluationContext.(Symbol, Symbol, Symbol) -> Symbol) :
            this({ a -> Fun { b -> Fun { c -> interp(a, b, c) } } })

    constructor(interp: EvaluationContext.(Symbol, Symbol, Symbol, Symbol) -> Symbol) :
            this({ a -> Fun { b -> Fun { c -> Fun { d -> interp(a, b, c, d) } } } })

    override fun toString(): String = name ?: interp.toString()

    operator fun getValue(self: Any?, prop: KProperty<*>) = copy(name = prop.name)
}

class EvaluationContext(val mapping: MutableMap<Symbol, Symbol>) {
    fun strict(sym: Symbol): Symbol {
        val cached = mapping[sym] ?: sym

        val res = cached.exhaustiveEval(mapping)
        mapping[sym] = res
        return res
    }
}

object Nil : Symbol() {
    override fun toString(): String = "()"

    fun interp(arg: Symbol) = t
}

data class Cons(val car: Symbol, val cdr: Symbol) : Symbol() {
    override fun subst(mapping: Map<Symbol, Symbol>): Symbol =
        Cons(car.subst(mapping), cdr.subst(mapping))

    fun interp(arg: Symbol) = arg(car)(cdr)

    override fun eval(mapping: MutableMap<Symbol, Symbol>): Symbol =
        copy(car.eval(mapping), cdr.eval(mapping))

    fun iterator(): Iterator<Symbol> = kotlin.sequences.iterator<Symbol> {
        yield(car)
        when (cdr) {
            is Cons -> yieldAll(cdr.iterator())
            else -> yield(cdr)
        }
    }

    override fun toString(): String =
        iterator().asSequence().joinToString(prefix = "(", postfix = ")")
}

data class Ap(val f: Symbol, val arg: Symbol) : Symbol() {
    override fun subst(mapping: Map<Symbol, Symbol>): Symbol =
        app(f.subst(mapping), arg.subst(mapping))

    override fun eval(mapping: MutableMap<Symbol, Symbol>): Symbol {
        with(EvaluationContext(mapping)) {
            return when (val ef = strict(f)) {
                is Fun -> with(ef) { interp(arg) }
                is Nil -> t
                is Cons -> strict(arg)(ef.car)(ef.cdr)
                else -> Ap(ef, arg)
            }
        }
    }
}

data class Var(val name: String) : Symbol() {
    override fun subst(mapping: Map<Symbol, Symbol>): Symbol {
        if (this in mapping) return mapping.getValue(this)
        else return this
    }

    override fun eval(mapping: MutableMap<Symbol, Symbol>): Symbol {
        if (this in mapping) {
            val ret = mapping.getValue(this)
            return ret
        } else return this
    }
}

data class Picture(val ones: Set<Pair<Long, Long>>) : Symbol() {
    override fun toString(): String {
        val minWidth = ones.map { it.first }.min()?.minus(2) ?: 0
        val minHeight = ones.map { it.second }.min()?.minus(2) ?: 0

        val maxWidth = ones.map { it.first }.max()?.plus(2) ?: 5
        val maxHeight = ones.map { it.second }.max()?.plus(2) ?: 5

        val width = maxWidth - minWidth
        val height = maxHeight - minHeight

        val canvas = List(height.toInt()) {
            StringBuilder("□".repeat(width.toInt()))
        }
        for ((x, y) in ones) {
            canvas[(y - minHeight).toInt()][(x - minWidth).toInt()] = '■'
        }
        return canvas.joinToString("\n")
    }
}

data class Binding(val lhs: Symbol, val rhs: Symbol) : Symbol()

fun app(f: Symbol, arg: Symbol): Symbol {
    return when (f) {
        //is Fun -> f.interp(arg)
        else -> Ap(f, arg)
    }
}

operator fun Symbol.invoke(that: Symbol) = app(this, that)

val inc by Fun { it -> strict(it) + Num(1) }
val dec by Fun { it -> strict(it) - Num(1) }
val add by Fun { a, b -> strict(a) + strict(b) }
val mul by Fun { a, b -> strict(a) * strict(b) }
val div by Fun { a, b -> strict(a) / strict(b) }

val s by Fun { a, b, c -> strict(strict(a)(c))(b(c)) }

val t by Fun { a, b -> a }
val f = app(s, t)

val lt by Fun { a, b ->
    val a = strict(a)
    val b = strict(b)
    check(a is Num)
    check(b is Num)
    if (a.number < b.number) t else f
}
val eq by Fun { a, b ->
    val a = strict(a)
    val b = strict(b)
    check(a is Num) { a }
    check(b is Num) { b }
    if (a.number == b.number) t else f
}
val neg by Fun { a -> -strict(a) }
val pwr2 by Fun { a ->
    val a = strict(a)
    check(a is Num)
    Num(2L.pow(a.number))
}

val cons by Fun { h, t -> Cons(h, t) }
val car by Fun { lst ->
    when (lst) {
        is Cons -> lst.car
        else -> lst(t)
    }
}
val cdr by Fun { lst ->
    when (lst) {
        is Cons -> lst.cdr
        else -> lst(f)
    }
}
val nil = Nil
val isnil by Fun { lst ->
    when (val lst = strict(lst)) {
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

fun Sequence<Symbol>.flatten(): Sequence<Symbol> {
    return flatMap { cell ->
        if (cell !is Cons) return@flatMap sequenceOf<Symbol>()
        val (x, y) = cell

        if (x is Num && y is Num) return@flatMap sequenceOf(cell)

        return@flatMap sequenceOf(x).flatten() + sequenceOf(y).flatten()
    }
}

val draw by Fun { lst ->
    when (val lst = strict(lst)) {
        is Nil -> Picture(setOf())
        is Cons -> lst.iterator().asSequence()
            .flatten()
            .mapNotNullTo(mutableSetOf<Pair<Long, Long>>()) { cell ->
                if (cell !is Cons) return@mapNotNullTo null
                val (x, y) = cell
                check(x is Num) { x }
                check(y is Num) { y }
                x.number to y.number
            }
            .let { Picture(it) }
        else -> TODO()
    }
}

val multipledraw: Symbol by Fun { lst ->
    when (val lst = strict(lst)) {
        is Nil -> Nil
        is Cons -> {
            val (x0, x1) = lst
            app(app(cons, app(draw, x0)), app(multipledraw, x1))
        }
        else -> TODO()
    }
}

val if0 by Fun { x ->
    when (val x = strict(x)) {
        Num(0) -> t
        Num(1) -> f
        else -> TODO()
    }
}

val i by Fun { x -> x }
val k = f
val c by Fun { f, x, y -> strict(f)(y)(x) }
val b by Fun { x0, x1, x2 -> strict(x0)(strict(x1)(x2)) }

fun eval(bindings: List<Symbol>) {
    val bindingContext = mutableMapOf<Symbol, Symbol>()

    for (binding in bindings) {
        check(binding is Binding)
        bindingContext[binding.lhs] = binding.rhs
    }

    for (binding in bindings) {
        check(binding is Binding)
        System.err.print("${binding.lhs} := ")
        val res = binding.rhs.eval(bindingContext)
        System.err.println("$res")
        bindingContext[binding.lhs] = res
    }

    val galaxy = bindings.last()
    galaxy as Binding
    println(galaxy)

    var state: Symbol = nil

    while (true) {
        val bc = bindingContext.toMutableMap()

        var task = interact(bc, galaxy.lhs, state, vec(Num(0))(Num(0)))
        val answer = task.exhaustiveEval(bc)
        println(answer)

        if (answer == state) break

        state = answer
    }
}

fun Symbol.exhaustiveEval(mapping: MutableMap<Symbol, Symbol>): Symbol {
    var current = this
    do {
        val prev = current
        current = current.eval(mapping)
    } while (current != prev)
    return current
}

fun f38(bindingContext: MutableMap<Symbol, Symbol>, protocol: Symbol, res: Symbol): Symbol {
    val flag = car(res).exhaustiveEval(bindingContext)
    val newState = car(cdr(res)).exhaustiveEval(bindingContext)
    val data = cdr(cdr(res)).exhaustiveEval(bindingContext)

    println(flag)
    println(newState)
    println(data)

    if (flag == Num(0)) {
        val p = Protocol()
        val modem = p.decode(p.encode(newState))
        val pics = multipledraw(data)

        println(pics.exhaustiveEval(bindingContext))

        return modem

        return cons(modem)(cons(pics)(nil))
    }

    return nil
}

fun interact(bindingContext: MutableMap<Symbol, Symbol>, protocol: Symbol, state: Symbol, point: Symbol): Symbol {
    return f38(bindingContext, protocol, protocol(state)(point).exhaustiveEval(bindingContext))
}

fun main() {
    val ps =
        consListOf(
            vec(Num(3))(Num(1)),
            vec(Num(4))(Num(2))
        )
    println(draw(ps))
    println(draw(ps).eval(mutableMapOf()))

    println(s(i)(i)(i)(Num(2)))
    println((s(i)(i)(i)(Num(2))).eval(mutableMapOf()))
    println(b(b(b)).eval(mutableMapOf()))

    val aa = parse(File("data/galaxy.txt").readText())
    eval(aa)
}
