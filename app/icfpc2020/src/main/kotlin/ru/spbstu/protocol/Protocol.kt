package ru.spbstu.protocol

import ru.spbstu.data.Cons
import ru.spbstu.data.Datum
import ru.spbstu.data.Nil
import ru.spbstu.data.Num
import kotlin.math.ceil
import kotlin.math.log2

class Protocol {

    private var currIndex = 0

    fun reset() {
        currIndex = 0
    }

    fun decode(msg: String): Datum {
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

                if (width == 0) return Num(0)

                val num = msg.substring(currIndex, currIndex + 4 * width).toInt(2)

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

    fun encode(msg: Datum): String {
        when (msg) {
            is Num -> {
                if (msg.value == 0) {
                    return "010"
                }

                val res = StringBuilder()

                val num = if (msg.value > 0) {
                    res.append("01")
                    msg.value
                } else {
                    res.append("10")
                    -msg.value
                }

                val width = ceil(log2(num.toFloat())).toInt()

                val encodedWidth = if (width % 4 == 0) {
                    width
                } else {
                    4 * ((width / 4) + 1)
                }

                res.append("1".repeat(encodedWidth))
                res.append("0")

                res.append(num.toString(2).padStart(encodedWidth - width, '0'))

                return res.toString()
            }
            is Cons -> {
                val res = StringBuilder()

                res.append("11")

                res.append(encode(msg.head))
                res.append(encode(msg.tail))

                return res.toString()
            }
            is Nil -> {
                return "00"
            }
        }
    }
}
