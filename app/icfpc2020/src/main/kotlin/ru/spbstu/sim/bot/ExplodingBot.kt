package ru.spbstu.sim.bot

import ru.spbstu.sim.GameShip
import ru.spbstu.sim.MapState
import ru.spbstu.sim.ShipCommand
import ru.spbstu.sim.ShipState

class ExplodingBot : AbstractBot() {
    override fun initialShipState(mapState: MapState) = ShipState(0, 0, 0, 0)

    private val GameShip.explosionRadius: Int get() {
        val sum = state.fuel + state.power + state.coolPerTick + state.numberOfCopies
        return when {
            sum < 1 -> 0
            sum == 1 -> 4
            sum == 2 -> 5
            sum == 3 -> 5
            sum in 4..15 -> 8
            else -> 12
        }
    }

    private val GameShip.explosionDamage: Int get() {
        val sum = state.fuel + state.power + state.coolPerTick + state.numberOfCopies
        return when {
            sum < 1 -> 0
            sum == 1 -> 128
            sum == 2 -> 161
            sum == 3 -> 181
            sum in 4..15 -> 256
            else -> 384
        }
    }

    init {
        step { gameShip, gameState, mapState, list ->
            val enemyShips = gameState.ships.filter { it.role != mapState.role }

            val explosionRadius = gameShip.explosionRadius
            when {
                enemyShips.any { it.position.manhattanDist(gameShip.position) <= explosionRadius } -> {
                    listOf(ShipCommand.Detonate(gameShip.id))
                }
                else -> listOf()
            }
        }
    }

}