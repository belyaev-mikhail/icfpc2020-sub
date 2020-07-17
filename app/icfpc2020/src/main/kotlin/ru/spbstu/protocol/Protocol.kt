package ru.spbstu.protocol

import ru.spbstu.sim.Cons
import ru.spbstu.sim.Nil
import ru.spbstu.sim.Num
import ru.spbstu.sim.Symbol
import kotlin.math.ceil
import kotlin.math.log2

class Protocol {

    private var currIndex = 0

    fun reset() {
        currIndex = 0
    }

    fun decode(msg: String): Symbol {
        val header = msg.substring(currIndex, currIndex + 2)

        currIndex += 2

        when (header) {
            "00" -> {
                return Nil
            }
            "01", "10" -> {
                var width = 0
                while (msg[currIndex] == '1') {
                    currIndex++
                    width++
                }
                currIndex++

                if (width == 0) return Num(0L)

                val num = msg.substring(currIndex, currIndex + 4 * width).toLong(2)

                currIndex += 4 * width

                return Num(if (header == "01") num else -num)
            }
            "11" -> {
                val head = decode(msg)
                val tail = decode(msg)

                return Cons(head, tail)
            }
        }

        TODO("SHOULD NOT HAPPEN")
    }

    fun encode(msg: Symbol): String {
        when (msg) {
            is Num -> {
                when {
                    msg.number == 0L -> return "010"
                    msg.number == 1L -> return "01100001"
                }

                val res = StringBuilder()

                val num = if (msg.number > 0) {
                    res.append("01")
                    msg.number
                } else {
                    res.append("10")
                    -msg.number
                }

                val width = ceil(log2(num.toFloat())).toInt()

                val encodedWidth = if (width % 4 == 0) {
                    width
                } else {
                    4 * ((width / 4) + 1)
                }

                res.append("1".repeat(encodedWidth / 4))
                res.append("0")

                res.append(num.toString(2).padStart(encodedWidth, '0'))

                return res.toString()
            }
            is Cons -> {
                val res = StringBuilder()

                res.append("11")

                res.append(encode(msg.car))
                res.append(encode(msg.cdr))

                return res.toString()
            }
            is Nil -> {
                return "00"
            }
            else -> {
                TODO("Cannot convert $msg")
            }
        }
    }
}
