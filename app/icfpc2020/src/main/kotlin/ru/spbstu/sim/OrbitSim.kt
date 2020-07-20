package ru.spbstu.sim

import ru.spbstu.sim.bot.gravity
import ru.spbstu.wheels.aStarSearch
import java.awt.*
import java.awt.event.*
import java.awt.geom.Ellipse2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.lang.Math.abs
import javax.swing.AbstractAction
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.SwingUtilities.invokeLater
import javax.swing.text.Keymap

class Planet(val radius: Int)
data class MutShip(
    var coords: Coordinates,
    var v: Coordinates,
    var a: Coordinates = Coordinates(0, 0)
) {

    fun tick() {
        a += gravity(coords)
        v += a
        coords += v
        a = Coordinates(0, 0)
    }

    fun tick(times: Int) {
        repeat(times) { tick() }
    }

    override fun equals(other: Any?): Boolean = other is MutShip && other.coords == coords
    override fun hashCode(): Int = coords.hashCode()
}
class OrbitSim(val planetRadius: Long, val spaceRadius: Long, ships: List<GameShip>) {
    val ships: MutableMap<Long, MutShip> = mutableMapOf()
    init {
        for(ship in ships) {
            this.ships[ship.id] = MutShip(
                ship.position,
                ship.velocity
            )
        }
    }

    fun gravity(relTo: Coordinates): Coordinates {
        val (x, y) = relTo
        var res = Coordinates(0, 0)
        if (x > 0 && abs(x) >= abs(y)) res += Coordinates(-1, 0)
        if (x < 0 && abs(x) >= abs(y)) res += Coordinates(1, 0)
        if (y > 0 && abs(x) <= abs(y)) res += Coordinates(0, -1)
        if (y < 0 && abs(x) <= abs(y)) res += Coordinates(0, 1)
        return res
    }

    fun tick() {
        for((_, ship) in ships) {
            ship.tick()
        }
    }

    fun applyCommand(sc: ShipCommand) {
        when(sc) {
            is ShipCommand.Accelerate -> {
                ships.getValue(sc.shipId).a += sc.velocity
            }
            else -> {}/**/
        }
    }

    fun findPath(me: Long, to: Coordinates): List<MutShip>? {
        val start = ships[me]!!

        return aStarSearch(start,
                { it.coords.manhattanDist(to).toDouble() / 5.0 },
                { it.coords.manhattanDist(to) <= 32.0 && it.v.abs() < 10 &&
                        (it.coords.x == to.x || it.coords.y == to.y) },
                {
                    val simple = it.copy().apply { tick() }
                    val neighbours = mutableListOf(simple)
                    for(i in listOf(-1L, 0L, 1L)) {
                        for (j in listOf(-1L, 0L, 1L)) {
                            if(i == j && i == 0L) continue

                            val accel = Coordinates(i, j)
                            val me = it.copy()
                            me.a += accel
                            neighbours += me.apply { tick() }
                        }
                    }
                    neighbours.filter {
                        it.v.abs() < 10 && it.coords.abs() > planetRadius && it.coords.abs() < spaceRadius }
                            .asSequence()
                }
        )

    }

    fun findStableOrbit(me: Long): List<MutShip>? {
        val start = ships[me]!!
        println("start = $start")

        return aStarSearch(start,
                { it.coords.manhattanDist(it.coords.swap()).toDouble() },
                goal@{ship ->
                    //MutShip(coords=Coordinates(x=47, y=12), v=Coordinates(x=-1, y=0), a=Coordinates(x=1, y=1))
                    val mt = ship.copy()
                    (0..(planetRadius.toInt() * 2000)).any {
                        mt.tick()
                        if(mt.coords.abs() < planetRadius || mt.coords.abs() > spaceRadius) return@goal false
                        (mt == ship && mt.v == ship.v && !mt.v.isZero())
                    }
//                    repeat(planetRadius.toInt() * 6000) {
//                        mt.tick()
//                        res = res and (mt.coords.abs() > planetRadius && mt.coords.abs() < spaceRadius)
//                        if(!res) return@goal false
//                        if(ship == mt && ship.v == mt.v) return@goal true
//                    }
//                    res
                },
                {
                    val simple = it.copy().apply { tick() }
                    val neighbours = mutableListOf(simple)
                    for(i in listOf(-1L, 0L, 1L)) {
                        for (j in listOf(-1L, 0L, 1L)) {
                            if(i == j && i == 0L) continue

                            val accel = Coordinates(i, j)
                            val me = it.copy()
                            me.a += accel
                            neighbours += me.apply { tick() }
                        }
                    }
                    neighbours.filter {
                        it.v.abs() < 10 && it.coords.abs() > planetRadius && it.coords.abs() < spaceRadius }
                            .asSequence()
                }
        )

    }
}

fun Action(body: (ActionEvent) -> Unit) = object: AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
        return body(e)
    }
}

class OrbitPanel(val scale: Double = 2.0, val sim: OrbitSim): JPanel() {
    val center get() =
        Point2D.Double(this.width / 2.0, this.height / 2.0)

