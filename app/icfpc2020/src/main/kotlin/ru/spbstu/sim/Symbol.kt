package ru.spbstu.sim

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.spbstu.draw.GalaxyDraw
import ru.spbstu.pow
import ru.spbstu.protocol.Protocol
import java.io.File
import java.lang.Exception
import java.util.*
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

interface TwoArgFunction
interface OneArgFunction
interface ThreeArgFunction

data class Curry2(val interp: EvaluationContext.(Symbol, Symbol) -> Symbol): (EvaluationContext, Symbol) -> Symbol, TwoArgFunction {
    data class Applied(val interp: EvaluationContext.(Symbol, Symbol) -> Symbol, val arg1: Symbol): (EvaluationContext, Symbol) -> Symbol, OneArgFunction {
        override fun invoke(ec: EvaluationContext, arg2: Symbol): Symbol {
            return ec.interp(arg1, arg2)
        }

        val hashCode_ = Objects.hash(interp, arg1)
        override fun hashCode(): Int = hashCode_
    }
    override fun invoke(ec: EvaluationContext, arg1: Symbol): Symbol {
        return Fun(Applied(interp, arg1))
    }
}

data class Curry3(
    val interp: EvaluationContext.(Symbol, Symbol, Symbol) -> Symbol
): (EvaluationContext, Symbol) -> Symbol, ThreeArgFunction {



    data class Applied2(
        val interp: EvaluationContext.(Symbol, Symbol, Symbol) -> Symbol,
        val arg1: Symbol,
        val arg2: Symbol
    ): (EvaluationContext, Symbol) -> Symbol, OneArgFunction {
        override fun invoke(ec: EvaluationContext, arg3: Symbol): Symbol {
            return ec.interp(arg1, arg2, arg3)
        }

        val hashCode_ = Objects.hash(interp, arg1, arg2)
        override fun hashCode(): Int = hashCode_
    }
    data class Applied1(
        val interp: EvaluationContext.(Symbol, Symbol, Symbol) -> Symbol,
        val arg1: Symbol
    ): (EvaluationContext, Symbol) -> Symbol, TwoArgFunction {
        override fun invoke(ec: EvaluationContext, arg2: Symbol): Symbol {
            return Fun(Applied2(interp, arg1, arg2))
        }

        val hashCode_ = Objects.hash(interp, arg1)
        override fun hashCode(): Int = hashCode_
    }
    override fun invoke(ec: EvaluationContext, arg1: Symbol): Symbol {
        return Fun(Applied1(interp, arg1))
    }
}

data class Fun(val name: String?, val interp: EvaluationContext.(Symbol) -> Symbol) : Symbol() {
    constructor(interp: EvaluationContext.(Symbol) -> Symbol) : this(null, interp)
    constructor(interp: EvaluationContext.(Symbol, Symbol) -> Symbol) :
            this(Curry2(interp))

    constructor(interp: EvaluationContext.(Symbol, Symbol, Symbol) -> Symbol) :
            this(Curry3(interp))

    val numArgs: Int? get() = when(interp) {
        is Curry3 -> 3
        is Curry2, is Curry3.Applied1 -> 2
        else -> 1
    }

    companion object FunNaming {
        var counter = 0
        fun freshVar() = Var("$${++counter}")
    }
    override fun toString(): String = name ?: run {
        with(EvaluationContext(mutableMapOf(), dryRun = true)) {
            val v = freshVar()
            val app = interp(v)
            "$v -> $app"
        }
    }

    override fun equals(other: Any?): Boolean =
        other is Fun
                && (this.name == other.name)
                && (name !== null || interp == other.interp)

    override fun hashCode(): Int {
        return if(name != null) name.hashCode() else interp.hashCode()
    }

    operator fun getValue(self: Any?, prop: KProperty<*>) = copy(name = prop.name)
}

