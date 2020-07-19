package ru.spbstu.sim

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.spbstu.pow
import ru.spbstu.protocol.Protocol
import ru.spbstu.sim.bot.Bot
import kotlin.math.abs
import kotlin.math.sqrt

interface GameRequest {
    fun symbol(): Symbol
    fun modulate() = Protocol().encode(symbol())
}

data class ShipState(val fuel: Int, val power: Int, val coolPerTick: Int, val numberOfCopies: Int) {
    val isDead: Boolean
        get() = listOf(fuel, power, coolPerTick, numberOfCopies).all { it == 0 }

    fun asSymbol(): Symbol = consListOf(listOf(fuel, power, coolPerTick, numberOfCopies).map { Num(it.toLong()) })

    companion object {
        fun fromSymbol(symbol: Symbol): ShipState {
            val (a, b, c, d) = symbol.asLongList().map { it.toInt() }
            return ShipState(a, b, c, d)
        }
    }
}

data class JoinRequest(val arguments: List<Symbol>) : GameRequest {
    override fun symbol() = consListOf(
        Num(2),
        Num(GSMS.playerKey.toLong()),
        consListOf(arguments)
    )
}

data class StartRequest(val shipState: ShipState) : GameRequest {
    override fun symbol() = consListOf(
        Num(3),
        Num(GSMS.playerKey.toLong()),
        shipState.asSymbol()
    )
}

data class ShipCommandRequest(val commands: List<ShipCommand>) : GameRequest {
    override fun symbol() = consListOf(
        Num(4),
        Num(GSMS.playerKey.toLong()),
        consListOf(commands.map { it.asSymbol() })
    )
}

enum class GameStage {
    NOT_STARTED, STARTED, FINISHED;
}

enum class GameRole {
    ATTACKER, DEFENDER;
}

data class MapState(
    val tickLimit: Long,
    val role: GameRole,
    val mapParams1: List<Long>,
    val planeRadius: Long,
    val spaceRadius: Long,
    val attackerStats: ShipState?
) {
    val maxStats get() = mapParams1.first()
    val maxShootingDistance get() = mapParams1[2]
}

data class GameState(
    val tick: Long,
    val unknownParam: List<Long>,
    val ships: List<GameShip>
)

data class GameResponse(
    val status: Long,
    val stage: GameStage,
    val mapState: MapState,
    val gameStateUnsafe: GameState?
) {
    val gameState: GameState
        get() = when (stage) {
            GameStage.STARTED -> gameStateUnsafe!!
            else -> throw IllegalStateException("game is not started")
        }

    companion object {
        fun valueOf(symbol: Symbol): GameResponse? {
            val gameResponse = symbol.asList()
            val status = gameResponse.first().asLong()
            if (status != 1L) {
                System.err.println("Incorrect response: $symbol")
                return null
            }
            val (_, stageIndexSym, mapStateSym, gameStateSym) = gameResponse
            val stage = GameStage.values()[stageIndexSym.asLong().toInt()]
            if (stage == GameStage.FINISHED) {
                System.err.println("Game finished")
                return null
            }
            val mapState = parseMapState(mapStateSym)
            val gameState = parseGameState(gameStateSym)
            return GameResponse(status, stage, mapState, gameState)
        }

        private fun parseMapState(symbol: Symbol): MapState {
            val mapState = symbol.asList()
            val (tickLimitSym, roleSym, mapParams1Sym, mapParams2Sym, mapParams3Sym) = mapState
            val mapParams1 = mapParams1Sym.asLongList()
            val mapParams2 = mapParams2Sym.asLongList()
            val mapParams3 = mapParams3Sym.asLongList()
            val role = GameRole.values()[roleSym.asLong().toInt()]
            val attackerStats = when {
                mapParams3.isEmpty() -> null
                else -> ShipState.fromSymbol(mapParams3Sym)
            }
            return MapState(tickLimitSym.asLong(), role, mapParams1, mapParams2[0], mapParams2[1], attackerStats)
        }

        private fun parseGameState(symbol: Symbol): GameState? {
            val gameState = symbol.asList()
            if (gameState.isEmpty()) return null
            val (tickSym, unknownParamSym, shipsSym) = gameState
            val unknownParam = unknownParamSym.asLongList()
            val ships = shipsSym.asList().mapNotNull { parseShipData(it) }
            return GameState(tickSym.asLong(), unknownParam, ships)
        }

        private fun parseShipData(shipData: Symbol): GameShip? {
            if (shipData.asList().isEmpty()) return null
            val (shipStateSym, shipCommandsSym) = shipData.asList()
            val (roleSym, shipIdSym, positionSym, velocitySym, stateSym) = shipStateSym.asList()
            val (unknown1, unknown2, unknown3) = shipStateSym.asList().drop(5)
            val shipId = shipIdSym.asLong()
            val commands = shipCommandsSym.asList().mapNotNull { parseShipCommand(shipId, it) }
            val role = GameRole.values()[roleSym.asLong().toInt()]
            val state = ShipState.fromSymbol(stateSym)
            return GameShip(
                shipId, role, positionSym.coords(), velocitySym.coords(),
                state, unknown1, unknown2, unknown3, commands
            )
        }

        private fun parseShipCommand(shipId: Long, shipCommandData: Symbol): ShipCommand? {
            val commandParams = shipCommandData.asList()
            if (commandParams.isEmpty()) return null
            val type = CommandType.values()[commandParams.first().asLong().toInt()]
            return when (type) {
                CommandType.ACCELERATE -> ShipCommand.Accelerate(shipId, commandParams[1].coords())
                CommandType.DETONATE -> ShipCommand.Detonate(shipId)
                CommandType.SHOOT -> ShipCommand.Shoot(
                    shipId,
                    commandParams[1].coords(),
                    commandParams[2].asLong()
                )
                CommandType.SPLIT -> ShipCommand.Split(
                    shipId, ShipState.fromSymbol(commandParams[1])
                )
            }
        }
    }
}

