package ru.spbstu.sim.bot

import ru.spbstu.sim.*

class FollowerBot : AbstractBot() {
    override fun initialShipState(mapState: MapState) = ShipState(300, 0, 8, 1)

    init {
        step { ship, state, map, _ ->
            val default = ShipCommand.Accelerate(ship.id, -gravity(ship.position))
            val enemy = state.ships.first { it.id != ship.id }

            fun isValid(coords: Coordinates) = coords.abs() < map.planeRadius || coords.abs() > map.spaceRadius

            val target = MutShip(enemy.position, enemy.velocity)
            if(!isValid(target.coords)) return@step listOf(default)
            repeat(5) { if(isValid(target.coords)) target.tick() }

            val sim = OrbitSim(map.planeRadius, map.spaceRadius, state.ships)

            val path = sim.findPath(ship.id, target.coords) ?: return@step listOf(default)
            System.err.println("Found path: $path")
            val pp = path.reversed()

            val accel = pp.getOrNull(1)?.v?.minus(pp[0].v)?.minus(gravity(pp[0].coords))

            when {
                accel == null || accel.isZero() -> listOf(default)
                else -> listOf(ShipCommand.Accelerate(ship.id, accel))
            }
        }
    }
}