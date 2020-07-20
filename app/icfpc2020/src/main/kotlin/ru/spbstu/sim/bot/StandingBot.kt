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

            val sim = OrbitSim(Planet(map.planeRadius.toInt()), state.ships.filter { it.role != ship.role })
            sim.tick(1)

            val kamikaze = sim.ships.entries.filter { (id, e) ->
                ship.position.manhattanDist(e.coords) <= state.shipMap[id]!!.explosionRadius
            }.minBy { (_, e) -> ship.position.manhattanDist(e.coords) }

            if (kamikaze == null) {
                val velocity = gravity(ship.position)
                return@step listOf(ShipCommand.Accelerate(ship.id, velocity + velocity.randomVector))
            }

            var target = ship.position + (ship.position - kamikaze.value.coords)

            if (target.manhattanDist(Coordinates(0, 0)) <= map.planeRadius) {
                target = ship.position + (ship.position - kamikaze.value.coords)
                target = target.copy(x = ship.position.x)
            }
            if (target.manhattanDist(Coordinates(0, 0)) <= map.planeRadius) {
                target = ship.position + (ship.position - kamikaze.value.coords)
                target = target.copy(y = ship.position.y)
            }
            if (target.manhattanDist(Coordinates(0, 0)) <= map.planeRadius) {
                val velocity = gravity(ship.position)
                return@step listOf(ShipCommand.Accelerate(ship.id, velocity + velocity.randomVector))
            }

            val path = sim.findPath(ship.id, target) ?: return@step listOf()
            val pp = path.reversed()
            val accel = pp[1].v - pp[0].v

            if (!accel.isZero()) listOf() else listOf(ShipCommand.Accelerate(ship.id, accel))
        }
    }
}