package ru.spbstu.sim.bot

import ru.spbstu.sim.GameState
import ru.spbstu.sim.MapState
import ru.spbstu.sim.ShipCommand
import ru.spbstu.sim.ShipState

class CompositeBot(val initialShipState: ShipState, vararg val bots: Bot) : Bot {
    override fun prepare(mapState: MapState) {
        bots.forEach { it.prepare(mapState) }
    }

    override fun initialShipState(mapState: MapState): ShipState {
        return initialShipState
    }

    override fun step(gameState: GameState, mapState: MapState): List<ShipCommand> {
        return bots.flatMap { it.step(gameState, mapState) }
    }
}