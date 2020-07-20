package ru.spbstu.sim.bot

import ru.spbstu.sim.*

class HermitBot : AbstractBot() {
    override fun initialShipState(mapState: MapState) = ShipState(300, 0, 8, 1)

    fun acceleration(from: MutShip, to: MutShip) =
            to.v - from.v - gravity(from.coords)

    lateinit var points: List<Coordinates>

    var currPoint: Coordinates? = null

    override fun prepare(mapState: MapState) {
        val whereTo = mapState.spaceRadius - mapState.planeRadius

        points = listOf(
            Coordinates(whereTo, whereTo),
            Coordinates(whereTo, -whereTo),
            Coordinates(-whereTo, -whereTo),
            Coordinates(-whereTo, whereTo)
        )
    }

    init {
        step { ship, state, map, _ ->
            if (currPoint == null) {
                currPoint = points.minBy { ship.position.manhattanDist(it) }
            }

            val currPoint1 = currPoint ?: return@step listOf()

            if (ship.position.manhattanDist(currPoint1) < 4) {
                val idx = (points.indexOf(currPoint1) + 1) % points.size
                currPoint = points[idx]

            }

            val currPoint2 = this.currPoint ?: return@step listOf()

            val default = ShipCommand.Accelerate(ship.id, gravity(ship.position))

            val sim = OrbitSim(map.planeRadius, map.spaceRadius, state.ships)

            val path = sim.findPath(ship.id, currPoint2) ?: return@step listOf(default)
            System.err.println("Found path: $path")
            val pp = path.reversed()

            val accel = when {
                pp.size > 1 -> acceleration(pp[0], pp[1])
                else -> return@step listOf(default)
            }

            when {
                accel.isZero() -> listOf(default)
                else -> listOf(ShipCommand.Accelerate(ship.id, -accel))
            }
        }
    }
}
