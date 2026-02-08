package com.replica.levelviewer.data

import com.replica.levelviewer.model.LevelEntry
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object LevelTreeParser {

    fun parse(levelTreeFile: File, stringsFile: File): List<LevelEntry> {
        val strings = parseStrings(stringsFile)
        val entries = mutableListOf<LevelEntry>()

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(levelTreeFile)
        val groups = doc.getElementsByTagName("group")

        for (g in 0 until groups.length) {
            val group = groups.item(g)
            val levels = (group as org.w3c.dom.Element).getElementsByTagName("level")
            for (l in 0 until levels.length) {
                val level = levels.item(l) as org.w3c.dom.Element
                val resource = level.getAttribute("resource") // e.g. "@raw/level_0_1_sewer"
                val titleRef = level.getAttribute("title")     // e.g. "@string/level_0_1_sewer"

                val filename = resource.removePrefix("@raw/")
                val stringKey = titleRef.removePrefix("@string/")
                val name = strings[stringKey] ?: filename

                entries.add(LevelEntry(name = name, filename = filename, groupIndex = g))
            }
        }

        return entries
    }

    private fun parseStrings(stringsFile: File): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsFile)
        val stringNodes = doc.getElementsByTagName("string")
        val map = mutableMapOf<String, String>()

        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i) as org.w3c.dom.Element
            val name = node.getAttribute("name")
            val value = node.textContent
            map[name] = value
        }

        return map
    }
}
