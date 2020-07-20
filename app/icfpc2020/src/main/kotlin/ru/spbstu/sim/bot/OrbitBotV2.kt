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
                val orbitDuration = minOf(map.planeRadius, map.tickLimit - game.tick)
                if (isDieAfter(orbitDuration, ship, map)) {
                    add(ShipCommand.Accelerate(ship.id, -accelerate(ship)))
                }
            }
        }
    }

    fun isDieAfter(ticks: Long, ship: GameShip, map: MapState): Boolean {
        val planetRadius = map.planeRadius
        var position = ship.position
        var velocity = ship.velocity
        if (isInsidePlanet(position, planetRadius)) return true
        for (i in 1..ticks) {
            position += velocity
            velocity -= gravity(position)
            if (isInsidePlanet(position, planetRadius)) return true
        }
        return false
    }

    fun isInsidePlanet(position: Coordinates, planetRadius: Long): Boolean {
        return abs(position.x) <= planetRadius || abs(position.y) <= planetRadius
    }

    fun accelerate(ship: GameShip): Coordinates {
        val position = ship.position
        val velocity = ship.velocity
        val gravity = gravity(position)

        return when {
            gravity.x != 0L ->
                when (velocity.y) {
                    0L -> Coordinates(gravity.x, -position.y / abs(position.y))
                    else -> Coordinates(gravity.x, -velocity.y / abs(velocity.y))
                }
            gravity.y != 0L ->
                when (velocity.x) {
                    0L -> Coordinates(-position.x / abs(position.x), gravity.y)
                    else -> Coordinates(-velocity.x / abs(velocity.x), gravity.y)
                }
            else -> TODO()
        }
    }
}