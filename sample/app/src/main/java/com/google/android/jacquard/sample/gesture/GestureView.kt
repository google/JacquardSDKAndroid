/*
 * Copyright 2021 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sample.gesture

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.gesture.GestureViewModel.GestureViewItem

/** Overlay for showing the performed gesture.  */
class GestureView : RelativeLayout {
    private lateinit var gestureImageView: ImageView
    private lateinit var gestureTextView: TextView

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        setBackgroundResource(R.color.gesture_overlay_color)
        inflate(context, R.layout.gesture_overlay, this)
        gestureImageView = findViewById(R.id.gesture_overlay_imageview)
        gestureTextView = findViewById(R.id.gesture_overlay_textview)
    }

    /** Animated in/out this view while showing the provided gesture.  */
    fun show(item: GestureViewItem) {
        gestureImageView.setImageResource(item.drawableResId)
        gestureTextView.text = item.gesture.gestureType().description
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = ANIMATION_DURATION_IN_MS.toLong()
        animator.interpolator = AccelerateInterpolator()
        animator.repeatCount = 1
        animator.repeatMode = ValueAnimator.REVERSE
        animator.addUpdateListener { animation: ValueAnimator ->
            val alpha = animation.animatedValue as Float
            setAlpha(alpha)
        }
        animator.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = GONE
                }

                override fun onAnimationStart(animation: Animator) {
                    visibility = VISIBLE
                }
            })
        animator.start()
    }

    companion object {
        private const val ANIMATION_DURATION_IN_MS = 1000
    }
}