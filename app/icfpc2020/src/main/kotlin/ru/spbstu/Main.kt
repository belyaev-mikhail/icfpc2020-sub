package ru.spbstu

import ru.spbstu.sim.GSMS
import ru.spbstu.sim.Game
import ru.spbstu.sim.ShipState
import ru.spbstu.sim.bot.CompositeBot
import ru.spbstu.sim.bot.OrbitBot
import ru.spbstu.sim.bot.ShootingBot
import ru.spbstu.sim.bot.SplitterBot

fun main(args: Array<String>) {
    val (serverUrl, playerKey) = args
    GSMS.serverUrl = serverUrl
    GSMS.playerKey = playerKey

    val bot = CompositeBot(
        ShipState(200, 20, 10, 3),
        ShootingBot()
    )
    val game = Game(bot)
    game.loop()
}
