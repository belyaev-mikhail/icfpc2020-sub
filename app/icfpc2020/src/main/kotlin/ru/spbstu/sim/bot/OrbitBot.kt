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
            val gravity = gravity(ship.position)
            var acceleration = Coordinates(0, 0)
            val nextVelocity = ship.velocity + gravity
            val truncVelocity = ship.velocity * gravity
            val isAbovePlanet = isAbovePlanet(ship.position, mapState.planeRadius)
            val isCompensateX = isAbovePlanet && truncVelocity.x < ACCELERATION
            val isCompensateY = isAbovePlanet && truncVelocity.y < ACCELERATION
            when {
                isCompensateX -> acceleration += Coordinates(-gravity.x, 0)
                abs(nextVelocity.x) < mapState.planeRadius / 2 -> acceleration += Coordinates(gravity.x, 0)
                abs(nextVelocity.x) > mapState.planeRadius -> acceleration += Coordinates(-gravity.x, 0)
            }
            when {
                isCompensateY -> acceleration += Coordinates(0, -gravity.y)
                abs(nextVelocity.y) < mapState.planeRadius / 2 -> acceleration += Coordinates(0, gravity.y)
                abs(nextVelocity.y) > mapState.planeRadius -> acceleration += Coordinates(0, -gravity.y)
            }
            println("$isCompensateX $isCompensateY $isAbovePlanet ${ship.velocity} ${gravity}")

            when {
                acceleration.isZero() -> emptyList()
                else -> listOf(ShipCommand.Accelerate(ship.id, -acceleration))
            }
        }
    }
}