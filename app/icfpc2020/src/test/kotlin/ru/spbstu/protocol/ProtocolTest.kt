package ru.spbstu.protocol

import org.junit.Assert.*
import org.junit.Test

class ProtocolTest {
    @Test
    fun decode() {

        val what = Protocol().decode("110110000111011111100001001111110101000000")

        println(what)

    }

    @Test
    fun encode() {
    }
}
