package ru.spbstu

import ru.spbstu.sim.GSMS
import ru.spbstu.sim.Game
import ru.spbstu.sim.ShipState
import ru.spbstu.sim.bot.*
import kotlin.math.PI

fun main(args: Array<String>) {
    val (serverUrl, playerKey) = args
    GSMS.serverUrl = serverUrl
    GSMS.playerKey = playerKey

    val bot = AdaptiveBot(
        CompositeBot(
            ShipState(256, 0, 0, 1),
            FloatingBot()
        ),
        CompositeBot(
            ShipState(256, 0, 15, 1),
            FloatingBot()
        )
    )
    val game = Game(bot)
    game.loop()
}
