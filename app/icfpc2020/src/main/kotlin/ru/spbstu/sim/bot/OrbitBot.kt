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
            val position = ship.position
            val planetRadius = mapState.planeRadius
            val gravity = gravity(ship.position)
            var acceleration = Coordinates(0, 0)
            val truncVelocity = ship.velocity * gravity
            val isCompensateX = abs(position.y) < planetRadius && truncVelocity.x < ACCELERATION
            val isCompensateY = abs(position.x) < planetRadius && truncVelocity.y < ACCELERATION
            val isCompensateXY = abs(position.x) == abs(position.y) &&
                    (truncVelocity.x < ACCELERATION && truncVelocity.y < ACCELERATION)
            when {
                isCompensateX || isCompensateXY -> acceleration += Coordinates(gravity.x, 0)
                abs(truncVelocity.x) < 2 * ACCELERATION -> acceleration += Coordinates(gravity.x, 0)
                abs(truncVelocity.x) > 5 * ACCELERATION -> acceleration += Coordinates(-gravity.x, 0)
            }
            when {
                isCompensateY -> acceleration += Coordinates(0, gravity.y)
                abs(truncVelocity.y) < 2 * ACCELERATION -> acceleration += Coordinates(0, gravity.y)
                abs(truncVelocity.y) > 5 * ACCELERATION -> acceleration += Coordinates(0, -gravity.y)
            }

            println("---------------------------")
            println("tick=${gameState.tick} " +
                    "velocity=${ship.velocity} " +
                    "truncVelocity=$truncVelocity " +
                    "gravity=$gravity" +
                    "isCompensateX=$isCompensateX " +
                    "isCompensateY=$isCompensateY " +
                    "isCompensateXY=$isCompensateXY ")

            when {
                acceleration.isZero() -> emptyList()
                else -> listOf(ShipCommand.Accelerate(ship.id, -acceleration))
            }
        }
    }
}