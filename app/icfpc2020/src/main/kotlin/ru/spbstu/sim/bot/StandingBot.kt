package ru.spbstu.sim.bot

import ru.spbstu.sim.Coordinates
import ru.spbstu.sim.MapState
import ru.spbstu.sim.ShipCommand
import ru.spbstu.sim.ShipState


class StandingBot : AbstractBot() {
    override fun initialShipState(mapState: MapState) = ShipState(300, 0, 8, 1)

    private var stepModifier = -1

    val Coordinates.randomVector: Coordinates get() {
        val reversed = this.swap()
        val newCoords = Coordinates(reversed.x * stepModifier, reversed.y * stepModifier)
        stepModifier *= -1
        return newCoords
    }

    init {
        step { ship, _, _ ->
            val velocity = gravity(ship.position)
            listOf(ShipCommand.Accelerate(ship.id, velocity + velocity.randomVector))
        }
    }
}