package ru.spbstu.sim.bot

import ru.spbstu.sim.Coordinates
import ru.spbstu.sim.MapState
import ru.spbstu.sim.ShipCommand
import ru.spbstu.sim.ShipState


class JumpingBot : AbstractBot() {
    override fun initialShipState(mapState: MapState) = ShipState(300, 0, 8, 1)

    private var stepModifier = -1

    val Coordinates.randomVector: Coordinates
        get() {
        val reversed = this.swap()
        val jumpLength = listOf(1, 2).random()
        val newCoords = Coordinates(reversed.x * stepModifier * jumpLength, reversed.y * stepModifier * jumpLength)
        stepModifier *= -1
        return newCoords
    }

    init {
        step { ship, _, _, _ ->
            val velocity = gravity(ship.position)
            listOf(ShipCommand.Accelerate(ship.id, velocity + velocity.randomVector))
        }
    }
}