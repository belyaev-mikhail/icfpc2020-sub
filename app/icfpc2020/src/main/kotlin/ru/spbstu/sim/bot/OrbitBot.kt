package ru.spbstu.sim.bot

import ru.spbstu.sim.MapState
import ru.spbstu.sim.ShipCommand
import ru.spbstu.sim.ShipState

open class OrbitBot : AbstractBot() {
    override fun initialShipState(mapState: MapState) = ShipState(300, 0, 8, 1)

    init {
        step { ship, gameState, mapState ->
            val velocity = gravity(ship.position)
            listOf(ShipCommand.Accelerate(ship.id, velocity))
        }
    }
}