class EvaluationContext(val mapping: MutableMap<Symbol, Symbol>, val dryRun: Boolean = false) {
    fun strict(sym: Symbol): Symbol {
        if(dryRun) return sym
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

data class Cons(val car: Symbol, val cdr: Symbol) : Symbol(), TwoArgFunction {
    override fun subst(mapping: Map<Symbol, Symbol>): Symbol =
        Cons(car.subst(mapping), cdr.subst(mapping))

    fun interp(arg: Symbol) = arg(car)(cdr)

    val hashCode_ = Objects.hash(car, cdr)
    override fun hashCode(): Int = hashCode_

    override fun eval(mapping: MutableMap<Symbol, Symbol>): Symbol =
        copy(car.eval(mapping), cdr.eval(mapping))

    fun iterator(): Iterator<Symbol> = kotlin.sequences.iterator<Symbol> {
        var current = this@Cons
        loop@while(true) {
            yield(current.car)
            when (val cdr = current.cdr) {
                is Cons -> {
                    current = cdr
                }
                else -> {
                    yield(cdr)
                    break@loop
                }
            }
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

    val hashCode_ = Objects.hash(f, arg)
    override fun hashCode(): Int = hashCode_
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

    override fun toString(): String = name
}

data class Picture(val ones: Set<Pair<Long, Long>>) : Symbol() {
    override fun toString(): String {
        val minWidth = ones.map { it.first }.min()?.minus(0) ?: 0
        val minHeight = ones.map { it.second }.min()?.minus(0) ?: 0

        val maxWidth = ones.map { it.first }.max()?.plus(0) ?: 5
        val maxHeight = ones.map { it.second }.max()?.plus(0) ?: 5

        val width = maxWidth - minWidth + 1
        val height = maxHeight - minHeight + 1

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

//val appCache = mutableMapOf<Pair<Symbol, Symbol>, Symbol>()
fun app(f: Symbol, arg: Symbol): Symbol {
    when(f) {
        is Fun -> try {
            with(f) {
                return EvaluationContext(mutableMapOf()).interp(arg)
            }
        } catch (ex: Exception) {
            return Ap(f, arg)
        }
        is Ap -> {
            when {
                f.f is Fun && f.f.numArgs == 2 -> {
                    try {
                        with(f.f) {
                            return EvaluationContext(mutableMapOf()).interp(f.arg)(arg)
                        }
                    } catch (ex: IllegalStateException) {
                        return Ap(f, arg)
                    }
                }
                f.f is Ap && f.f.f is Fun && f.f.f.numArgs == 3 -> {
                    try {
                        with(f.f.f) {
                            return EvaluationContext(mutableMapOf()).interp(f.f.arg)(f.arg)(arg)
                        }
                    } catch (ex: IllegalStateException) {
                        return Ap(f, arg)
                    }
                }
            }
        }
    }
    return Ap(f, arg)
    //return appCache[f to arg] ?: Ap(f, arg).also { appCache[f to arg] = it }
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
    check(a is Num) { a }
    check(b is Num) { b }
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
        else -> throw IllegalStateException()
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
        if (cell is Nil) return@flatMap sequenceOf<Symbol>()
        if (cell !is Cons) return@flatMap sequenceOf<Symbol>(cell)
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

data class State(val x: Long, val y: Long, val state: Symbol)

fun eval(bindings: List<Symbol>, initialState: State) {
    val states = ArrayDeque<State>()
    val galaxyDraw = GalaxyDraw()
    eval(bindings, initialState) { state, pics ->
        states.addFirst(state)
        val images = sequenceOf(pics)
            .flatten()
            .filterIsInstance<Picture>()
            .toList()

        val current = galaxyDraw.interact(images)
        if (current == null) {
            states.pop()
            states.pop()
        } else {
            val x = current.first.toLong()
            val y = current.second.toLong()
            State(x, y, state.state)
        }
    }
}

fun eval(bindings: List<Symbol>, initialState: State, next: (State, Symbol) -> State?) {
    val bindingContext = mutableMapOf<Symbol, Symbol>()

    for (binding in bindings) {
        check(binding is Binding)
        if (binding.lhs !in bindingContext)
            bindingContext[binding.lhs] = binding.rhs
    }

    val galaxy = bindings.last()
    galaxy as Binding
    println(galaxy)

    var (curX, curY, stateSymbol) = initialState

    println("[In] State: $stateSymbol")
    println("[In] Encoded state: ${encode(stateSymbol)}")
    println("[In] Coords: $curX -> $curY")

    while (true) {

        val (newState, pics) = interact(bindingContext, galaxy.lhs, stateSymbol, vec(Num(curX))(Num(curY)))
        val answer = newState.exhaustiveEval(bindingContext)
        val images = pics.exhaustiveEval(bindingContext)

        val currentState = State(curX, curY, answer)
        val nextState = next(currentState, images) ?: return

        curX = nextState.x
        curY = nextState.y
        stateSymbol = nextState.state

        println("State: $stateSymbol")
        println("Encoded state: ${encode(stateSymbol)}")
        println("Coords: $curX -> $curY")
    }
}

fun Symbol.exhaustiveEval(mapping: MutableMap<Symbol, Symbol>): Symbol {
    var current = mapping.getOrElse(this) { this }
    do {
        val prev = current
        current = current.eval(mapping)
    } while (current != prev)
    //mapping[this] = current
    return current
}

fun f38(bindingContext: MutableMap<Symbol, Symbol>, protocol: Symbol, res: Symbol): Pair<Symbol, Symbol> {
    val flag = car(res).exhaustiveEval(bindingContext)
    val newState = car(cdr(res)).exhaustiveEval(bindingContext)
    val data = car(cdr(cdr(res))).exhaustiveEval(bindingContext)

    println(flag)
    println(newState)
    println(Protocol().encode(newState))
    println(data)

    val p = Protocol()
    val modem = p.decode(p.encode(newState))

    if (modem != newState) {
        println("New state: $newState")
        println("Modem: $modem")
    }

    if (flag == Num(0)) {
        val pics = multipledraw(data)

        return modem to pics
    } else {
        val client = OkHttpClient()

        val request =
            Request.Builder().url(GSMS.serverUrl).post(Protocol().encode(data).toRequestBody()).build()
        val response = client.newCall(request).execute()
        val status = response.code
        val body = response.body ?: TODO("FUCK")

        val res = body.string()
        println(res)

        val parsed = Protocol().decode(res)
        println(parsed)

        return interact(bindingContext, protocol, modem, parsed)
    }
}

fun interact(
    bindingContext: MutableMap<Symbol, Symbol>,
    protocol: Symbol,
    state: Symbol,
    point: Symbol
): Pair<Symbol, Symbol> {
    return f38(bindingContext, protocol, protocol(state)(point).exhaustiveEval(bindingContext))
}

fun encode(msg: Symbol) = Protocol().encode(msg)
fun decode(msg: String) = Protocol().decode(msg)

fun main() {
    GSMS.playerKey = "5b38d34226a14d73878985ecdc25f79a"
    GSMS.serverUrl = "https://icfpc2020-api.testkontur.ru/aliens/send?apiKey=${GSMS.playerKey}"

    val aa = parse(File("data/galaxy.txt").readText())
//    val initialState = State(-3L, -3L, nil)
//    val initialState = State(-3L, -3L, decode("1101100001111101111110000000000000000010110011010110000"))
    val initialState = State(1L, 4L, decode("11011001011111011000101101011001100110011001100110111111111111111111000000000000000000000000000000000000000000000000000001100011111110101001101101001110000"))
//    val initialState = State(
//        46L,
//        1L,
//        decode("11011001011111011000111101011001100110011001111011100010010001011011111111111111111100000000000000000000000000000000000000000000000000000101101010011011100110111111000000000000000001001110000")
//    )
    eval(aa, initialState)
//    eval(aa, initialState) { state, _ ->
//        val prestate = state.state.cdr.car.cdr.cdr.cdr.car.cdr.car
//        val firstCodeState = prestate.car.cdr.car
//        val secondCodeState = prestate.cdr.car.cdr.car
//        println(firstCodeState)
//        println(secondCodeState)
//        null
//    }
}

val Symbol.car get() = (this as Cons).car
val Symbol.cdr get() = (this as Cons).cdr
