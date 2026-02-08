package com.replica.levelviewer.ui

import com.replica.levelviewer.data.CollisionParser
import com.replica.levelviewer.data.LevelFileParser
import com.replica.levelviewer.data.LevelTreeParser
import com.replica.levelviewer.model.CollisionData
import com.replica.levelviewer.model.LevelData
import com.replica.levelviewer.model.LevelEntry
import com.replica.levelviewer.rendering.CollisionRenderer
import com.replica.levelviewer.rendering.LevelRenderer
import com.replica.levelviewer.rendering.TileAtlasCache
import java.awt.BorderLayout
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import javax.imageio.ImageIO
import javax.swing.*

class LevelViewerFrame(private val projectRoot: File) : JFrame("Replica Island Level Viewer") {

    private val drawableDir = File(projectRoot, "app/src/main/res/drawable")
    private val rawDir = File(projectRoot, "app/src/main/res/raw")
    private val xmlDir = File(projectRoot, "app/src/main/res/xml")
    private val valuesDir = File(projectRoot, "app/src/main/res/values")

    private val atlasCache = TileAtlasCache(drawableDir)
    private val levelRenderer = LevelRenderer(atlasCache)
    private val collisionRenderer = CollisionRenderer()

    private val canvas = LevelCanvas()
    private var currentLevel: LevelData? = null
    private var currentImage: BufferedImage? = null
    private var collisionData: CollisionData? = null
    private var showCollision = false
    private var showObjects = false

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1200, 800)
        setLocationRelativeTo(null)

        loadCollisionData()

        val levels = loadLevelTree()
        val listPanel = LevelListPanel(levels) { entry -> loadLevel(entry) }

        val toolbar = ToolBar(
            onZoomChanged = { zoom -> canvas.zoomLevel = zoom },
            onCollisionToggled = { enabled ->
                showCollision = enabled
                rerenderCurrentLevel()
            },
            onObjectsToggled = { enabled ->
                showObjects = enabled
                rerenderCurrentLevel()
            },
            onExportPng = { exportPng() }
        )

        canvas.addPropertyChangeListener("zoomLevel") { evt ->
            toolbar.setZoomValue(evt.newValue as Double)
        }

        val scrollPane = JScrollPane(canvas)
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, scrollPane)
        splitPane.dividerLocation = 280

        layout = BorderLayout()
        add(toolbar, BorderLayout.NORTH)
        add(splitPane, BorderLayout.CENTER)
    }

    private fun loadCollisionData() {
        val collisionFile = File(rawDir, "collision.bin")
        if (collisionFile.exists()) {
            FileInputStream(collisionFile).use { stream ->
                collisionData = CollisionParser.parse(stream)
            }
        }
    }

    private fun loadLevelTree(): List<LevelEntry> {
        val levelTreeFile = File(xmlDir, "level_tree.xml")
        val stringsFile = File(valuesDir, "strings.xml")
        return if (levelTreeFile.exists() && stringsFile.exists()) {
            LevelTreeParser.parse(levelTreeFile, stringsFile)
        } else {
            // Fallback: enumerate .bin files in raw/
            rawDir.listFiles { f -> f.name.startsWith("level_") && f.name.endsWith(".bin") }
                ?.sortedBy { it.name }
                ?.mapIndexed { i, f ->
                    LevelEntry(f.nameWithoutExtension, f.nameWithoutExtension, i)
                } ?: emptyList()
        }
    }

    private fun loadLevel(entry: LevelEntry) {
        val file = File(rawDir, "${entry.filename}.bin")
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "Level file not found: ${file.name}", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }

        try {
            FileInputStream(file).use { stream ->
                currentLevel = LevelFileParser.parse(stream)
            }
            rerenderCurrentLevel()
            title = "Replica Island Level Viewer - ${entry.name}"
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "Error loading level: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
            e.printStackTrace()
        }
    }

    private fun rerenderCurrentLevel() {
        val level = currentLevel ?: return
        try {
            val image = levelRenderer.render(level, showObjects)

            if (showCollision) {
                val colLayer = level.collisionLayer
                val colData = collisionData
                if (colLayer != null && colData != null) {
                    collisionRenderer.render(image, colLayer, colData)
                }
            }

            currentImage = image
            canvas.levelImage = image
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "Error rendering level: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
            e.printStackTrace()
        }
    }

    private fun exportPng() {
        val image = currentImage
        if (image == null) {
            JOptionPane.showMessageDialog(this, "No level loaded to export.", "Export", JOptionPane.WARNING_MESSAGE)
            return
        }

        val fileChooser = JFileChooser()
        fileChooser.selectedFile = File("level_export.png")
        fileChooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter("PNG Images", "png")
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            var outFile = fileChooser.selectedFile
            if (!outFile.name.endsWith(".png")) {
                outFile = File(outFile.parentFile, outFile.name + ".png")
            }
            ImageIO.write(image, "PNG", outFile)
            JOptionPane.showMessageDialog(this, "Exported to ${outFile.absolutePath}", "Export", JOptionPane.INFORMATION_MESSAGE)
        }
    }
}
