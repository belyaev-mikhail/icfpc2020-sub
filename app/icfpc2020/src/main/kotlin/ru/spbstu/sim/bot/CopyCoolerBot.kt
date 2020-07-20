package ru.spbstu.sim.bot

import ru.spbstu.sim.MapState
import ru.spbstu.sim.ShipCommand
import ru.spbstu.sim.ShipState

class CopyCoolerBot : AbstractBot() {
    init {
        step { ship, gameState, mapState, prevCommands ->
            fun default(): List<ShipCommand> = listOf()

            val additionalHeat = prevCommands.fold(0L) { acc, cmd ->
                when (cmd) {
                    is ShipCommand.Accelerate -> acc + 8
                    is ShipCommand.Shoot -> acc + cmd.power
                    else -> acc
                }
            }

            if (ship.heatLevel + additionalHeat - ship.state.coolPerTick < ship.maxHeatingLevel - 10)
                return@step default()

            if (ship.state.numberOfCopies == 1)
                return@step default()

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
