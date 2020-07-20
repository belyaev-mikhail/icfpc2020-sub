package ru.spbstu.sim.bot

import ru.spbstu.sim.*
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


val GameShip.nextApproximatePosition: Coordinates
    get() {
        var approximatePosition = position
        approximatePosition += velocity
        approximatePosition += gravity(position)
        for (moveCommand in this.commands.filterIsInstance<ShipCommand.Accelerate>()) {
            approximatePosition += moveCommand.velocity
        }
        return approximatePosition
    }


fun GameShip.nextPosition(commands: List<ShipCommand>): Coordinates {
    var newPos = position
    newPos += velocity
    newPos += gravity(position)
    for (moveCommand in commands.filterIsInstance<ShipCommand.Accelerate>()) {
        newPos += moveCommand.velocity
    }
    return newPos
}