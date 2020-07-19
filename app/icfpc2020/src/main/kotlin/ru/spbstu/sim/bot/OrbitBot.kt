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
            if (isAbovePlanet(ship.position, mapState.planeRadius)) {
                val truncVelocity = ship.velocity * gravity
                if (truncVelocity.x < ACCELERATION && truncVelocity.y < ACCELERATION) {
                    acceleration -= gravity
                }
            } else {
                val velocity = ship.velocity + gravity
                acceleration += if (abs(velocity.x) < mapState.planeRadius) {
                    Coordinates(gravity.x, 0)
                } else {
                    Coordinates(-gravity.x, 0)
                }
                acceleration += if (abs(velocity.y) < mapState.planeRadius) {
                    Coordinates(0, gravity.y)
                } else {
                    Coordinates(0, -gravity.y)
                }
            }

            when {
                acceleration.isZero() -> emptyList()
                else -> listOf(ShipCommand.Accelerate(ship.id, -acceleration))
            }
        }
    }
}