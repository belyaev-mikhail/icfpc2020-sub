package ru.spbstu.sim.bot

import ru.spbstu.sim.*

class FollowerBot : AbstractBot() {
    override fun initialShipState(mapState: MapState) = ShipState(300, 0, 8, 1)

    init {
        step { ship, state, map, _ ->
            val enemy = state.ships.first { it.id != ship.id }

            val target = MutShip(enemy.position, enemy.velocity).apply { tick(2) }.coords
            if(target.abs() < map.planeRadius || target.abs() > map.spaceRadius) return@step listOf()

            val sim = OrbitSim(map.planeRadius, map.spaceRadius, state.ships)

            val path = sim.findPath(ship.id, target) ?: return@step listOf()
            System.err.println("Found path: $path")
            val pp = path.reversed()

            val accel = pp.getOrNull(1)?.v?.minus(pp[0].v)

            when {
                accel == null || accel.isZero() -> listOf()
                else -> listOf(ShipCommand.Accelerate(ship.id, accel))
            }
        }
    }
}