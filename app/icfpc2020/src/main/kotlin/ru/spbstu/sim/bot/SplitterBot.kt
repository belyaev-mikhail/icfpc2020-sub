package ru.spbstu.sim.bot

import ru.spbstu.sim.MapState
import ru.spbstu.sim.ShipCommand
import ru.spbstu.sim.ShipState

class SplitterBot : AbstractBot() {
    init {
        val splitAfter = (0..10).random()

        step { ship, gameState, mapState ->
            if (gameState.tick < splitAfter) return@step listOf()

            if (ship.state.numberOfCopies == 1) return@step listOf()

            val (fuel, power, cooling, numCopies) = ship.state

            return@step listOf(
                ShipCommand.Split(
                    ship.id,
                    ShipState(fuel / 2, power / 2, cooling / 2, numCopies / 2)
                )
            )
        }
    }

    override fun initialShipState(mapState: MapState): ShipState {
        TODO("Not yet implemented")
    }
}
