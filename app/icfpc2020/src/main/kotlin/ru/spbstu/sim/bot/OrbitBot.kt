package ru.spbstu.sim.bot

import ru.spbstu.sim.Coordinates
import ru.spbstu.sim.MapState
import ru.spbstu.sim.ShipCommand
import ru.spbstu.sim.ShipState
import kotlin.math.abs

class OrbitBot : AbstractBot() {
    override fun initialShipState(mapState: MapState) = ShipState(300, 0, 8, 1)

    init {
        step { ship, gameState, mapState ->
            val velocity = ship.velocity
            val position = ship.position
            val nextPosition = position + velocity
            val gravity = gravity(nextPosition)
            val nextVelocity = velocity + gravity
            val planetRadius = mapState.planeRadius
            var acceleration = Coordinates(0, 0)
            val truncVelocity = nextVelocity * gravity
            val isCompensateX = abs(nextPosition.y) < planetRadius && truncVelocity.x > 0
            val isCompensateY = abs(nextPosition.x) < planetRadius && truncVelocity.y > 0
            val isCompensateXY = abs(nextPosition.x) == abs(nextPosition.y) && (truncVelocity.x > 0 && truncVelocity.y > 0)
            when {
                isCompensateX || isCompensateXY -> acceleration += Coordinates(gravity.x, 0)
                abs(nextVelocity.x) == 0L -> acceleration += Coordinates(1, 0)
                abs(nextVelocity.x) < 2 * ACCELERATION -> acceleration += Coordinates(nextVelocity.x / abs(nextVelocity.x), 0)
                abs(nextVelocity.x) > 5 * ACCELERATION -> acceleration += Coordinates(-nextVelocity.x / abs(nextVelocity.x), 0)
            }
            when {
                isCompensateY -> acceleration += Coordinates(0, gravity.y)
                abs(nextVelocity.y) == 0L -> acceleration += Coordinates(0, 1)
                abs(nextVelocity.y) < 2 * ACCELERATION -> acceleration += Coordinates(0, nextVelocity.y / abs(nextVelocity.y))
                abs(nextVelocity.y) > 5 * ACCELERATION -> acceleration += Coordinates(0, -nextVelocity.y / abs(nextVelocity.y))
            }

            println("---------------------------")
            println("tick=${gameState.tick} " +
                    "position=$position " +
                    "nextPosition=$nextPosition " +
                    "velocity=$velocity " +
                    "nextVelocity=$nextVelocity " +
                    "truncVelocity=$truncVelocity " +
                    "gravity=$gravity" +
                    "isCompensateX=$isCompensateX " +
                    "isCompensateY=$isCompensateY " +
                    "isCompensateXY=$isCompensateXY ")

            when {
                acceleration.isZero() -> emptyList()
                else -> listOf(ShipCommand.Accelerate(ship.id, acceleration))
            }
        }
    }
}