package ru.spbstu

import ru.spbstu.sim.GSMS
import ru.spbstu.sim.Game
import ru.spbstu.sim.eval
import ru.spbstu.sim.parse
import java.io.File

fun main(args: Array<String>) {
    val aa = parse(File("app/icfpc2020/data/galaxy.txt").readText())

    val (serverUrl, playerKey) = args

    GSMS.serverUrl = serverUrl
    GSMS.playerKey = playerKey

    val game = Game()
    game.loop()
}

