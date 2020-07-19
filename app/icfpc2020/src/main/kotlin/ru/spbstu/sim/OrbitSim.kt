package ru.spbstu.sim

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
class MutShip(
    var coords: Coordinates,
    var v: Coordinates,
    var a: Coordinates = Coordinates(0, 0)
) {

    fun tick() {
        coords += v
        v += a
        a = Coordinates(0, 0)
    }
}
class OrbitSim(val planet: Planet, ships: List<GameShip>) {
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
            ship.a += gravity(ship.coords)
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
}

class OrbitPanel(val scale: Double = 7.0, val sim: OrbitSim): JPanel() {
    val center get() =
        Point2D.Double(this.width / 2.0, this.height / 2.0)

    val Coordinates.point: Point2D
        get() = Point2D.Double(center.x + x * scale, center.y + y * scale)

    init {
        minimumSize = Dimension(800, 800)
        this.addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                invokeLater {
                    sim.tick()
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
        fill(Ellipse2D.Double(c.x - scale * 0.5, c.y - scale * 0.5, scale, scale))
    }

    override fun getPreferredSize(): Dimension = Dimension(800, 800)

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        check(g is Graphics2D)

        g.draw(Rectangle2D.Double(center.x, center.y, 0.0, 0.0).apply {
            add(Coordinates(0, sim.planet.radius.toLong()).point)
            add(Coordinates(0, -sim.planet.radius.toLong()).point)
            add(Coordinates(sim.planet.radius.toLong(), 0).point)
            add(Coordinates(-sim.planet.radius.toLong(), 0).point)
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

    }
}

fun main() {
    JFrame("Hello")
        .apply {
            add(OrbitPanel(sim = OrbitSim(
                Planet(16),
                listOf()
            ).apply { ships[1] = MutShip(Coordinates(148, 168), Coordinates(0, 0)) }
            ))
        }
        .apply { pack(); defaultCloseOperation = JFrame.EXIT_ON_CLOSE }
        .apply {  setVisible(true) }

}


