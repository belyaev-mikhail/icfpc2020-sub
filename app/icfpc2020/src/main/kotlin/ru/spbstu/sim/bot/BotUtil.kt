package ru.spbstu.sim.bot

import ru.spbstu.sim.Coordinates
import kotlin.math.abs

const val ACCELERATION = 1L

fun gravity(relTo: Coordinates): Coordinates {
    val (x, y) = relTo
    var res = Coordinates(0, 0)
    if (x > 0 && abs(x) >= abs(y)) res += Coordinates(-ACCELERATION, 0)
    if (x < 0 && abs(x) >= abs(y)) res += Coordinates(ACCELERATION, 0)
    if (y > 0 && abs(x) <= abs(y)) res += Coordinates(0, -ACCELERATION)
    if (y < 0 && abs(x) <= abs(y)) res += Coordinates(0, ACCELERATION)
    return res
}

fun isAbovePlanet(relTo: Coordinates, planetRadius: Long): Boolean {
    return abs(relTo.x) < planetRadius || abs(relTo.y) < planetRadius
}