package ru.spbstu.protocol

import org.junit.Assert.*
import org.junit.Test

class ProtocolTest {
    @Test
    fun decode() {
        val what = Protocol().decode("1101100010110111111111111111110011000100110100011010110000010010111110100000101110000001010110000")

        val j = "0110001001101000110101100000100101111101000001011100000010101100"

        val i = "0110001001101000110101100000100101111101000001011100000010101100".toLong(2)

        println(i)

        println(what)

        println(Protocol().encode(what))

    }

    @Test
    fun encode() {
    }
}
