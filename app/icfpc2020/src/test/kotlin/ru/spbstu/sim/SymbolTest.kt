package ru.spbstu.sim

import org.junit.Test
import kotlin.test.assertEquals

internal class SymbolTest {
    @Test
    fun smokeyTest() {
        val ps =
            consListOf(
                vec(Num(3))(Num(1)),
                vec(Num(4))(Num(2))
            )
        assertEquals(Picture(setOf(Pair(3L, 1L), Pair(4L, 2L))), draw(ps).eval(mutableMapOf()))

        assertEquals(Num(2), (s(i)(i)(i)(Num(2))).eval(mutableMapOf()))
    }
}