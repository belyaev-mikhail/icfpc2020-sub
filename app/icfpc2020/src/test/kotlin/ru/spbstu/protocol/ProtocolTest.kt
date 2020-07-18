package ru.spbstu.protocol

import org.junit.Assert.*
import org.junit.Test

class ProtocolTest {
    @Test
    fun decode() {

        val what = Protocol().decode("11010111101001011010110000")

        println(what)

        println(Protocol().encode(what))

    }

    @Test
    fun encode() {
    }
}
