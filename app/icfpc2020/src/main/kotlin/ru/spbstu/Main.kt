package ru.spbstu

import ru.spbstu.sim.*
import ru.spbstu.sim.bot.EmptyBot
import java.io.File

fun main(args: Array<String>) {
    val aa = parse(File("app/icfpc2020/data/galaxy.txt").readText())

    val (serverUrl, playerKey) = args

    GSMS.serverUrl = serverUrl
    GSMS.playerKey = playerKey

    val bot = EmptyBot(ShipState(216, 0, 4, 1 ))
    val game = Game(bot)
    game.loop()
}

