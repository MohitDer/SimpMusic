package com.maxrave.simpmusic.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlin.math.abs

class AppSwipeRefreshLayout(context: Context, attrs: AttributeSet) : SwipeRefreshLayout(context, attrs) {

    private val mTouchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private var mPrevX: Float = 0f
    private var mDeclined: Boolean = false

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mPrevX = event.x
                mDeclined = false
            }

            MotionEvent.ACTION_MOVE -> {
                val eventX = event.x
                val xDiff = abs(eventX - mPrevX)
                // Check if the movement is greater than the slop threshold
                if (xDiff > mTouchSlop) {
                    mDeclined = true
                    return false // Do not intercept, allow child to handle it
                }
            }
        }
        return !mDeclined && super.onInterceptTouchEvent(event)
    }
}