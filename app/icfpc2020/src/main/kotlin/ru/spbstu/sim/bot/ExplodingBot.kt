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
            sum in 3..14 -> 5
            sum in 15..510 -> 8
            else -> 12
        }
    }

    private val GameShip.explosionDamage: Int get() {
        val sum = state.fuel + state.power + state.coolPerTick + state.numberOfCopies
        return when {
            sum < 1 -> 0
            sum == 1 -> 128
            sum == 2 -> 161
            sum in 3..14 -> 181
            sum in 15..510 -> 256
            else -> 384
        }
    }

    private val GameShip.approximateHealth: Int get() {
        var health = state.sum()
        health += (this.maxHeatingLevel - this.heatLevel + this.state.coolPerTick.toLong()).toInt()
        return health
    }

    private val GameShip.isShrapnel: Boolean get() = this.state.sum() <= 3
    private fun GameShip.canReach(other: GameShip) = other.position.manhattanDist(this.position) <= this.explosionRadius

    init {
        step { gameShip, gameState, mapState, _ ->
            val enemyShips = gameState.ships.filter { it.role != mapState.role }
            when {
                gameShip.isShrapnel -> {
                    when {
                        enemyShips.any { gameShip.canReach(it) } -> listOf(ShipCommand.Detonate(gameShip.id))
                        else -> listOf()
                    }
                }
                else -> {
                    if (enemyShips.size != 1) return@step listOf()
                    val enemy = enemyShips.first()
                    when {
                        gameShip.canReach(enemy) && enemy.approximateHealth <= gameShip.explosionDamage -> listOf(ShipCommand.Detonate(gameShip.id))
                        else -> listOf()
                    }
                }
            }
        }
    }

}