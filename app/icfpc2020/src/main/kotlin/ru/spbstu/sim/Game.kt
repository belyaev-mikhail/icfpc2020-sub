package ru.spbstu.sim

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.spbstu.protocol.Protocol

interface GameRequest {
    fun symbol(): Symbol
    fun modulate() = Protocol().encode(symbol())
}

data class Join(val arguments: List<Symbol>) : GameRequest {
    override fun symbol() = listOf(
        Num(2),
        Num(GSMS.playerKey.toLong()),
        arguments.asListSymbol()
    ).asListSymbol()
}

data class Start(val number1: Long, val number2: Long, val number3: Long, val number4: Long) : GameRequest {
    override fun symbol() = listOf(
        Num(3),
        Num(GSMS.playerKey.toLong()),
        listOf(number1, number2, number3, number4).map { Num(it) }.asListSymbol()
    ).asListSymbol()
}

data class ShipCommand(val commands: List<Symbol>) : GameRequest {
    override fun symbol() = listOf(
        Num(4),
        Num(GSMS.playerKey.toLong()),
        commands.asListSymbol()
    ).asListSymbol()
}

enum class GameStage {
    NOT_STARTED, STARTED, FINISHED;
}

data class GameResponse(
    val status: Long,
    val stage: GameStage,
    val constantUnknownList: Symbol,
    val maybeStateOfGame: Symbol
) {
    companion object {
        fun valueOf(symbol: Symbol): GameResponse {
            var cons = symbol as Cons
            val status = (cons.car as Num).number
            if (status != 1L) TODO("Incorrect game response")
            cons = cons.cdr as Cons
            val stageIndex = (cons.car as Num).number.toInt()
            val stage = GameStage.values()[stageIndex]
            cons = cons.cdr as Cons
            val unknownList1 = cons.car
            cons = cons.cdr as Cons
            val unknownList2 = cons.car
            return GameResponse(status, stage, unknownList1, unknownList2)
        }
    }
}

private fun List<Symbol>.asListSymbol(): Symbol = fold(nil as Symbol) { acc, symbol -> Cons(symbol, acc) }

class Game {
    private val client = OkHttpClient()
    private fun send(request: GameRequest): GameResponse {
        val requestData = request.modulate()
        val httpRequest = Request.Builder().url(GSMS.serverUrl).post(requestData.toRequestBody()).build()
        val response = client.newCall(httpRequest).execute()
        val status = response.code
        check(status == 200)
        val body = response.body ?: TODO("FUCK")
        val res = body.string()
        val parsed = Protocol().decode(res)
        return GameResponse.valueOf(parsed)
    }

    open fun join() = Join(emptyList())
    open fun start(state: GameResponse) = Start(1, 1, 1, 1)
    open fun command(state: GameResponse) = ShipCommand(emptyList())

    fun loop() {
        val join = join()
        var state = send(join)
        System.err.println("Join")
        System.err.println("$state")
        val start = start(state)
        state = send(start)
        System.err.println("Start")
        System.err.println("$state")
        while (state.stage != GameStage.FINISHED) {
            val cmd = command(state)
            state = send(cmd)
            System.err.println("Command")
            System.err.println("$state")
        }
    }

}
