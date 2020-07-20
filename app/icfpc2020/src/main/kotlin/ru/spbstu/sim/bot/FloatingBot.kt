package ru.spbstu.sim.bot

import ru.spbstu.sim.*
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
    private val simSteps = 5

    init {
        step { gameShip, gameState, mapState, list ->
            val planet = Planet(mapState.planeRadius.toInt())
            val currentTurn = gameState.tick.toInt()
            val section = gameShip.position.getSection(mapState)
            System.err.println("Section $section")

            var move = Coordinates(0, 0)

            if (currentTurn == 0 && isAbovePlanet(gameShip.position, planet.radius.toLong())) {
                move += -gameShip.position.getFlightDirection(section)
                System.err.println("Standing above planet, adding move to $move")
            }

            val futureSteps = OrbitSim(planet, listOf(gameShip)).simulateFor(gameShip.id, simSteps)
            System.err.println(futureSteps)
            val willCrash = futureSteps.any { it.coords in planet }

            if (willCrash) {
                move += gravity(gameShip.position)
                System.err.println("Will crash in 5 turns, adding move to $move")
            }

            when (move) {
                Coordinates(0, 0) -> listOf()
                else -> listOf(ShipCommand.Accelerate(gameShip.id, move))
            }
        }
    }
}