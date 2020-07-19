package ru.spbstu

import ru.spbstu.sim.*
import ru.spbstu.sim.bot.DummyBot
import ru.spbstu.sim.bot.EmptyBot
import java.io.File

fun main(args: Array<String>) {
    val (serverUrl, playerKey) = args
    GSMS.serverUrl = serverUrl
    GSMS.playerKey = playerKey

    val bot = DummyBot()
    val game = Game(bot)
    game.loop()
}

