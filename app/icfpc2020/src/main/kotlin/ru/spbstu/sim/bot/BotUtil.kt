package ru.spbstu.sim.bot

import ru.spbstu.sim.Coordinates
import kotlin.math.abs


fun gravity(relTo: Coordinates): Coordinates {
    val (x, y) = relTo
    var res = Coordinates(0, 0)
    if (x > 0 && abs(x) >= abs(y)) res += Coordinates(-1, 0)
    if (x < 0 && abs(x) >= abs(y)) res += Coordinates(1, 0)
    if (y > 0 && abs(x) <= abs(y)) res += Coordinates(0, -1)
    if (y < 0 && abs(x) <= abs(y)) res += Coordinates(0, 1)
    return res
}

//fun abovePlant(relTo: Coordinates, planetDiameter: Long): Coordinates {
//    val
//}