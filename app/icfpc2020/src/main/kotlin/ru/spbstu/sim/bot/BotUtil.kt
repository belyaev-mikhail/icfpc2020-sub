package ru.spbstu.sim.bot

import ru.spbstu.sim.Coordinates
import ru.spbstu.sim.GameShip
import ru.spbstu.sim.Num
import ru.spbstu.sim.Symbol
import kotlin.math.abs

const val ACCELERATION = 1L

fun Symbol.long(): Long = (this as Num).number

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

val GameShip.explosionRadius: Int
    get() {
        val sum = state.fuel + state.power + state.coolPerTick + state.numberOfCopies
        return when {
            sum < 1 -> 0
            sum == 1 -> 4
            sum == 2 -> 5
            sum in 3..14 -> 5
            sum in 15..510 -> 8
            else -> 12
        }
    }
