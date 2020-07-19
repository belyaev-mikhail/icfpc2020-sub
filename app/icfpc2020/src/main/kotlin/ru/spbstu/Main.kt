package ru.spbstu

import ru.spbstu.sim.GSMS
import ru.spbstu.sim.Game
import ru.spbstu.sim.ShipState
import ru.spbstu.sim.bot.CompositeBot
import ru.spbstu.sim.bot.OrbitBot

fun main(args: Array<String>) {
    val (serverUrl, playerKey) = args
    GSMS.serverUrl = serverUrl
    GSMS.playerKey = playerKey

    val bot = CompositeBot(ShipState(256, 0, 15, 1), OrbitBot())
    val game = Game(bot)
    game.loop()
}
