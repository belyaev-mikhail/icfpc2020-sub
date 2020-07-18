package ru.spbstu.draw

import ru.spbstu.Symbols
import ru.spbstu.parseMatrix
import ru.spbstu.sim.Picture
import java.awt.*
import java.awt.event.*
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.swing.*
import javax.swing.BoxLayout
import javax.swing.JTextPane


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

    val startX get() = start.first
    val startY get() = start.second
    var start: Pair<Int, Int> = 0 to 0
        set(value) {
            field = value
            startLabel.text = "start = ($startX, $startY)"
        }

    val endX get() = end.first
    val endY get() = end.second
    var end: Pair<Int, Int> = 0 to 0
        set(value) {
            field = value
            endLabel.text = "end = ($endX, $endY)"
        }

    val realX get() = real.first
    val realY get() = real.second
    val real: Pair<Int, Int> get() = currentX + shiftX to currentY + shiftY

    private val shiftLabel = JLabel("shift = ($shiftX, $shiftY)")
    private val sizeLabel = JLabel("size = ($sizeX, $sizeY)")
    private val currentLabel = JLabel("current = ($currentX, $currentY)")
    private val startLabel = JLabel("start = ($startX, $startY)")
    private val endLabel = JLabel("end = ($endX, $endY)")
    private val realLabel = JLabel("current + shift = ($realX, $realY)")

    init {
        layout = FlowLayout()
        add(shiftLabel)
        add(sizeLabel)
        add(currentLabel)
        add(startLabel)
        add(endLabel)
        add(realLabel)
    }
}

private open class MatrixPane(
    val sizeX: Int, val sizeY: Int, val scale: Int,
    val layers: List<Pair<List<Pair<Int, Int>>, Color>>
) : JPanel() {
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.color = Color.BLACK
        g.fillRect(0, 0, sizeX * scale, sizeY * scale)
        for ((points, color) in layers) {
            g.color = color
            for ((x, y) in points) {
                g.fillRect(x * scale, y * scale, scale, scale)
            }
        }
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(sizeX * scale, sizeY * scale)
    }
}

private class GalaxyPane(val statusBar: StatusBar) : JPanel() {
    var layers: List<Pair<List<Pair<Int, Int>>, Color>> = emptyList()

    fun selection(): List<Pair<List<Pair<Int, Int>>, Color>> {
        val rect = selectionRect()
        return layers.map { it.first.filter { (x, y) -> rect.contains(x, y) } to it.second }
    }

    private fun selectionRect(): Rectangle {
        val x = minOf(statusBar.startX, statusBar.endX) - 1
        val y = minOf(statusBar.startY, statusBar.endY) - 1
        val width = maxOf(statusBar.startX, statusBar.endX) - x + 1
        val height = maxOf(statusBar.startY, statusBar.endY) - y + 1
        return Rectangle(x, y, width, height)
    }

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
        g.drawRect(
            statusBar.currentX * statusBar.scale - 1,
            statusBar.currentY * statusBar.scale - 1,
            statusBar.scale + 2,
            statusBar.scale + 2
        )
        g.color = Color.BLUE
        val selection = selectionRect()
        g.drawRect(
            selection.x * statusBar.scale,
            selection.y * statusBar.scale,
            selection.width * statusBar.scale,
            selection.height * statusBar.scale
        )
        g.drawRect(
            selection.x * statusBar.scale - 1,
            selection.y * statusBar.scale - 1,
            selection.width * statusBar.scale + 2,
            selection.height * statusBar.scale + 2
        )
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(statusBar.sizeX * statusBar.scale, statusBar.sizeY * statusBar.scale)
    }
}

private class TranslatorPane(private val statusBar: StatusBar) : JPanel() {
    fun add(part: List<Pair<List<Pair<Int, Int>>, Color>>) {
        val rect = GalaxyDraw.boundingBox(part.flatMap { it.first })
        val layers = part.map { (it, c) -> it.map { (x, y) -> x - rect.x to y - rect.y } to c }
        val points = layers.flatMap { it.first }
        val data = List(rect.height) { y -> List(rect.width) { x -> x to y in points } }
        val symbol = Symbols.get(parseMatrix(data))

        val commandName = JTextPane()
        commandName.text = symbol.command
        commandName.isEditable = false

        val rowPane = JPanel()
        rowPane.layout = BoxLayout(rowPane, BoxLayout.Y_AXIS)
        rowPane.add(MatrixPane(rect.width, rect.height, statusBar.scale, layers))
        rowPane.add(commandName)
        add(rowPane)
    }

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
}

