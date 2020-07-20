package ru.spbstu.sim.bot

import ru.spbstu.sim.*


class StandingBot : AbstractBot() {
    override fun initialShipState(mapState: MapState) = ShipState(300, 0, 8, 1)

    private var stepModifier = -1

    val Coordinates.randomVector: Coordinates
        get() {
            val reversed = this.swap()
            val newCoords = Coordinates(reversed.x * stepModifier, reversed.y * stepModifier)
            stepModifier *= -1
            return newCoords
        }

    init {
        step { ship, state, map, _ ->

            fun default(): List<ShipCommand> {
                val velocity = gravity(ship.position)
                return listOf(ShipCommand.Accelerate(ship.id, velocity + velocity.randomVector))
            }

            fun acceleration(from: MutShip, to: MutShip) =
                to.v - from.v - gravity(from.coords)

            val sim = OrbitSim(map.planeRadius, map.planeRadius, state.ships.filter { it.role != ship.role })
            sim.tick(1)

            val kamikaze = sim.ships.entries.filter { (id, e) ->
                ship.position.manhattanDist(e.coords) <= state.shipMap[id]!!.explosionRadius
            }.minBy { (_, e) -> ship.position.manhattanDist(e.coords) } ?: return@step default()

            val idealTarget = ship.position + (ship.position - kamikaze.value.coords)
            var target = idealTarget

            if (target.manhattanDist(Coordinates(0, 0)) <= map.planeRadius) {
                target = idealTarget.copy(x = ship.position.x)
            }
            if (target.manhattanDist(Coordinates(0, 0)) <= map.planeRadius) {
                target = idealTarget.copy(y = ship.position.y)
            }
            if (target.manhattanDist(Coordinates(0, 0)) <= map.planeRadius) {
                return@step default()
            }

            val sim2 = OrbitSim(map.planeRadius, map.spaceRadius, state.ships.filter { it.id == ship.id })

            val path = sim2.findPath(ship.id, target) ?: return@step default()
            val pp = path.reversed()

            val accel = when {
                pp.size > 1 -> acceleration(pp[0], pp[1])
                else -> return@step default()
            }

            when {
                accel.isZero() -> default()
                else -> listOf(ShipCommand.Accelerate(ship.id, -accel))
            }
        }
    }
}