data class Coordinates(val x: Long, val y: Long) {
    operator fun plus(other: Coordinates) = Coordinates(x + other.x, y + other.y)
    operator fun minus(other: Coordinates) = Coordinates(x - other.x, y - other.y)
    operator fun unaryMinus() = Coordinates(-x, -y)
    operator fun times(other: Coordinates) = Coordinates(x * other.x, y * other.y)
    fun asSymbol(): Symbol = Cons(Num(x), Num(y))
    fun isZero() = x == 0L && y == 0L
    fun swap(): Coordinates = Coordinates(y, x)

    fun dist(other: Coordinates) = sqrt(((this.x - other.x).pow(2) + (this.y - other.y).pow(2)).toDouble())
    fun manhattanDist(other: Coordinates) = maxOf(abs(this.x - other.x), abs(this.y - other.y))
}

data class GameShip(
    val id: Long,
    val role: GameRole,
    val position: Coordinates,
    val velocity: Coordinates,
    val state: ShipState,
    val heatLevel: Symbol,
    val maxHeatingLevel: Symbol,
    val accelerationBonus: Symbol,
    val commands: List<ShipCommand>
) {
    val isDead: Boolean
        get() = state.isDead
}

enum class CommandType {
    ACCELERATE, DETONATE, SHOOT, SPLIT
}

sealed class ShipCommand(val type: CommandType) {
    abstract fun asSymbol(): Symbol

    data class Accelerate(val shipId: Long, val velocity: Coordinates) : ShipCommand(CommandType.ACCELERATE) {
        override fun asSymbol(): Symbol = consListOf(Num(0), Num(shipId), velocity.asSymbol())
    }

    data class Detonate(val shipId: Long) : ShipCommand(CommandType.DETONATE) {
        override fun asSymbol(): Symbol = consListOf(Num(1), Num(shipId))
    }

    data class Shoot(val shipId: Long, val coordinates: Coordinates, val power: Long) : ShipCommand(CommandType.SHOOT) {
        override fun asSymbol(): Symbol =
            consListOf(Num(2), Num(shipId), coordinates.asSymbol(), Num(power))
    }

    data class Split(val shipId: Long, val stats: ShipState) : ShipCommand(CommandType.SPLIT) {
        override fun asSymbol(): Symbol = consListOf(Num(3), Num(shipId), stats.asSymbol())
    }
}

private fun Symbol.coords() = asLongList().let {
    val (x, y) = it
    Coordinates(x, y)
}

private fun Symbol.asList(): List<Symbol> = when (this) {
    is Nil -> emptyList()
    is Cons -> this.iterator().asSequence().toList()
    else -> TODO()
}

private fun Symbol.asLongList() = asList().filterIsInstance<Num>().map { it.asLong() }

private fun Symbol.asLong() = (this as Num).number


class Game(val bot: Bot) {
    private val client = OkHttpClient()
    private fun send(request: GameRequest): GameResponse? {
        System.err.println("$request")
        System.err.println("${request.symbol()}")
        val requestData = request.modulate()
        val httpRequest =
            Request.Builder().url("${GSMS.serverUrl}/aliens/send").post(requestData.toRequestBody()).build()
        val response = client.newCall(httpRequest).execute()
        val status = response.code
        check(status == 200)
        val body = response.body ?: TODO("FUCK")
        val res = body.string()
        val parsed = Protocol().decode(res)
        System.err.println("Response")
        System.err.println(parsed)
        return GameResponse.valueOf(parsed)
    }

    private fun join() = JoinRequest(listOf(Num(103652820), Num(192496425430)))

    private fun start(state: GameResponse): StartRequest {
        bot.prepare(state.mapState)
        return StartRequest(bot.initialShipState(state.mapState))
    }

    private fun command(state: GameResponse): ShipCommandRequest {
        return ShipCommandRequest(bot.step(state.gameState, state.mapState))
    }

    fun loop() {
        val join = join()
        var state = send(join) ?: return
        System.err.println("Join")
        System.err.println("$state")
        val start = start(state)
        state = send(start) ?: return
        System.err.println("Start")
        System.err.println("$state")
        while (state.stage != GameStage.FINISHED) {
            val cmd = command(state)
            state = send(cmd) ?: return
            System.err.println("Command")
            System.err.println("$state")
        }
    }

}
