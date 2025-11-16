package com.replica.replicaisland.ui

import android.R
import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet

class YesNoDialogPreference @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null,
                                                      defStyle: Int = R.attr.yesNoPreferenceStyle
) : DialogPreference(context, attrs, defStyle) {
    private var mListener: YesNoDialogListener? = null

    interface YesNoDialogListener {
        fun onDialogClosed(positiveResult: Boolean)
    }

    fun setListener(listener: YesNoDialogListener?) {
        mListener = listener
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (mListener != null) {
            mListener!!.onDialogClosed(positiveResult)
        }
    }
}