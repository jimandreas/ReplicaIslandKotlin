/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2025 Jim Andreas kotlin conversion
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("DEPRECATION", "UNCHECKED_CAST")

package com.replica.replicaisland.ui

import android.app.Dialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import com.replica.replicaisland.AndouKun
import com.replica.replicaisland.R
import com.replica.replicaisland.levels.LevelTree

/**
 * DialogFragment that displays the level selection list.
 * Replaces LevelSelectActivity with a modern fragment-based approach using Fragment Result API.
 */
class LevelSelectDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_UNLOCK_ALL = "unlockAll"
        const val TAG = "LevelSelectDialogFragment"

        private const val UNLOCK_ALL_LEVELS_ID = 0
        private const val UNLOCK_NEXT_LEVEL_ID = 1

        private const val TYPE_ENABLED = 0
        private const val TYPE_DISABLED = 1
        private const val TYPE_COMPLETED = 2
        private const val TYPE_COUNT = 3

        /**
         * Factory method to create a new instance with unlock option.
         */
        fun newInstance(unlockAll: Boolean): LevelSelectDialogFragment {
            return LevelSelectDialogFragment().apply {
                arguments = bundleOf(ARG_UNLOCK_ALL to unlockAll)
            }
        }
    }

    private var levelData: ArrayList<LevelMetaData>? = null
    private var buttonFlickerAnimation: Animation? = null
    private var levelSelected = false
    private val sLevelComparator = LevelDataComparator()
    private var listView: ListView? = null
    private var adapter: DisableItemArrayAdapter<LevelMetaData>? = null

    private class LevelMetaData {
        var level: LevelTree.Level? = null
        var x = 0
        var y = 0
        var enabled = false
        override fun toString(): String {
            return level!!.name!!
        }
    }

    private inner class DisableItemArrayAdapter<T>(
        private val contextLocal: Context,
        private val rowResource: Int,
        private val disabledRowResource: Int,
        private val completedRowResource: Int,
        private val textViewResource: Int,
        private val textViewResource2: Int,
        objects: List<T>?
    ) : ArrayAdapter<T>(contextLocal, rowResource, textViewResource, objects!!) {

        override fun isEnabled(position: Int): Boolean {
            return levelData!![position].enabled
        }

        override fun areAllItemsEnabled(): Boolean {
            return false
        }

        override fun getItemViewType(position: Int): Int {
            var type = TYPE_ENABLED
            val level = levelData!![position]
            if (!level.enabled) {
                type = if (level.level!!.completed) {
                    TYPE_COMPLETED
                } else {
                    TYPE_DISABLED
                }
            }
            return type
        }

        override fun getViewTypeCount(): Int {
            return TYPE_COUNT
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun isEmpty(): Boolean {
            return levelData!!.isNotEmpty()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val sourceView = if (levelData!![position].enabled) {
                if (convertView != null && convertView.id == rowResource) {
                    convertView
                } else {
                    LayoutInflater.from(contextLocal).inflate(
                        rowResource, parent, false
                    )
                }
            } else if (levelData!![position].level!!.completed) {
                if (convertView != null && convertView.id == completedRowResource) {
                    convertView
                } else {
                    LayoutInflater.from(contextLocal).inflate(
                        completedRowResource, parent, false
                    )
                }
            } else {
                if (convertView != null && convertView.id == disabledRowResource) {
                    convertView
                } else {
                    LayoutInflater.from(contextLocal).inflate(
                        disabledRowResource, parent, false
                    )
                }
            }
            val view = sourceView!!.findViewById<View>(textViewResource) as TextView
            view.text = levelData!![position].level!!.name
            val view2 = sourceView.findViewById<View>(textViewResource2) as TextView
            view2.text = levelData!![position].level!!.timeStamp
            return sourceView
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_FullscreenDialogFragment)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_level_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Force landscape orientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        listView = view.findViewById(R.id.level_list)
        levelData = ArrayList()

        val unlockAll = arguments?.getBoolean(ARG_UNLOCK_ALL, false) ?: false
        if (unlockAll) {
            generateLevelList(false)
            for (level in levelData!!) {
                level.enabled = true
            }
        } else {
            generateLevelList(true)
        }

        adapter = DisableItemArrayAdapter(
            requireContext(),
            R.layout.level_select_row,
            R.layout.level_select_disabled_row,
            R.layout.level_select_completed_row,
            R.id.title,
            R.id.time,
            levelData
        )

        adapter!!.sort(sLevelComparator)
        listView!!.adapter = adapter

        buttonFlickerAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.button_flicker)
        levelSelected = false

        // Keep the volume control type consistent across all activities.
        activity?.volumeControlStream = AudioManager.STREAM_MUSIC

        // Set up item click listener
        listView!!.onItemClickListener = AdapterView.OnItemClickListener { _, v, position, _ ->
            onListItemClick(position, v)
        }

        // Set up menu for debug mode
        if (AndouKun.VERSION < 0) {
            requireActivity().addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menu.add(0, UNLOCK_NEXT_LEVEL_ID, 0, R.string.unlock_next_level)
                    menu.add(0, UNLOCK_ALL_LEVELS_ID, 0, R.string.unlock_levels)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        UNLOCK_NEXT_LEVEL_ID -> {
                            unlockNext()
                            levelData!!.clear()
                            generateLevelList(false)
                            val sorter = LevelDataComparator()
                            adapter!!.sort(sorter as Comparator<in LevelMetaData>)
                            adapter!!.notifyDataSetChanged()
                            true
                        }
                        UNLOCK_ALL_LEVELS_ID -> {
                            levelData!!.clear()
                            generateLevelList(false)
                            for (level in levelData!!) {
                                level.enabled = true
                            }
                            val sorter = LevelDataComparator()
                            adapter!!.sort(sorter as Comparator<in LevelMetaData>)
                            adapter!!.notifyDataSetChanged()
                            true
                        }
                        else -> false
                    }
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }
    }

    private fun generateLevelList(onlyAllowThePast: Boolean) {
        val count = LevelTree.levels.size
        var oneBranchUnlocked = false
        for (x in 0 until count) {
            var anyUnlocksThisBranch = false
            val group = LevelTree.levels[x]
            for (y in group.levels.indices) {
                val level = group.levels[y]
                var enabled = false
                if (!level.completed && !oneBranchUnlocked) {
                    enabled = true
                    anyUnlocksThisBranch = true
                }
                if (enabled || level.completed || !onlyAllowThePast || level.inThePast) {
                    addItem(level, x, y, enabled)
                }
            }
            if (anyUnlocksThisBranch) {
                oneBranchUnlocked = true
            }
        }
    }

    private fun unlockNext() {
        val count = LevelTree.levels.size
        for (x in 0 until count) {
            val group = LevelTree.levels[x]
            for (y in group.levels.indices) {
                val level = group.levels[y]
                if (!level.completed) {
                    level.completed = true
                    return
                }
            }
        }
    }

    private fun onListItemClick(position: Int, v: View) {
        if (!levelSelected) {
            val selectedLevel = levelData!![position]
            if (selectedLevel.enabled) {
                levelSelected = true
                val text = v.findViewById<View>(R.id.title) as TextView
                text.startAnimation(buttonFlickerAnimation)
                buttonFlickerAnimation!!.setAnimationListener(EndDialogAfterAnimation(
                    selectedLevel.level!!.resource,
                    selectedLevel.x,
                    selectedLevel.y
                ))
            }
        }
    }

    private fun addItem(level: LevelTree.Level, x: Int, y: Int, enabled: Boolean) {
        val data = LevelMetaData()
        data.level = level
        data.x = x
        data.y = y
        data.enabled = enabled
        levelData!!.add(data)
    }

    /** Comparator for level meta data. */
    private class LevelDataComparator : Comparator<LevelMetaData?> {
        override fun compare(object1: LevelMetaData?, object2: LevelMetaData?): Int {
            var result = 0
            if (object1 == null && object2 != null) {
                result = 1
            } else if (object1 != null && object2 == null) {
                result = -1
            } else if (object1 != null && object2 != null) {
                result = object1.level!!.timeStamp!!.compareTo(object2.level!!.timeStamp!!)
            }
            return result
        }
    }

    private inner class EndDialogAfterAnimation(
        private val resource: Int,
        private val row: Int,
        private val index: Int
    ) : Animation.AnimationListener {

        override fun onAnimationEnd(animation: Animation) {
            // Use Fragment Result API to communicate the selection back
            parentFragmentManager.setFragmentResult(
                LevelSelectResult.REQUEST_KEY,
                bundleOf(
                    LevelSelectResult.RESULT_RESOURCE to resource,
                    LevelSelectResult.RESULT_ROW to row,
                    LevelSelectResult.RESULT_INDEX to index
                )
            )
            dismiss()
        }

        override fun onAnimationRepeat(animation: Animation) {
            // Not used
        }

        override fun onAnimationStart(animation: Animation) {
            // Not used
        }
    }

    override fun onStart() {
        super.onStart()
        // Ensure dialog is fullscreen
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
    }
}
