package ru.spbstu.sim.bot

import ru.spbstu.sim.*
import kotlin.math.abs

class OrbitBotV2 : AbstractBot() {
    override fun initialShipState(mapState: MapState): ShipState {
        TODO("Not yet implemented")
    }

    init {
        step { ship, game, map, _ ->
            ArrayList<ShipCommand>().apply {
                if (isDieAfter(minOf(map.planeRadius / 2, map.tickLimit - game.tick), ship, map)) {
                    println("${game.tick} DEAD DETECTED")
                    add(ShipCommand.Accelerate(ship.id, accelerate(ship)))
                } else if (isEscapeAfter(minOf(10, map.tickLimit - game.tick), ship, map)) {
                    println("${game.tick} ESCAPE DETECTED")
                }
            }
        }
    }

    private fun anyAfter(ticks: Long, ship: GameShip, check: (Coordinates) -> Boolean): Boolean {
        var position = ship.position
        var velocity = ship.velocity
        if (check(position)) return true
        for (i in 1..ticks) {
            position += velocity
            velocity -= gravity(position)
            if (check(position)) return true
        }
        return false
    }

    private fun isDieAfter(ticks: Long, ship: GameShip, map: MapState): Boolean {
        val planetRadius = map.planeRadius
        return anyAfter(ticks, ship) {
            abs(it.x) <= planetRadius || abs(it.y) <= planetRadius
        }
    }

    private fun isEscapeAfter(ticks: Long, ship: GameShip, map: MapState): Boolean {
        val spaceRadius = map.spaceRadius
        return anyAfter(ticks, ship) {
            abs(it.x) >= spaceRadius || abs(it.y) >= spaceRadius
        }
    }

    private fun accelerate(ship: GameShip): Coordinates {
        val position = ship.position
        val velocity = ship.velocity
        val gravity = gravity(position)

        println("DATA: position=$position, velocity=$velocity, gravity=$gravity")

        return when {
            gravity.x != 0L ->
                when (velocity.y) {
                    0L -> when (position.y) {
                        0L -> Coordinates(-gravity.x, 1)
                        else -> Coordinates(-gravity.x, -position.y / abs(position.y))
                    }
                    else -> Coordinates(-gravity.x, -velocity.y / abs(velocity.y))
                }
            gravity.y != 0L ->
                when (velocity.x) {
                    0L -> when (position.x) {
                        0L -> Coordinates(1, -gravity.y)
                        else -> Coordinates(-position.x / abs(position.x), -gravity.y)
                    }
                    else -> Coordinates(-velocity.x / abs(velocity.x), -gravity.y)
                }
            else -> TODO()
        }
    }
}