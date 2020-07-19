package ru.spbstu.sim.bot

import ru.spbstu.sim.*

typealias StepAction = (GameShip, GameState, MapState) -> List<ShipCommand>

abstract class AbstractBot : Bot {
    private val ships = HashMap<Long, Int>()
    private val stepActions = HashMap<Int?, MutableList<StepAction>>()

    override fun step(gameState: GameState, mapState: MapState): List<ShipCommand> {
        registerShips(gameState)
        val commands = ArrayList<ShipCommand>()
        for (ship in gameState.ships) {
            val shipOrderNumber = ships[ship.id]!!
            stepActions[null]?.forEach { commands.addAll(it(ship, gameState, mapState)) }
            stepActions[shipOrderNumber]?.forEach { commands.addAll(it(ship, gameState, mapState)) }
        }
        return commands
    }

    private fun registerShips(gameState: GameState) {
        for ((ship) in gameState.ships) {
            if (ship !in ships) {
                ships[ship] = ships.size
            }
        }
    }

    protected fun step(shipOrderNumber: Int? = null, action: StepAction) {
        val tickActions = stepActions.getOrPut(shipOrderNumber) { ArrayList() }
        tickActions.add(action)
    }
}