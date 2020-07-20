package ru.spbstu.sim.bot

import ru.spbstu.sim.*

class FollowerBot : AbstractBot() {
    override fun initialShipState(mapState: MapState) = ShipState(300, 0, 8, 1)

    fun acceleration(from: MutShip, to: MutShip) =
            to.v - from.v - gravity(from.coords)

    init {
        step { ship, state, map, _ ->
            val default = ShipCommand.Accelerate(ship.id, -gravity(ship.position))
            val enemy = state.ships.first { it.id != ship.id }

            fun isValid(coords: Coordinates) = coords.abs() > map.planeRadius && coords.abs() < map.spaceRadius

            val target = MutShip(enemy.position, enemy.velocity)
            var tCoords = target.coords
            if(!isValid(tCoords)) return@step listOf(default)
            repeat(5) {
                target.tick()
                if(isValid(target.coords)) tCoords = target.coords
            }

            val sim = OrbitSim(map.planeRadius, map.spaceRadius, state.ships)

            val path = sim.findPath(ship.id, tCoords) ?: return@step listOf(default)
            System.err.println("Found path: $path")
            val pp = path.reversed()

            val accel = when {
                pp.size > 1 -> acceleration(pp[0], pp[1])
                else -> return@step listOf(default)
            }

            when {
                accel.isZero() -> listOf(default)
                else -> listOf(ShipCommand.Accelerate(ship.id, accel))
            }
        }
    }
}