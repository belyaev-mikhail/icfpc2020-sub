package ru.spbstu.sim.bot

import ru.spbstu.sim.*
import java.lang.Integer.min
import kotlin.math.pow

fun computeDefenderStats(maxStats: Int): ShipState {
    var availableStats = maxStats - 1
    val copies = 5
    availableStats -= 2.0.pow(copies).toInt() - 1

    var fuel = min(256, availableStats)
    availableStats -= fuel

    val cooling = availableStats / 12
    availableStats -= 12 * cooling

//    val attack = availableStats / 4
//    availableStats -= 4 * attack

    fuel += availableStats

    return ShipState(fuel, 0, cooling, copies)
}

fun computeAttackerStats(maxStats: Int): ShipState {
    var availableStats = maxStats - 1
    val copies = 1
    availableStats -= 2.0.pow(copies).toInt() - 1

    var fuel = min(256, availableStats)
    availableStats -= fuel

    val cooling = min(8, availableStats / 12)
    availableStats -= 12 * cooling

    val attack = availableStats / 4
    availableStats -= 4 * attack

    fuel += availableStats

    return ShipState(fuel, attack, cooling, copies)
}

class AdaptiveBot(val attackerBot: Bot, val defenderBot: Bot) : Bot {
    lateinit var currentBot: Bot

    override fun prepare(mapState: MapState) {
        when (mapState.role) {
            GameRole.DEFENDER -> {
                defenderBot.prepare(mapState)
            }
            GameRole.ATTACKER -> {
                attackerBot.prepare(mapState)
            }
        }
    }

    override fun initialShipState(mapState: MapState) = when (mapState.role) {
        GameRole.DEFENDER -> {
            currentBot = defenderBot
            computeDefenderStats(mapState.maxStats.toInt())
        }
        GameRole.ATTACKER -> {
            currentBot = attackerBot
            computeAttackerStats(mapState.maxStats.toInt())
        }
    }

    override fun step(gameState: GameState, mapState: MapState) = currentBot.step(gameState, mapState)
}