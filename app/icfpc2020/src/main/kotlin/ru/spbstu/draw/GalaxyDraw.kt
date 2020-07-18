package ru.spbstu.draw

import ru.spbstu.sim.Picture
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.util.*
import javax.swing.*
import javax.swing.BoxLayout

class StatusBar(val shift: Pair<Int, Int>) : JPanel() {
    val shiftX get() = shift.first
    val shiftY get() = shift.second

    val currentX get() = current.first
    val currentY get() = current.second
    var current: Pair<Int, Int> = 0 to 0
        set(value) {
            field = value
            currentLabel.text = "current = ($currentX, $currentY)"
            realLabel.text = "current + shift = (${currentX + shiftX}, ${currentY + shiftY})"
        }

    private val shiftLabel = JLabel("shift = ($shiftX, $shiftY)")
    private val currentLabel = JLabel("current = ($currentX, $currentY)")
    private val realLabel = JLabel("current + shift = (${currentX + shiftX}, ${currentY + shiftY})")

    init {
        layout = FlowLayout()
        add(shiftLabel)
        add(currentLabel)
        add(realLabel)
    }
}

class GalaxyDraw(
        private val statusBar: StatusBar,
        private val layers: List<Pair<List<Pair<Int, Int>>, Color>>
) : JPanel() {
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        for ((points, color) in layers) {
            g.color = color
            for ((x, y) in points) {
                g.fillRect(x * RECT_WIDTH, y * RECT_WIDTH, RECT_WIDTH, RECT_HEIGHT)
            }
            g.color = Color.BLACK
            g.drawRect(statusBar.currentX * RECT_WIDTH - 1, statusBar.currentY * RECT_WIDTH - 1, RECT_WIDTH + 2, RECT_HEIGHT + 2)
        }
    }

    companion object {
        private const val RECT_WIDTH = 10
        private const val RECT_HEIGHT = RECT_WIDTH

        private fun show(left: Int, top: Int, layers: List<Pair<List<Pair<Int, Int>>, Color>>) {
            SwingUtilities.invokeLater {
                val statusBar = StatusBar(left to top)
                val galaxyDraw = GalaxyDraw(statusBar, layers)
                galaxyDraw.addMouseListener(object : MouseListener {
                    override fun mousePressed(e: MouseEvent?) {}
                    override fun mouseReleased(e: MouseEvent?) {}
                    override fun mouseEntered(e: MouseEvent?) {}
                    override fun mouseExited(e: MouseEvent?) {}
                    override fun mouseClicked(e: MouseEvent?) {
                        val point = e?.point ?: return
                        val x = point.x / RECT_WIDTH
                        val y = point.y / RECT_HEIGHT
                        statusBar.current = x to y
                        galaxyDraw.repaint()
                    }
                })
                val button = Button("send")
                button.addActionListener { println("send") }

                val mainPanel = JPanel()
                mainPanel.layout = BoxLayout(mainPanel, BoxLayout.PAGE_AXIS)
                mainPanel.add(statusBar)
                mainPanel.add(Box.createRigidArea(Dimension(0, 5)))
                mainPanel.add(galaxyDraw)
                mainPanel.add(Box.createRigidArea(Dimension(0, 5)))
                mainPanel.add(button)
                mainPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

                val frame = JFrame("Galaxy")
                frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                frame.contentPane.add(mainPanel)
                frame.pack()
                frame.isLocationByPlatform = true
                frame.isVisible = true
            }
        }

        fun show(vararg pictures: Picture) {
            val random = Random(1)
            val left = pictures.mapNotNull { it.ones.map { it.first }.min() }.min()!!.toInt()
            val top = pictures.mapNotNull { it.ones.map { it.second }.min() }.min()!!.toInt()
            val right = pictures.mapNotNull { it.ones.map { it.first }.max() }.max()!!.toInt()
            val bottom = pictures.mapNotNull { it.ones.map { it.second }.max() }.max()!!.toInt()
            val layers = pictures.map {
                it.ones
                        .map { (x, y) -> x.toInt() to y.toInt() }
                        .map { (x, y) -> x - left to y - top } to random.nextColor()
            }
            show(right - left, bottom - top, layers)
        }

        private fun Random.nextColor(): Color {
            return Color(nextInt(256), nextInt(256), nextInt(256), nextInt(256))
        }
    }
}

fun main() {
    GalaxyDraw.show(Picture(setOf(Pair(3L, 1L), Pair(4L, 2L))))
}
