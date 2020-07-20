package ru.spbstu.sim.bot

import ru.spbstu.sim.*

class FollowerBot : AbstractBot() {
    override fun initialShipState(mapState: MapState) = ShipState(300, 0, 8, 1)

    init {
        step { ship, state, map, _ ->
            val enemy = state.ships.first { it.id != ship.id }

            val target = MutShip(enemy.position, enemy.velocity).apply { tick(5) }.coords

            val sim = OrbitSim(Planet(map.planeRadius.toInt()), state.ships)

            val path = sim.findPath(ship.id, target) ?: return@step listOf()
            val pp = path.reversed()

            val accel = pp[1].v - pp[0].v

            if(!accel.isZero()) listOf() else listOf(ShipCommand.Accelerate(ship.id, accel))
        }
    }
}