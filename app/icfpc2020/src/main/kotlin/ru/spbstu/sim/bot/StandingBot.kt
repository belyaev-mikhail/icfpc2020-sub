package ru.spbstu.sim.bot

import ru.spbstu.sim.MapState
import ru.spbstu.sim.ShipCommand
import ru.spbstu.sim.ShipState


class StandingBot : AbstractBot() {
    override fun initialShipState(mapState: MapState) = ShipState(300, 0, 8, 1)

    init {
        step { ship, _, _ ->
            val velocity = gravity(ship.position)
            val reversedGravity = gravity(ship.position.swap())
            listOf(ShipCommand.Accelerate(ship.id, velocity + reversedGravity))
        }
    }
}