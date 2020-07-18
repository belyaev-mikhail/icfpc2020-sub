package ru.spbstu.sim

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class ParserTest {
    @Test
    fun `test parser regressions`() {
        val file = File("data/galaxy.txt").readText()
        val expected = parseOld(file)
        val actual = parse(file)
        assertEquals(expected.size, actual.size)
        for ((e, a) in expected.zip(actual)) {
            assertEquals(e, a)
        }
    }

    @Test
    fun `test parser`() {
        val file = File("data/galaxy.txt").readText()
        assertEquals(file.lines().size, parse(file).size)
    }

    @Test
    fun `test old parser`() {
        val file = File("data/galaxy.txt").readText()
        assertEquals(file.lines().size, parseOld(file).size)
    }
}