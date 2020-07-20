package ru.spbstu.sim.bot

import ru.spbstu.sim.Coordinates
import ru.spbstu.sim.MapState
import ru.spbstu.sim.ShipCommand
import ru.spbstu.sim.ShipState
import kotlin.math.abs

private val Coordinates.isDiagonal get() = abs(x) == abs(y)

private fun Coordinates.getSection(mapState: MapState): Int = when {
    x <= -mapState.planeRadius -> when {
        abs(y) <= mapState.planeRadius -> 1
        y >= mapState.planeRadius -> if (-x >= y) 2 else 3
        y <= -mapState.planeRadius -> if (-x >= -y) 12 else 11
        else -> TODO()
    }
    abs(x) <= mapState.planeRadius -> when {
        y >= mapState.planeRadius -> 4
        y <= -mapState.planeRadius -> 10
        else -> TODO()
    }
    x >= mapState.planeRadius -> when {
        abs(y) <= mapState.planeRadius -> 7
        y >= mapState.planeRadius -> if (x >= y) 6 else 5
        y <= -mapState.planeRadius -> if (x >= -y) 8 else 9
        else -> TODO()
    }
    else -> TODO()
}

private fun Coordinates.getFlightDirection(section: Int): Coordinates = when (section) {
    1, 7 -> if (y < 0) Coordinates(0, 1) else Coordinates(0, -1)
    2, 6 -> Coordinates(0, -1)
    3, 12 -> Coordinates(1, 0)
    4, 10 -> if (x < 0) Coordinates(1, 0) else Coordinates(-1, 0)
    5, 8 -> Coordinates(-1, 0)
    9, 11 -> Coordinates(0, 1)
    else -> TODO()
}

private fun Coordinates.getGravityPull(section: Int): Coordinates = when (section) {
    1 -> Coordinates(1, 0)
    4 -> Coordinates(0, -1)
    7 -> Coordinates(-1, 0)
    10 -> Coordinates(0, 1)
    2, 3 -> if (this.isDiagonal) Coordinates(1, -1) else Coordinates(0, 0)
    5, 6 -> if (this.isDiagonal) Coordinates(-1, -1) else Coordinates(0, 0)
    8, 9 -> if (this.isDiagonal) Coordinates(-1, 1) else Coordinates(0, 0)
    11, 12 -> if (this.isDiagonal) Coordinates(1, 1) else Coordinates(0, 0)
    else -> Coordinates(0, 0)
}


class FloatingBot : AbstractBot() {
    override fun initialShipState(mapState: MapState) = ShipState(0, 0, 0, 0)
    lateinit var move: Coordinates

    init {
        step { gameShip, gameState, mapState, list ->
            val currentTurn = gameState.tick.toInt()
            val section = gameShip.position.getSection(mapState)

            if (currentTurn == 0) {
                move = -gameShip.position.getFlightDirection(section)
            }

            if (currentTurn < 5) {
                val gravityFix = gameShip.position.getGravityPull(section)
                listOf(ShipCommand.Accelerate(gameShip.id, move * 2 + gravityFix))
            } else {
                listOf()
            }
        }
    }
}