private class GalaxyFrame : JFrame("Galaxy") {
    var promise = CompletableFuture<Pair<Int, Int>?>()

    val statusBar = StatusBar()
    val galaxyPane = GalaxyPane(statusBar)
    val translatorPane = TranslatorPane(statusBar)
    val galaxyScroll = JScrollPane(galaxyPane)
    val translatorScroll = JScrollPane(translatorPane)
    val buttonsPane = JPanel(FlowLayout())

    init {
        SwingUtilities.invokeLater {
            galaxyPane.addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent?) {
                    val point = e?.point ?: return
                    val x = point.x / statusBar.scale
                    val y = point.y / statusBar.scale
                    statusBar.start = x to y
                    statusBar.current = x to y
                    galaxyPane.repaint()
                }

                override fun mouseReleased(e: MouseEvent?) {
                    val point = e?.point ?: return
                    val x = point.x / statusBar.scale
                    val y = point.y / statusBar.scale
                    statusBar.end = x to y
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
            val translateButton = Button("translate")
            translateButton.addActionListener {
                val selection = galaxyPane.selection()
                if (selection.flatMap { it.first }.isNotEmpty()) {
                    translatorPane.add(selection)
                    translatorScroll.repaint()
                    galaxyScroll.repaint()
                }
            }
            val incButton = Button("+")
            incButton.addActionListener {
                if (statusBar.scale++ >= 11) {
                    statusBar.scale = 10
                }
                galaxyPane.repaint()
                translatorScroll.repaint()
            }
            val decButton = Button("-")
            decButton.addActionListener {
                if (statusBar.scale-- <= 0) {
                    statusBar.scale = 1
                }
                galaxyPane.repaint()
                translatorScroll.repaint()
            }
            galaxyPane.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "KEY_P")
            galaxyPane.actionMap.put("KEY_P", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    promise.complete(statusBar.real)
                    promise = CompletableFuture()
                }
            })
            buttonsPane.add(backButton)
            buttonsPane.add(sendButton)
            buttonsPane.add(translateButton)
            buttonsPane.add(incButton)
            buttonsPane.add(decButton)

            val mainPanel = JPanel()
            mainPanel.layout = BorderLayout()
            mainPanel.add(statusBar, BorderLayout.NORTH)
            mainPanel.add(galaxyScroll, BorderLayout.CENTER)
            mainPanel.add(translatorScroll, BorderLayout.EAST)
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
        val box = boundingBox(pictures.flatMap { it.ones }
            .map { (x, y) -> x.toInt() to y.toInt() })
        val layers = pictures.zip(colors.take(pictures.size).toList()).reversed()
            .map { (pic, color) ->
                pic.ones
                    .map { (x, y) -> x.toInt() to y.toInt() }
                    .map { (x, y) -> x - box.x to y - box.y } to color
            }
        return interact(box, layers)
    }

    fun boundingBox(points: List<Pair<Int, Int>>): Rectangle {
        val left = points.map { it.first }.min()!!
        val top = points.map { it.second }.min()!!
        val right = points.map { it.first }.max()!! + 1
        val bottom = points.map { it.second }.max()!! + 1
        return Rectangle(left, top, right - left, bottom - top)
    }

    private fun interact(rect: Rectangle, layers: List<Pair<List<Pair<Int, Int>>, Color>>): Pair<Int, Int>? {
        SwingUtilities.invokeLater {
            galaxyFrame.statusBar.shift = rect.x to rect.y
            galaxyFrame.statusBar.size = rect.width to rect.height
            galaxyFrame.galaxyPane.layers = layers
            galaxyFrame.galaxyScroll.invalidate()
            galaxyFrame.galaxyScroll.repaint()
        }
        return galaxyFrame.promise.get()
    }
}
