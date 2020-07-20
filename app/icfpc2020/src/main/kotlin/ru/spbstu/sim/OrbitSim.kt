package ru.spbstu.sim

import ru.spbstu.sim.bot.gravity
import ru.spbstu.wheels.aStarSearch
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.lang.Math.abs
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities.invokeLater

class Planet(val radius: Int)
data class MutShip(
    var coords: Coordinates,
    var v: Coordinates,
    var a: Coordinates = Coordinates(0, 0)
) {

    fun tick() {
        coords += v
        a += gravity(coords)
        v += a
        a = Coordinates(0, 0)
    }

    fun tick(times: Int) {
        repeat(times) { tick() }
    }
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
                { it.coords.manhattanDist(to).toDouble() },
                { it.coords.manhattanDist(to) <= 10.0 && it.v.abs() < 5 },
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
                    neighbours.filter { it.coords.abs() > planetRadius && it.coords.abs() < spaceRadius }
                            .asSequence()
                }
        )

    }
}

class OrbitPanel(val scale: Double = 2.0, val sim: OrbitSim): JPanel() {
    val center get() =
        Point2D.Double(this.width / 2.0, this.height / 2.0)

    val Coordinates.point: Point2D
        get() = Point2D.Double(center.x + x * scale, center.y + y * scale)


    var path: List<MutShip>? = null

    init {
        minimumSize = Dimension(800, 800)
        this.addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                invokeLater {
                    val enemy = sim.ships[1]!!

                    val target = enemy.copy().apply { tick(2) }.coords

                    if(target.abs() > sim.planetRadius) {
                        path = sim.findPath(0, target) ?: System.err.println("NO PATH").let { null }
                    } else System.err.println("Target out of reach")

                    sim.tick()

                    if(path != null) {
                        val rev = path!!.reversed()
                        val accel = rev.getOrNull(1)?.v?.minus(rev[0].v)
                        if(accel != null && !accel.isZero()) {
                            println(ShipCommand.Accelerate(0, accel))
                            sim.applyCommand(ShipCommand.Accelerate(0, accel))
                        }

                    }
                    println(path)

                    this@OrbitPanel.repaint()
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

        for((_, ship) in sim.ships) {
            g.drawPoint(ship.coords)
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
                36, 255,
                listOf()
            ).apply { ships[0] = MutShip(Coordinates(48, 12), Coordinates(0, 0))
                ships[1] = MutShip(Coordinates(-48, -12), Coordinates(0, 0))}
            ))
        }
        .apply { pack(); defaultCloseOperation = JFrame.EXIT_ON_CLOSE }
        .apply {  setVisible(true) }

}


