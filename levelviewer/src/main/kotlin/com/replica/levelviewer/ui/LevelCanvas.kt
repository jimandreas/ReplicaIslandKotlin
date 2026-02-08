package com.replica.levelviewer.ui

import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import javax.swing.JPanel
import javax.swing.SwingUtilities

class LevelCanvas : JPanel() {
    var levelImage: BufferedImage? = null
        set(value) {
            field = value
            updatePreferredSize()
            revalidate()
            repaint()
        }

    var zoomLevel: Double = 1.0
        set(value) {
            field = value.coerceIn(0.1, 4.0)
            updatePreferredSize()
            revalidate()
            repaint()
        }

    private var dragStartPoint: Point? = null
    private var dragStartScroll: Point? = null

    init {
        background = Color(32, 32, 48)

        addMouseWheelListener { e ->
            val oldZoom = zoomLevel
            zoomLevel = if (e.wheelRotation < 0) {
                (zoomLevel * 1.15).coerceAtMost(4.0)
            } else {
                (zoomLevel / 1.15).coerceAtLeast(0.1)
            }
            if (oldZoom != zoomLevel) {
                firePropertyChange("zoomLevel", oldZoom, zoomLevel)
            }
        }

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    dragStartPoint = e.point
                    val scrollPane = parent?.parent
                    if (scrollPane is javax.swing.JScrollPane) {
                        val viewport = scrollPane.viewport
                        dragStartScroll = viewport.viewPosition
                    }
                    cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                dragStartPoint = null
                dragStartScroll = null
                cursor = Cursor.getDefaultCursor()
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                val start = dragStartPoint ?: return
                val scrollStart = dragStartScroll ?: return
                val scrollPane = parent?.parent
                if (scrollPane is javax.swing.JScrollPane) {
                    val viewport = scrollPane.viewport
                    val dx = start.x - e.x
                    val dy = start.y - e.y
                    val newX = (scrollStart.x + dx).coerceIn(0, maxOf(0, preferredSize.width - viewport.width))
                    val newY = (scrollStart.y + dy).coerceIn(0, maxOf(0, preferredSize.height - viewport.height))
                    viewport.viewPosition = Point(newX, newY)
                }
            }
        })
    }

    private fun updatePreferredSize() {
        val img = levelImage
        preferredSize = if (img != null) {
            Dimension((img.width * zoomLevel).toInt(), (img.height * zoomLevel).toInt())
        } else {
            Dimension(400, 300)
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val img = levelImage ?: return
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        g2.scale(zoomLevel, zoomLevel)
        g2.drawImage(img, 0, 0, null)
    }
}
