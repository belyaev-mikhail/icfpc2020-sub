package ru.spbstu.sim.bot

import ru.spbstu.sim.*
import java.lang.Long.min
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

class ShootingBot(val initialShipState: ShipState, val eps: Double) : AbstractBot() {
    constructor(eps: Double) : this(ShipState(0, 0, 0, 1), eps)

    private fun Coordinates.angle(other: Coordinates) =
        atan2((this.y - other.y).toDouble(), (this.x - other.x).toDouble())

    private fun isValidAngle(angle: Double): Boolean {
        val normalizedAngle = abs(angle % (PI / 2))
        return when {
            abs(normalizedAngle - PI / 2) <= eps -> true
            abs(normalizedAngle - PI / 4) <= eps -> true
            else -> false
        }
    }

    private val GameShip.nextApproximatePosition: Coordinates
        get() {
            var approximatePosition = position
            approximatePosition += velocity
            approximatePosition += gravity(position)
            for (moveCommand in this.commands.filterIsInstance<ShipCommand.Accelerate>()) {
                approximatePosition += moveCommand.velocity
            }
            return approximatePosition
        }

    private fun GameShip.computeNewPos(commands: List<ShipCommand>): Coordinates {
        var newPos = position
        newPos += velocity
        newPos += gravity(position)
        for (moveCommand in commands.filterIsInstance<ShipCommand.Accelerate>()) {
            newPos += moveCommand.velocity
        }
        return newPos
    }

    override fun initialShipState(mapState: MapState) = initialShipState

    init {
        step { ally, gameState, mapState, previousCommands ->
            val currentTurnHeatLevel = previousCommands.map {
                when (it) {
                    is ShipCommand.Accelerate -> 8
                    else -> 0
                }
            }.sum()
            val power = min(
                ally.state.power.toLong(),
                ((ally.maxHeatingLevel - (Num(currentTurnHeatLevel.toLong()) + ally.heatLevel - Num(ally.state.coolPerTick.toLong()))) as Num).number
            )
            if (power <= 0L) return@step listOf()


            val allyCoordinates = ally.computeNewPos(previousCommands)

            val role = mapState.role
            val enemyShip = gameState.ships.filter { it.role != role }.firstOrNull { enemy ->
                val enemyCoordinates = enemy.nextApproximatePosition

                val angle = allyCoordinates.angle(enemyCoordinates)
                isValidAngle(angle) && (enemy.state.coolPerTick < power)
            } ?: return@step listOf()

            listOf(ShipCommand.Shoot(ally.id, enemyShip.nextApproximatePosition, power))
        }
    }
}