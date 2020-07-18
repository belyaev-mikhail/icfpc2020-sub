package ru.spbstu.draw

import ru.spbstu.sim.Picture
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.swing.*


private class StatusBar : JPanel() {
    var scale = 7

    val shiftX get() = shift.first
    val shiftY get() = shift.second
    var shift: Pair<Int, Int> = 0 to 0
        set(value) {
            field = value
            shiftLabel.text = "current = ($shiftX, $shiftY)"
        }

    val sizeX get() = size.first
    val sizeY get() = size.second
    var size: Pair<Int, Int> = 0 to 0
        set(value) {
            field = value
            shiftLabel.text = "size = ($sizeX, $sizeY)"
        }

    val currentX get() = current.first
    val currentY get() = current.second
    var current: Pair<Int, Int> = 0 to 0
        set(value) {
            field = value
            currentLabel.text = "current = ($currentX, $currentY)"
            realLabel.text = "current + shift = ($realX, $realY)"
        }

    val realX get() = real.first
    val realY get() = real.second
    val real: Pair<Int, Int> get() = currentX + shiftX to currentY + shiftY

    private val shiftLabel = JLabel("shift = ($shiftX, $shiftY)")
    private val sizeLabel = JLabel("size = ($sizeX, $sizeY)")
    private val currentLabel = JLabel("current = ($currentX, $currentY)")
    private val realLabel = JLabel("current + shift = ($realX, $realY)")

    init {
        layout = FlowLayout()
        add(shiftLabel)
        add(sizeLabel)
        add(currentLabel)
        add(realLabel)
    }
}

private class GalaxyPane(private val statusBar: StatusBar) : JPanel() {
    var layers: List<Pair<List<Pair<Int, Int>>, Color>> = emptyList()

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.color = Color.BLACK
        g.fillRect(0, 0, statusBar.sizeX * statusBar.scale, statusBar.sizeY * statusBar.scale)
        for ((points, color) in layers) {
            g.color = color
            for ((x, y) in points) {
                g.fillRect(x * statusBar.scale, y * statusBar.scale, statusBar.scale, statusBar.scale)
            }
        }
        g.color = Color.RED
        g.drawRect(
            statusBar.currentX * statusBar.scale,
            statusBar.currentY * statusBar.scale,
            statusBar.scale,
            statusBar.scale
        )
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(statusBar.sizeX * statusBar.scale, statusBar.sizeY * statusBar.scale)
    }
}

private class GalaxyFrame : JFrame("Galaxy") {
    var promise = CompletableFuture<Pair<Int, Int>?>()

    val statusBar = StatusBar()
    val galaxyPane = GalaxyPane(statusBar)
    val galaxyScroll = JScrollPane(galaxyPane)
    val buttonsPane = JPanel(FlowLayout())

    init {
        SwingUtilities.invokeLater {
            galaxyPane.addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent?) {
                    val point = e?.point ?: return
                    val x = point.x / statusBar.scale
                    val y = point.y / statusBar.scale
                    statusBar.current = x to y
                    galaxyPane.repaint()
                }
            })
            val sendButton = Button("send")
            sendButton.addActionListener {
                promise.complete(statusBar.real)
                promise = CompletableFuture()
            }
            val backButton = Button("back")
            backButton.addActionListener {
                promise.complete(null)
                promise = CompletableFuture()
            }
            val incButton = Button("+")
            incButton.addActionListener {
                if (statusBar.scale++ >= 11) {
                    statusBar.scale = 10
                }
                galaxyPane.repaint()
            }
            val decButton = Button("-")
            decButton.addActionListener {
                if (statusBar.scale-- <= 0) {
                    statusBar.scale = 1
                }
                galaxyPane.repaint()
            }
            buttonsPane.add(backButton)
            buttonsPane.add(sendButton)
            buttonsPane.add(incButton)
            buttonsPane.add(decButton)

            val mainPanel = JPanel()
            mainPanel.layout = BorderLayout()
            mainPanel.add(statusBar, BorderLayout.NORTH)
            mainPanel.add(galaxyScroll, BorderLayout.CENTER)
            mainPanel.add(buttonsPane, BorderLayout.SOUTH)
            mainPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

            defaultCloseOperation = EXIT_ON_CLOSE
            contentPane.add(mainPanel)
            pack()
            isLocationByPlatform = true
            isVisible = true
            size = Dimension(1280, 1024)
        }
    }
}

object GalaxyDraw {
    private val galaxyFrame = GalaxyFrame()

    private val colors: Sequence<Color>
        get() = sequence {
            val random = Random(1)
            yield(Color.WHITE)
            yield(Color.LIGHT_GRAY)
            yield(Color.GRAY)
            yield(Color.DARK_GRAY)
            while (true) {
                with(random) {
                    yield(Color(nextInt(256), nextInt(256), nextInt(256), nextInt(256)))
                }
            }
        }

    fun interact(pictures: List<Picture>): Pair<Int, Int>? {
        val left = pictures.mapNotNull { it.ones.map { it.first }.min() }.min()!!.toInt()
        val top = pictures.mapNotNull { it.ones.map { it.second }.min() }.min()!!.toInt()
        val right = pictures.mapNotNull { it.ones.map { it.first }.max() }.max()!!.toInt() + 1
        val bottom = pictures.mapNotNull { it.ones.map { it.second }.max() }.max()!!.toInt() + 1
        val layers = pictures.zip(colors.take(pictures.size).toList()).reversed()
                .map { (pic, color) ->
                    pic.ones
                            .map { (x, y) -> x.toInt() to y.toInt() }
                            .map { (x, y) -> x - left to y - top } to color
                }
        return interact(Rectangle(left, top, right - left, bottom - top), layers)
    }

    private fun interact(rect: Rectangle, layers: List<Pair<List<Pair<Int, Int>>, Color>>): Pair<Int, Int>? {
        SwingUtilities.invokeLater {
            galaxyFrame.statusBar.shift = rect.x to rect.y
            galaxyFrame.statusBar.size = rect.width to rect.height
            galaxyFrame.galaxyPane.layers = layers
            galaxyFrame.galaxyScroll.invalidate()
        }
        return galaxyFrame.promise.get()
    }
}
