@file:Suppress("CanBeParameter", "CanBeParameter")

package com.replica.levelviewer.ui

import com.replica.levelviewer.model.LevelEntry
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

class LevelListPanel(
    private val levels: List<LevelEntry>,
    private val onLevelSelected: (LevelEntry) -> Unit
) : JScrollPane() {

    private val tree: JTree

    init {
        val root = DefaultMutableTreeNode("Levels")

        val groups = levels.groupBy { it.groupIndex }
        for ((groupIndex, groupLevels) in groups.toSortedMap()) {
            val groupNode = DefaultMutableTreeNode("Group $groupIndex")
            for (level in groupLevels) {
                groupNode.add(DefaultMutableTreeNode(level))
            }
            root.add(groupNode)
        }

        tree = JTree(DefaultTreeModel(root))
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION

        tree.setCellRenderer(object : javax.swing.tree.DefaultTreeCellRenderer() {
            override fun getTreeCellRendererComponent(
                tree: JTree, value: Any?, sel: Boolean, expanded: Boolean,
                leaf: Boolean, row: Int, hasFocus: Boolean
            ): java.awt.Component {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
                val node = value as? DefaultMutableTreeNode
                val userObj = node?.userObject
                if (userObj is LevelEntry) {
                    text = "${userObj.name} (${userObj.filename})"
                }
                return this
            }
        })

        tree.addTreeSelectionListener { e ->
            val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return@addTreeSelectionListener
            val entry = node.userObject as? LevelEntry ?: return@addTreeSelectionListener
            onLevelSelected(entry)
        }

        setViewportView(tree)
        expandAllNodes(tree)
        preferredSize = java.awt.Dimension(280, 400)

    }

    fun expandAllNodes(tree: JTree) {
        var i = 0
        while (i < tree.rowCount) {
            tree.expandRow(i)
            i++
        }
    }
}
