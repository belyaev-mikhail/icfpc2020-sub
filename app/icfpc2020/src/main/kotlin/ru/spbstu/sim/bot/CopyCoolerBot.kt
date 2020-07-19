package ru.spbstu.sim.bot

import ru.spbstu.sim.MapState
import ru.spbstu.sim.ShipCommand
import ru.spbstu.sim.ShipState

class CopyCoolerBot : AbstractBot() {
    init {
        step { ship, gameState, mapState, prevCommands ->
            val additionalCooling = prevCommands.fold(0L) { acc, cmd ->
                when (cmd) {
                    is ShipCommand.Accelerate -> acc + 8
                    is ShipCommand.Shoot -> acc + cmd.power
                    else -> acc
                }
            }

            if (ship.heatLevel + additionalCooling < ship.maxHeatingLevel) return@step listOf()

            if (ship.state.numberOfCopies == 1) return@step listOf()

            val (fuel, power, cooling, clones) = ship.state

            return@step listOf(
                ShipCommand.Split(
                    ship.id,
                    ShipState(fuel, power, cooling, clones)
                )
            )
        }
    }

    override fun initialShipState(mapState: MapState): ShipState {
        TODO("Not yet implemented")
    }
}
