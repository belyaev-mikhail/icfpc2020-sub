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

//    println(flag)
//    println(newState)
//    println(Protocol().encode(newState))
//    println(data)

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
    val initialState = State(0L, 0L, decode("11011001001111011000011111011100111101011011111111110000000000000000000000000000011001011110111100001100110101101111111111000000000000000000000000000001010010011011110000110111100110111100001111001001101111111111000000000000000000000000000001100101011011100100110111011111111110000000000000000000000000000011111011110111000111000110111100001110010001101111000011011001111011100001110011011110000101001001110111100001000000011101111000010000100111011110000111110101110111000010010110111111111100000000000000000000000000000101111101101111000011010011111011110000110000000110111100001101100101101111000010000101011011100100010111011100010001011011110000110110101110111111111100000000000000000000000000000110010111101111111111000000000000000000000000000001001100011011111111110000000000000000000000000000010100000110111100001101010011101111111111000000000000000000000000000001111010111011110000110101100110111001100011110111001101011110111111111100000000000000000000000000000110000001101111000010111010011011110000101011010110111100001010110001101111111111000000000000000000000000000001010100111011110000111011110110111100001100010011101111000011111011011011111111110000000000000000000000000000011001001110111100001111100011101111000010011100111011100010000011011110000100011001110111100001111111101101111000011011010011011100001011011011111111110000000000000000000000000000011101101110111001010000110111100001010001011101111000011001010111011111111110000000000000000000000000000010111000110111100001011001101101110001110011101111000010001010011011110000101100111110111111111100000000000000000000000000000101111011101111000010001110011011110000100010101110111111111100000000000000000000000000000110001101101111111111000000000000000000000000000001111010000111010000111011111111000100100000000100010110101101111011111111110000000000000000000000000000011001011110111111111100000000000000000000000000000110010111101111111111000000000000000000000000000001100000011011110000110000000110111001111010110111100001100110100000110101111011111111001100010110110011101110101000000"))
    "1101100100111101100001111101110011110101101111111111000000000000000000000000000001100101111011110000110011010110111111111100000000000000000000000000000101001001101111000011011110011011110000111100100110111111111100000000000000000000000000000110010101101110010011011101111111111000000000000000000000000000001111101111011100011100011011110000111001000110111100001101100111101110000111001101111000010100100111011110000100000001110111100001000010011101111000011111010111011100001001011011111111110000000000000000000000000000010111110110111100001101001111101111000011000000011011110000110110010110111100001000010101101110010001011101110001000101101111000011011010111011111111110000000000000000000000000000011001011110111111111100000000000000000000000000000100110001101111111111000000000000000000000000000001010000011011110000110101001110111111111100000000000000000000000000000111101011101111000011010110011011100110001111011100110101111011111111110000000000000000000000000000011000000110111100001011101001101111000010101101011011110000101011000110111111111100000000000000000000000000000101010011101111000011101111011011110000110001001110111100001111101101101111111111000000000000000000000000000001100100111011110000111110001110111100001001110011101110001000001101111000010001100111011110000111111110110111100001101101001101110000101101101111111111000000000000000000000000000001110110111011100101000011011110000101000101110111100001100101011101111111111000000000000000000000000000001011100011011110000101100110110111000111001110111100001000101001101111000010110011111011111111110000000000000000000000000000010111101110111100001000111001101111000010001010111011111111110000000000000000000000000000011000110110111111111100000000000000000000000000000111101000011011100001111111011111111111000101100110100011010110001000010110101101111011110000111001000110111111111100000000000000000000000000000110010011101110010011011101111000010100100111011110000110110101110111100001111100011101110001000101101111111111000000000000000000000000000001010000011011111111110000000000000000000000000000011001011110111111111100000000000000000000000000000110010111101111111111000000000000000000000000000001100000011011110000110000000110111001111010110111100001100110100000110101111011111111001100010110110011101110101000000"
    "1101100011111101011110101101011011000011101011011000101101011010110101101000111101111111001101000101011010010000111011111110011010001011011001111011110111111100110100010110110100000011101111111001110011001100100110000111011111110011100110011101001101011110111111100111001100111011110000011101111111001110011011110111110000111011111111111111111111111111000000000000000000000000000000000000000000000000000000000000000000000000000001011111010100001011000011101111111111111111111111111100000000000000000000000000000000000000000000000000000000000000000000000000000101111101010000110001011110111111111111111111111111110000000000000000000000000000000000000000000000000000000000000000000000000000011000000011010111001101100110100011010110000"
    "11011000111111011000101111011000101101100001110110000111011000011101100001110110001011011000101101100010110110000100111101111111001101000101011010010000111011111110011010001011011001111011110111111100110100010110110100000011101111111001110011001100100110000111011111110011100110011101001101011110111111100111001100111011110000011101111111001110011011100101011000111011111110011100110111101111100001110111111100111010100111110000100011101111111111111111111111111100000000000000000000000000000000000000000000000000000000000000000000000000000101111101010000101100001110111111111111111111111111110000000000000000000000000000000000000000000000000000000000000000000000000000010111110101000011000101111011111111111111111111111111000000000000000000000000000000000000000000000000000000000000000000000000000001100000001101011100110110011011111111001100010110110011101110101000011010110000" // AAAA!!!
//    val initialState = State(1L, 4L, decode("11011001011111011000101101011001100110011001100110111111111111111111000000000000000000000000000000000000000000000000000001100011111110101001101101001110000"))
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
