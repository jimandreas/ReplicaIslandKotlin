package com.replica.replicaisland.ui

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.replica.replicaisland.R
import com.replica.replicaisland.core.BaseObject

class CustomToastSystem(context: Context) : BaseObject() {
    private val mView: View
    private val mText: TextView
    private val mToast: Toast
    override fun reset() {
    }

    fun toast(text: String?, length: Int) {
        mText.text = text
        mToast.setGravity(Gravity.CENTER, 0, 0)
        mToast.duration = length
        mToast.show()
    }

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        // TODO: fix this null passing
        mView = inflater.inflate(R.layout.custom_toast, null)
        mText = mView.findViewById<View>(R.id.text) as TextView
        mToast = Toast(context)
        mToast.view = mView
    }
}