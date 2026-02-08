package com.replica.levelviewer

import com.replica.levelviewer.ui.LevelViewerFrame
import java.io.File
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main(args: Array<String>) {
    val projectRoot = if (args.isNotEmpty()) {
        File(args[0])
    } else {
        // Walk up from the current directory to find the project root
        findProjectRoot() ?: File(".")
    }

    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

    SwingUtilities.invokeLater {
        val frame = LevelViewerFrame(projectRoot)
        frame.isVisible = true
    }
}

private fun findProjectRoot(): File? {
    var dir = File(".").canonicalFile
    while (dir.parentFile != null) {
        if (File(dir, "app/src/main/res/raw").exists() &&
            File(dir, "app/src/main/res/drawable").exists()) {
            return dir
        }
        dir = dir.parentFile
    }
    return null
}
