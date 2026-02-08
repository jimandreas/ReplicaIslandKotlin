package com.replica.levelviewer

import com.replica.levelviewer.data.LevelTreeParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import java.io.File

class LevelTreeParserTest {

    private fun findProjectRoot(): File? {
        var dir = File(".").canonicalFile
        while (dir.parentFile != null) {
            if (File(dir, "app/src/main/res/xml/level_tree.xml").exists()) return dir
            dir = dir.parentFile
        }
        return null
    }

    fun filesExist(): Boolean {
        val root = findProjectRoot() ?: return false
        return File(root, "app/src/main/res/xml/level_tree.xml").exists() &&
                File(root, "app/src/main/res/values/strings.xml").exists()
    }

    @Test
    @EnabledIf("filesExist")
    fun `parse level tree from actual game data`() {
        val root = findProjectRoot()!!
        val levelTree = File(root, "app/src/main/res/xml/level_tree.xml")
        val strings = File(root, "app/src/main/res/values/strings.xml")

        val entries = LevelTreeParser.parse(levelTree, strings)

        assertTrue(entries.isNotEmpty(), "Should have parsed at least one level")

        // First level should be level_0_1_sewer
        val first = entries.first()
        assertEquals("level_0_1_sewer", first.filename)
        assertEquals("Memory #000", first.name)
        assertEquals(0, first.groupIndex)

        // Verify we have multiple groups
        val groups = entries.map { it.groupIndex }.distinct()
        assertTrue(groups.size > 10, "Should have many groups, got ${groups.size}")

        // Check the final boss level exists
        val boss = entries.find { it.filename == "level_final_boss_lab" }
        assertNotNull(boss, "Should have final boss level")
        assertEquals("Memory #040", boss!!.name)
    }
}
