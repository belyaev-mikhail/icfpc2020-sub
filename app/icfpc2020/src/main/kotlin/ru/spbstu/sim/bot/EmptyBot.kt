package ru.spbstu.sim.bot

import ru.spbstu.sim.GameState
import ru.spbstu.sim.MapState
import ru.spbstu.sim.ShipCommand
import ru.spbstu.sim.ShipState

class EmptyBot(val shipState: ShipState) : Bot {
    override fun initialShipState(gameState: GameState, mapState: MapState): ShipState = shipState
    override fun step(gameState: GameState, mapState: MapState): List<ShipCommand> = listOf()
}