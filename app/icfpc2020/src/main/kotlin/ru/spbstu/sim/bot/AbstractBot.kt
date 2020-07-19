package ru.spbstu.sim.bot

import ru.spbstu.sim.*

typealias StepAction = (GameShip, GameState, MapState, List<ShipCommand>) -> List<ShipCommand>

abstract class AbstractBot : Bot {
    private val ships = HashMap<Long, Int>()
    private val stepActions = HashMap<Int?, MutableList<StepAction>>()

    override fun step(gameState: GameState, mapState: MapState): List<ShipCommand> {
        val allyShips = gameState.ships.filter { mapState.role == it.role }
        for ((ship) in allyShips) {
            if (ship !in ships) {
                ships[ship] = ships.size
            }
        }
        val commands = ArrayList<ShipCommand>()
        for (ship in allyShips) {
            val shipOrderNumber = ships[ship.id]!!
            stepActions[null]?.forEach { commands.addAll(it(ship, gameState, mapState, commands)) }
            stepActions[shipOrderNumber]?.forEach { commands.addAll(it(ship, gameState, mapState, commands)) }
        }
        return commands
    }

    protected fun step(shipOrderNumber: Int? = null, action: StepAction) {
        val tickActions = stepActions.getOrPut(shipOrderNumber) { ArrayList() }
        tickActions.add(action)
    }
}