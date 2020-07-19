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
                abs(velocity.x) == 0L -> acceleration += Coordinates(1, 0)
                isCompensateY && abs(velocity.x) < planetRadius / 2 -> acceleration += Coordinates(-velocity.x / abs(velocity.x), 0)
//                abs(velocity.x) > planetRadius -> acceleration += Coordinates(velocity.x / abs(velocity.x), 0)
            }
            when {
                isCompensateY -> acceleration += Coordinates(0, gravity.y)
                abs(velocity.y) == 0L -> acceleration += Coordinates(0, 1)
                isCompensateX && abs(velocity.y) < planetRadius / 2 -> acceleration += Coordinates(0, -velocity.y / abs(velocity.y))
//                abs(velocity.y) > planetRadius -> acceleration += Coordinates(0, velocity.y / abs(velocity.y))
            }

            println("---------------------------")
            println("tick=${gameState.tick}\n" +
                    "position=$position " +
                    "nextPosition=$nextPosition\n" +
                    "velocity=$velocity " +
                    "nextVelocity=$nextVelocity " +
                    "truncVelocity=$truncVelocity\n" +
                    "acceleration=$acceleration " +
                    "gravity=$gravity\n" +
                    "isCompensateX=$isCompensateX " +
                    "isCompensateY=$isCompensateY " +
                    "isCompensateXY=$isCompensateXY")

            when {
                acceleration.isZero() -> emptyList()
                else -> listOf(ShipCommand.Accelerate(ship.id, acceleration))
            }
        }
    }
}