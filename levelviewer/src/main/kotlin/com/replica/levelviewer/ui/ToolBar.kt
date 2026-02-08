package com.replica.levelviewer.ui

import java.awt.Dimension
import javax.swing.*

class ToolBar(
    private val onZoomChanged: (Double) -> Unit,
    private val onCollisionToggled: (Boolean) -> Unit,
    private val onObjectsToggled: (Boolean) -> Unit,
    private val onExportPng: () -> Unit
) : JToolBar() {

    val zoomSlider: JSlider
    val collisionCheckBox: JCheckBox
    val objectsCheckBox: JCheckBox

    init {
        isFloatable = false

        add(JLabel(" Zoom: "))

        zoomSlider = JSlider(10, 400, 100)
        zoomSlider.preferredSize = Dimension(200, 24)
        zoomSlider.maximumSize = Dimension(200, 24)
        zoomSlider.addChangeListener {
            onZoomChanged(zoomSlider.value / 100.0)
        }
        add(zoomSlider)

        addSeparator()

        collisionCheckBox = JCheckBox("Collision", false)
        collisionCheckBox.addActionListener { onCollisionToggled(collisionCheckBox.isSelected) }
        add(collisionCheckBox)

        objectsCheckBox = JCheckBox("Objects", false)
        objectsCheckBox.addActionListener { onObjectsToggled(objectsCheckBox.isSelected) }
        add(objectsCheckBox)

        addSeparator()

        val exportButton = JButton("Export PNG")
        exportButton.addActionListener { onExportPng() }
        add(exportButton)
    }

    fun setZoomValue(zoom: Double) {
        zoomSlider.value = (zoom * 100).toInt()
    }
}
