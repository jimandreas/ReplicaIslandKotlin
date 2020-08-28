/*
 * Copyright (C) 2010 The Android Open Source Project
 *
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
@file:Suppress("SENSELESS_COMPARISON", "UNCHECKED_CAST")

package com.replica.replicaisland

import android.app.ListActivity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import java.lang.reflect.InvocationTargetException
import java.util.*

class LevelSelectActivity : ListActivity() {
    private var mLevelData: ArrayList<LevelMetaData>? = null
    private var mButtonFlickerAnimation: Animation? = null
    private var mLevelSelected = false
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
            private val mContext: Context,
            private val mRowResource: Int,
            private val mDisabledRowResource: Int,
            private val mCompletedRowResource: Int,
            private val mTextViewResource: Int,
            private val mTextViewResource2: Int,
            objects: List<T>?) : ArrayAdapter<T>(mContext, mRowResource, mTextViewResource, objects!!) {

        override fun isEnabled(position: Int): Boolean {
            // TODO: do we have separators in this list?
            return mLevelData!![position].enabled
        }

        override fun areAllItemsEnabled(): Boolean {
            return false
        }

        override fun getItemViewType(position: Int): Int {
            var type = Companion.TYPE_ENABLED
            val level = mLevelData!![position]
            if (level != null) {
                if (!level.enabled) {
                    type = if (level.level!!.completed) {
                        Companion.TYPE_COMPLETED
                    } else {
                        Companion.TYPE_DISABLED
                    }
                }
            }
            return type
        }

        override fun getViewTypeCount(): Int {
            return Companion.TYPE_COUNT
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun isEmpty(): Boolean {
            return mLevelData!!.size > 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val sourceView = if (mLevelData!![position].enabled) {
                if (convertView != null && convertView.id == mRowResource) {
                    convertView
                } else {
                    LayoutInflater.from(mContext).inflate(
                            mRowResource, parent, false)
                }
            } else if (mLevelData!![position].level!!.completed) {
                if (convertView != null && convertView.id == mCompletedRowResource) {
                    convertView
                } else {
                    LayoutInflater.from(mContext).inflate(
                            mCompletedRowResource, parent, false)
                }
            } else {
                if (convertView != null && convertView.id == mDisabledRowResource) {
                    convertView
                } else {
                    LayoutInflater.from(mContext).inflate(
                            mDisabledRowResource, parent, false)
                }
            }
            val view = sourceView!!.findViewById<View>(mTextViewResource) as TextView
            if (view != null) {
                view.text = mLevelData!![position].level!!.name
            }
            val view2 = sourceView.findViewById<View>(mTextViewResource2) as TextView
            if (view2 != null) {
                view2.text = mLevelData!![position].level!!.timeStamp
            }
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
        setContentView(R.layout.level_select)
        mLevelData = ArrayList()
        if (intent.getBooleanExtra("unlockAll", false)) {
            generateLevelList(false)
            for (level in mLevelData!!) {
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
                        mLevelData)

        adapter.sort(sLevelComparator)
        listAdapter = adapter
        mButtonFlickerAnimation = AnimationUtils.loadAnimation(this, R.anim.button_flicker)
        mLevelSelected = false

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
                if (enabled || level.completed || !onlyAllowThePast || onlyAllowThePast && level.inThePast) {
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

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        if (!mLevelSelected) {
            super.onListItemClick(l, v, position, id)
            val selectedLevel = mLevelData!![position]
            if (selectedLevel.enabled) {
                mLevelSelected = true
                val intent = Intent()
                intent.putExtra("resource", selectedLevel.level!!.resource)
                intent.putExtra("row", selectedLevel.x)
                intent.putExtra("index", selectedLevel.y)
                val text = v.findViewById<View>(R.id.title) as TextView
                if (text != null) {
                    text.startAnimation(mButtonFlickerAnimation)
                    mButtonFlickerAnimation!!.setAnimationListener(EndActivityAfterAnimation(intent))
                } else {
                    setResult(RESULT_OK, intent)
                    finish()
                    if (UIConstants.mOverridePendingTransition != null) {
                        try {
                            UIConstants.mOverridePendingTransition!!.invoke(this@LevelSelectActivity, R.anim.activity_fade_in, R.anim.activity_fade_out)
                        } catch (ite: InvocationTargetException) {
                            DebugLog.d("Activity Transition", "Invocation Target Exception")
                        } catch (ie: IllegalAccessException) {
                            DebugLog.d("Activity Transition", "Illegal Access Exception")
                        }
                    }
                }
            }
        }
    }

    private fun addItem(level: LevelTree.Level, x: Int, y: Int, enabled: Boolean) {
        val data = LevelMetaData()
        data.level = level
        data.x = x
        data.y = y
        data.enabled = enabled
        mLevelData!!.add(data)
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
                mLevelData!!.clear()
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
                mLevelData!!.clear()
                generateLevelList(false)
                for (level in mLevelData!!) {
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        var result = false
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            result = true
        }
        return result
    }

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

    private inner class EndActivityAfterAnimation
        constructor(private val mIntent: Intent) : AnimationListener {
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