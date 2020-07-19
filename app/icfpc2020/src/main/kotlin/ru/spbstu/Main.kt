package ru.spbstu

import ru.spbstu.sim.GSMS
import ru.spbstu.sim.Game
import ru.spbstu.sim.bot.OrbitBot

fun main(args: Array<String>) {
    val (serverUrl, playerKey) = args
    GSMS.serverUrl = serverUrl
    GSMS.playerKey = playerKey

    val bot = OrbitBot()
    val game = Game(bot)
    game.loop()
}

