package ru.spbstu.sim.bot

import ru.spbstu.sim.*
import java.lang.Integer.min
import kotlin.math.pow

fun computeDefenderStats(maxStats: Int, maxTicks: Int): ShipState {
    var availableStats = maxStats - 2
    val copies = 1

    var fuel = min(maxTicks, availableStats)
    availableStats -= fuel

    val cooling = availableStats / 12
    availableStats -= 12 * cooling

    val attack = availableStats / 4
    availableStats -= 4 * attack

    fuel += availableStats

    return ShipState(fuel, attack, cooling, copies)
}

fun computeAttackerStats(maxStats: Int, maxTicks: Int): ShipState {
//    var availableStats = maxStats - 1
//    val copies = 1
//    availableStats -= 2.0.pow(copies).toInt() - 1
//
//    var fuel = min(maxTicks, availableStats)
//    availableStats -= fuel
//
//    val cooling = min(8, availableStats / 12)
//    availableStats -= 12 * cooling
//
//    val attack = availableStats / 4
//    availableStats -= 4 * attack
//
//    fuel += availableStats
//
//    return ShipState(fuel, attack, cooling, copies)
    return ShipState(27, 96 ,8, 1)
}

class AdaptiveBot(val attackerBot: Bot, val defenderBot: Bot) : Bot {
    lateinit var currentBot: Bot
    override fun initialShipState(mapState: MapState) = when (mapState.role) {
        GameRole.DEFENDER -> {
            currentBot = defenderBot
            computeDefenderStats(mapState.maxStats.toInt(), mapState.tickLimit.toInt())
        }
        GameRole.ATTACKER -> {
            currentBot = attackerBot
            computeAttackerStats(mapState.maxStats.toInt(), mapState.tickLimit.toInt())
        }
    }

    override fun step(gameState: GameState, mapState: MapState) = currentBot.step(gameState, mapState)
}