package com.replica.replicaisland.ui

import android.content.Context
import android.content.res.TypedArray
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import com.replica.replicaisland.R
import com.replica.replicaisland.Utils

class SliderPreference : Preference, SeekBar.OnSeekBarChangeListener {
    private var value = INITIAL_VALUE
    private var mMinText: String? = null
    private var mMaxText: String? = null

    constructor(context: Context?) : super(context) {
        widgetLayoutResource = R.layout.slider_preference
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, android.R.attr.preferenceStyle) {
        widgetLayoutResource = R.layout.slider_preference
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        val a = context.obtainStyledAttributes(attrs,
                R.styleable.SliderPreference, defStyle, 0)
        mMinText = a.getString(R.styleable.SliderPreference_minText)
        mMaxText = a.getString(R.styleable.SliderPreference_maxText)
        a.recycle()
        widgetLayoutResource = R.layout.slider_preference
    }

    override fun onBindView(view: View) {
        super.onBindView(view)
        if (mMinText != null) {
            val minText = view.findViewById<View>(R.id.min) as TextView
            minText.text = mMinText
        }
        if (mMaxText != null) {
            val maxText = view.findViewById<View>(R.id.max) as TextView
            maxText.text = mMaxText
        }
        val bar = view.findViewById<View>(R.id.slider) as SeekBar
        bar.max = MAX_SLIDER_VALUE
        bar.progress = value
        bar.setOnSeekBarChangeListener(this)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            value = progress
            persistInt(value)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}
    override fun onGetDefaultValue(ta: TypedArray, index: Int): Any {
        return Utils.Companion.clamp(ta.getInt(index, INITIAL_VALUE), 0, MAX_SLIDER_VALUE)
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        value = if (defaultValue != null) defaultValue as Int else INITIAL_VALUE
        if (!restoreValue) {
            persistInt(value)
        } else {
            value = getPersistedInt(value)
        }
    }

    companion object {
        private const val MAX_SLIDER_VALUE = 100
        private const val INITIAL_VALUE = 50
    }
}