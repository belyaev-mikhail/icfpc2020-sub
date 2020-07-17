package ru.spbstu.data

sealed class Datum

data class Cons(val head: Datum, val tail: Datum) : Datum()

data class Num(val value: Int) : Datum()

object Nil : Datum() {
    override fun toString(): String {
        return "Nil"
    }
}
