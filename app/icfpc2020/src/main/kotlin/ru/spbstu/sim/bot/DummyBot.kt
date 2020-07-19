package ru.spbstu.sim.bot

import ru.spbstu.sim.*

class DummyBot : Bot {
    override fun initialShipState(mapState: MapState): ShipState = ShipState(200, 20, 10, 3)

    override fun step(gameState: GameState, mapState: MapState): List<ShipCommand> {
        val myRole = mapState.role
        val me = gameState.ships.first { it.role == myRole }
        return listOf(ShipCommand.Accelerate(me.id, Coordinates(-1, 1)))
    }
}