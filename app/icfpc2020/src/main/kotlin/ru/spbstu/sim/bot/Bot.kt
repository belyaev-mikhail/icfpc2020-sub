package ru.spbstu.sim.bot

import ru.spbstu.sim.GameState
import ru.spbstu.sim.MapState
import ru.spbstu.sim.ShipCommand
import ru.spbstu.sim.ShipState

interface Bot {
    fun prepare(gameState: GameState, mapState: MapState) {}
    fun initialShipState(gameState: GameState, mapState: MapState): ShipState
    fun step(gameState: GameState, mapState: MapState): List<ShipCommand>
}