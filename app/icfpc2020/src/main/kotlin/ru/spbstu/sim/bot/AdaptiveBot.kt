package ru.spbstu.sim.bot

import ru.spbstu.sim.*

class AdaptiveBot (val attackerBot: Bot, val defenderBot: Bot): Bot {
    lateinit var currentBot: Bot
    override fun initialShipState(mapState: MapState): ShipState {
        currentBot = when (mapState.role) {
            GameRole.DEFENDER -> defenderBot
            GameRole.ATTACKER -> attackerBot
        }
        return currentBot.initialShipState(mapState)
    }

    override fun step(gameState: GameState, mapState: MapState) = currentBot.step(gameState, mapState)
}