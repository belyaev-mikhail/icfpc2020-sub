package ru.spbstu.sim.bot

import ru.spbstu.sim.MapState
import ru.spbstu.sim.ShipCommand
import ru.spbstu.sim.ShipState

class FloatingBot : AbstractBot() {
    override fun initialShipState(mapState: MapState) = ShipState(0, 0, 0, 0)
    private var currentTurn = 0

    init {
        step { gameShip, gameState, mapState, list ->
            ++currentTurn
            if (currentTurn < 5) {
                when {
                    isAbovePlanet(gameShip.position, mapState.planeRadius) -> {
                        val gravity = -gravity(gameShip.position)
                        val reverseGravity = gravity.swap()
                        listOf(ShipCommand.Accelerate(gameShip.id, reverseGravity * 2 + gravity))
                    }
                    else -> {
                        val acceleration = -gravity(gameShip.position) * 2
                        listOf(ShipCommand.Accelerate(gameShip.id, acceleration))
                    }
                }
            } else {
                listOf()
            }
        }
    }
}