    val Coordinates.point: Point2D
        get() = Point2D.Double(center.x + x * scale, center.y + y * scale)


    var path: MutableList<MutShip> = mutableListOf()
    var built = false
    fun tick() {
        val target = sim.ships[1]!!

        fun isValid(coords: Coordinates) = coords.abs() > sim.planetRadius && coords.abs() < sim.spaceRadius

        if(path.isEmpty() && !built) {
            built = true
            path = sim.findStableOrbit(0)?.toMutableList() ?: mutableListOf()
        }
        println("Path: $path")

        sim.tick()

        if(path.isNotEmpty()) {
            val rev = path.reversed()
            val from = rev[0]
            val to = rev.getOrNull(1) ?: sim.ships[0]!!
            to.v = to.coords - from.coords
            val accel = to.v - from.v - gravity(from.coords)
            if(accel != null && !accel.isZero()) {
                println(ShipCommand.Accelerate(0, accel))
                sim.applyCommand(ShipCommand.Accelerate(0, accel))
            }
            println("Expected: ${path.last()}")
            println("Actual: ${sim.ships[0]}")
            println("Target: $to")
            println("Actual next: ${sim.ships[0]!!.copy().apply { tick() } }")
            path.removeAt(path.lastIndex)
        } else {
            println("out of moves")
        }

        this@OrbitPanel.repaint()
    }

    fun onKey(key: Int, action: () -> Unit) {
        val skey = "VK_$key"
        this.inputMap.put(KeyStroke.getKeyStroke(key, 0), skey)
        this.actionMap.put(skey, Action { action() })
    }

    init {
        minimumSize = Dimension(800, 800)
        val keyMapping = mapOf(
                KeyEvent.VK_NUMPAD3 to Coordinates(1, 1),
                KeyEvent.VK_NUMPAD2 to Coordinates(0, 1),
                KeyEvent.VK_NUMPAD1 to Coordinates(-1, 1),
                KeyEvent.VK_NUMPAD4 to Coordinates(-1, 0),
                KeyEvent.VK_NUMPAD6 to Coordinates(1, 0),
                KeyEvent.VK_NUMPAD7 to Coordinates(-1, -1),
                KeyEvent.VK_NUMPAD8 to Coordinates(0, -1),
                KeyEvent.VK_NUMPAD9 to Coordinates(1, -1)
        )
        onKey(KeyEvent.VK_SPACE) {
            tick()
        }
        for((k, v) in keyMapping) {
            onKey(k) {
                println("Pressed ${KeyStroke.getKeyStroke(k, 0).keyChar}")
                invokeLater {
                    val me = sim.ships[0]!!.copy()
                    me.a += v
                    me.tick()
                    path.add(0, me)
                    tick()
                }
            }
        }
        this.addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                invokeLater {
                    tick()
                }
            }
        })
    }
    override fun setMinimumSize(minimumSize: Dimension?) {
        super.setMinimumSize(minimumSize)
    }

    fun Graphics2D.drawPoint(c: Coordinates) {
        val c = c.point
        fill(Ellipse2D.Double(c.x - scale * 2, c.y - scale * 2, scale * 4, scale * 4))
    }

    override fun getPreferredSize(): Dimension = Dimension(800, 800)

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        check(g is Graphics2D)

        g.draw(Rectangle2D.Double(center.x, center.y, 0.0, 0.0).apply {
            add(Coordinates(0, sim.planetRadius.toLong()).point)
            add(Coordinates(0, -sim.planetRadius.toLong()).point)
            add(Coordinates(sim.planetRadius.toLong(), 0).point)
            add(Coordinates(-sim.planetRadius.toLong(), 0).point)
        })

        g.paint = Color.RED

        for((id, ship) in sim.ships) {
            g.paint = Color.RED
            g.drawPoint(ship.coords)
            g.paint = Color(255, 0, 0, 30)
            if(id != 0L) continue
            val mt = ship.copy()
            (0..(sim.planetRadius.toInt() * 30)).forEach {
                mt.tick()
                g.drawPoint(mt.coords)
            }
        }

        g.paint = Color.BLUE.brighter()

        for((_, ship) in sim.ships) {
            g.drawPoint(ship.coords + sim.gravity(ship.coords))
        }

        g.paint = Color.GREEN.brighter()

        for((_, ship) in sim.ships) {
            g.drawPoint(ship.coords + ship.v)
        }

        g.paint = Color.ORANGE
        for(p in path.orEmpty()) {
            g.drawPoint(p.coords)
        }

    }
}

fun main() {
    JFrame("Hello")
        .apply {
            add(OrbitPanel(sim = OrbitSim(
                72, 255,
                listOf()
            ).apply { ships[0] = MutShip(Coordinates(112, 112), Coordinates(0, 0))
                ships[1] = MutShip(Coordinates(-112, -112), Coordinates(0, 0))}
            ))
        }
        .apply { pack(); defaultCloseOperation = JFrame.EXIT_ON_CLOSE }
        .apply {  setVisible(true) }

}


