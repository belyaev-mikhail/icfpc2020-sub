package ru.spbstu.sim.bot

import ru.spbstu.sim.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

class ShootingBot(val initialShipState: ShipState) : AbstractBot() {
    constructor() : this(ShipState(0, 0, 0, 1))

    private fun Coordinates.angle(other: Coordinates) = atan2((this.y - other.y).toDouble(), (this.x - other.x).toDouble())

    private fun isValidAngle(angle: Double): Boolean {
        val eps = 0.001
        return when {
            abs(angle - PI / 2) <= eps -> true
            abs(angle - PI / 4) <= eps -> true
            else -> false
        }
    }

    private val GameShip.nextApproximatePosition: Coordinates
        get() {
            val current = position
            val velocity = velocity
            val gravityApprox = gravity(current)
            return current + velocity + gravityApprox
        }

    override fun initialShipState(mapState: MapState) = initialShipState

    init {
        step { ally, gameState, mapState ->
            val allyCoordinates = ally.nextApproximatePosition

            val role = mapState.role
            val enemyShip = gameState.ships.filter { it.role != role }.firstOrNull() { enemy ->
                val enemyCoordinates = enemy.nextApproximatePosition

                val angle = allyCoordinates.angle(enemyCoordinates)
                isValidAngle(angle)
            } ?: return@step listOf()

            listOf(ShipCommand.Shoot(ally.id, enemyShip.nextApproximatePosition, ally.state.power.toLong()))
        }
    }
}