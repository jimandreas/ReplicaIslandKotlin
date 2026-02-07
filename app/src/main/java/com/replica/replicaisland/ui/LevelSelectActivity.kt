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

@file:Suppress("DEPRECATION",
    "SimplifyBooleanWithConstants",
    "KotlinConstantConditions", "UNCHECKED_CAST"
)

package com.replica.replicaisland.ui

import android.annotation.SuppressLint
import android.app.ListActivity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.replica.replicaisland.AndouKun
import com.replica.replicaisland.R
import com.replica.replicaisland.levels.LevelTree

/**
 * @deprecated Use [LevelSelectDialogFragment] instead for level selection.
 * This activity is kept for rollback purposes.
 */
@Deprecated("Use LevelSelectDialogFragment instead", ReplaceWith("LevelSelectDialogFragment"))
class LevelSelectActivity : ListActivity() {
    private var levelData: ArrayList<LevelMetaData>? = null
    private var buttonFlickerAnimation: Animation? = null
    private var levelSelected = false
    private val sLevelComparator = LevelDataComparator()

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
        objects: List<T>?) : ArrayAdapter<T>(contextLocal, rowResource, textViewResource, objects!!) {

        override fun isEnabled(position: Int): Boolean {
            // TODO: do we have separators in this list?
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
                            rowResource, parent, false)
                }
            } else if (levelData!![position].level!!.completed) {
                if (convertView != null && convertView.id == completedRowResource) {
                    convertView
                } else {
                    LayoutInflater.from(contextLocal).inflate(
                            completedRowResource, parent, false)
                }
            } else {
                if (convertView != null && convertView.id == disabledRowResource) {
                    convertView
                } else {
                    LayoutInflater.from(contextLocal).inflate(
                            disabledRowResource, parent, false)
                }
            }
            val view = sourceView!!.findViewById<View>(textViewResource) as TextView
            view.text = levelData!![position].level!!.name
            val view2 = sourceView.findViewById<View>(textViewResource2) as TextView
            view2.text = levelData!![position].level!!.timeStamp
            return sourceView
        }


    }

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            super.onCreate(savedInstanceState)
        } else {
            super.onCreate(null)
        }

        // New method of landscape orientation.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        // end of new method of landscape orientation

        setContentView(R.layout.level_select)
        levelData = ArrayList()
        if (intent.getBooleanExtra("unlockAll", false)) {
            generateLevelList(false)
            for (level in levelData!!) {
                level.enabled = true
            }
        } else {
            generateLevelList(true)
        }
        val adapter: DisableItemArrayAdapter<LevelMetaData> =
                DisableItemArrayAdapter(
                        this,
                        R.layout.level_select_row,
                        R.layout.level_select_disabled_row,
                        R.layout.level_select_completed_row,
                        R.id.title,
                        R.id.time,
                        levelData)

        adapter.sort(sLevelComparator)
        listAdapter = adapter
        buttonFlickerAnimation = AnimationUtils.loadAnimation(this, R.anim.button_flicker)
        levelSelected = false

        // Keep the volume control type consistent across all activities.
        volumeControlStream = AudioManager.STREAM_MUSIC
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

    @Deprecated("Deprecated in Java")
    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        if (!levelSelected) {
            super.onListItemClick(l, v, position, id)
            val selectedLevel = levelData!![position]
            if (selectedLevel.enabled) {
                levelSelected = true
                val intent = Intent()
                intent.putExtra("resource", selectedLevel.level!!.resource)
                intent.putExtra("row", selectedLevel.x)
                intent.putExtra("index", selectedLevel.y)
                val text = v.findViewById<View>(R.id.title) as TextView
                text.startAnimation(buttonFlickerAnimation)
                buttonFlickerAnimation!!.setAnimationListener(EndActivityAfterAnimation(intent))
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        var handled = false
        if (AndouKun.VERSION < 0) {
            menu.add(0, UNLOCK_NEXT_LEVEL_ID, 0, R.string.unlock_next_level)
            menu.add(0, UNLOCK_ALL_LEVELS_ID, 0, R.string.unlock_levels)
            handled = true
        }
        return handled
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        when (item.itemId) {
            UNLOCK_NEXT_LEVEL_ID -> {
                unlockNext()
                levelData!!.clear()
                generateLevelList(false)

                // TODO: needs attention
                val sorter = LevelDataComparator()
                (listAdapter as ArrayAdapter<*>).sort(sorter as Comparator<in Any>)
                //(listAdapter as ArrayAdapter<*>).sort(sLevelComparator)
                (listAdapter as ArrayAdapter<*>).notifyDataSetChanged()
                return true
            }
            UNLOCK_ALL_LEVELS_ID -> {
                // Regenerate the level list to remove the past-only filter.
                levelData!!.clear()
                generateLevelList(false)
                for (level in levelData!!) {
                    level.enabled = true
                }

                // TODO: needs attention
                val sorter = LevelDataComparator()
                (listAdapter as ArrayAdapter<*>).sort(sorter as Comparator<in Any>)
                //(listAdapter as ArrayAdapter<*>).sort(sLevelComparator)
                (listAdapter as ArrayAdapter<*>).notifyDataSetChanged()
                return true
            }
        }
        return super.onMenuItemSelected(featureId, item)
    }

    @SuppressLint("GestureBackNavigation")
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        var result = false
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            result = true
        }
        return result
    }

    @SuppressLint("GestureBackNavigation")
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        var result = false
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            result = true
        }
        return result
    }

    /** Comparator for level meta data.  */
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

    private inner class EndActivityAfterAnimation(private val mIntent: Intent) : Animation.AnimationListener {
        override fun onAnimationEnd(animation: Animation) {
            setResult(RESULT_OK, mIntent)
            finish()
        }

        override fun onAnimationRepeat(animation: Animation) {
            // TODO Auto-generated method stub
        }

        override fun onAnimationStart(animation: Animation) {
            // TODO Auto-generated method stub
        }
    }

    companion object {
        private const val UNLOCK_ALL_LEVELS_ID = 0
        private const val UNLOCK_NEXT_LEVEL_ID = 1


        private const val TYPE_ENABLED = 0
        private const val TYPE_DISABLED = 1
        private const val TYPE_COMPLETED = 2
        private const val TYPE_COUNT = 3

    }
}