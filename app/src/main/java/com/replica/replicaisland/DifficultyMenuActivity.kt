package com.replica.replicaisland

import android.app.Activity
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import java.lang.reflect.InvocationTargetException

class DifficultyMenuActivity : Activity() {
    private var mBabyButton: View? = null
    private var mKidsButton: View? = null
    private var mAdultsButton: View? = null
    private var mBackground: View? = null
    private var mBabyText: View? = null
    private var mKidsText: View? = null
    private var mAdultsText: View? = null
    private var mButtonFlickerAnimation: Animation? = null
    private var mFadeOutAnimation: Animation? = null
    private var mAlternateFadeOutAnimation: Animation? = null
    private val sBabyButtonListener = View.OnClickListener { v ->
        val i = Intent(baseContext, AndouKun::class.java)
        i.putExtras(intent)
        i.putExtra("difficulty", 0)
        v.startAnimation(mButtonFlickerAnimation)
        mFadeOutAnimation!!.setAnimationListener(StartActivityAfterAnimation(i))
        mBackground!!.startAnimation(mFadeOutAnimation)
        mKidsButton!!.startAnimation(mAlternateFadeOutAnimation)
        mAdultsButton!!.startAnimation(mAlternateFadeOutAnimation)
        mBabyText!!.startAnimation(mAlternateFadeOutAnimation)
        mKidsText!!.startAnimation(mAlternateFadeOutAnimation)
        mAdultsText!!.startAnimation(mAlternateFadeOutAnimation)
    }
    private val sKidsButtonListener = View.OnClickListener { v ->
        val i = Intent(baseContext, AndouKun::class.java)
        i.putExtras(intent)
        i.putExtra("difficulty", 1)
        v.startAnimation(mButtonFlickerAnimation)
        mFadeOutAnimation!!.setAnimationListener(StartActivityAfterAnimation(i))
        mBackground!!.startAnimation(mFadeOutAnimation)
        mBabyButton!!.startAnimation(mAlternateFadeOutAnimation)
        mAdultsButton!!.startAnimation(mAlternateFadeOutAnimation)
        mBabyText!!.startAnimation(mAlternateFadeOutAnimation)
        mKidsText!!.startAnimation(mAlternateFadeOutAnimation)
        mAdultsText!!.startAnimation(mAlternateFadeOutAnimation)
    }
    private val sAdultsButtonListener = View.OnClickListener { v ->
        val i = Intent(baseContext, AndouKun::class.java)
        i.putExtras(intent)
        i.putExtra("difficulty", 2)
        v.startAnimation(mButtonFlickerAnimation)
        mFadeOutAnimation!!.setAnimationListener(StartActivityAfterAnimation(i))
        mBackground!!.startAnimation(mFadeOutAnimation)
        mBabyButton!!.startAnimation(mAlternateFadeOutAnimation)
        mKidsButton!!.startAnimation(mAlternateFadeOutAnimation)
        mBabyText!!.startAnimation(mAlternateFadeOutAnimation)
        mKidsText!!.startAnimation(mAlternateFadeOutAnimation)
        mAdultsText!!.startAnimation(mAlternateFadeOutAnimation)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            super.onCreate(savedInstanceState)
        } else {
            super.onCreate(null)
        }
        setContentView(R.layout.difficulty_menu)
        mBabyButton = findViewById(R.id.babyButton)
        mKidsButton = findViewById(R.id.kidsButton)
        mAdultsButton = findViewById(R.id.adultsButton)
        mBabyText = findViewById(R.id.babyText)
        mKidsText = findViewById(R.id.kidsText)
        mAdultsText = findViewById(R.id.adultsText)
        mBackground = findViewById(R.id.mainMenuBackground)
        mBabyButton!!.setOnClickListener(sBabyButtonListener)
        mKidsButton!!.setOnClickListener(sKidsButtonListener)
        mAdultsButton!!.setOnClickListener(sAdultsButtonListener)
        mButtonFlickerAnimation = AnimationUtils.loadAnimation(this, R.anim.button_flicker)
        mFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        mAlternateFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)

        // Keep the volume control type consistent across all activities.
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        var result = true
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            if (UIConstants.mOverridePendingTransition != null) {
                try {
                    UIConstants.mOverridePendingTransition!!.invoke(this@DifficultyMenuActivity, R.anim.activity_fade_in, R.anim.activity_fade_out)
                } catch (ite: InvocationTargetException) {
                    DebugLog.d("Activity Transition", "Invocation Target Exception")
                } catch (ie: IllegalAccessException) {
                    DebugLog.d("Activity Transition", "Illegal Access Exception")
                }
            }
        } else {
            result = super.onKeyDown(keyCode, event)
        }
        return result
    }

    private inner class StartActivityAfterAnimation constructor(private val mIntent: Intent) : AnimationListener {
        override fun onAnimationEnd(animation: Animation) {
            mBabyButton!!.visibility = View.INVISIBLE
            mBabyButton!!.clearAnimation()
            mKidsButton!!.visibility = View.INVISIBLE
            mKidsButton!!.clearAnimation()
            mAdultsButton!!.visibility = View.INVISIBLE
            mAdultsButton!!.clearAnimation()
            startActivity(mIntent)
            finish() // This activity dies when it spawns a new intent.
            if (UIConstants.mOverridePendingTransition != null) {
                try {
                    UIConstants.mOverridePendingTransition!!.invoke(this@DifficultyMenuActivity, R.anim.activity_fade_in, R.anim.activity_fade_out)
                } catch (ite: InvocationTargetException) {
                    DebugLog.d("Activity Transition", "Invocation Target Exception")
                } catch (ie: IllegalAccessException) {
                    DebugLog.d("Activity Transition", "Illegal Access Exception")
                }
            }
        }

        override fun onAnimationRepeat(animation: Animation) {
            // TODO Auto-generated method stub
        }

        override fun onAnimationStart(animation: Animation) {
            // TODO Auto-generated method stub
        }
    }
}