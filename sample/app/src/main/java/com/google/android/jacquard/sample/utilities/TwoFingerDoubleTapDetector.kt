/*
 * Copyright 2021 Google LLC
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
package com.google.android.jacquard.sample.utilities

import android.view.MotionEvent
import android.view.ViewConfiguration

/*
 * Class to detect Two Fingers double tap on a particular view.
 */
abstract class TwoFingerDoubleTapDetector {

    private val TIME_OUT = ViewConfiguration.getDoubleTapTimeout() + 100
    private var firstDownTime: Long = 0
    private var separateTouches = false
    private var twoFingerTapCount: Byte = 0

    fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN ->
                if (firstDownTime == 0L || event.eventTime - firstDownTime > TIME_OUT)
                    reset(event.downTime)
            MotionEvent.ACTION_POINTER_UP ->
                if (event.pointerCount == 2) twoFingerTapCount++ else firstDownTime = 0
            MotionEvent.ACTION_UP -> if (!separateTouches) separateTouches =
                true else if (twoFingerTapCount.toInt() == 2
                && event.eventTime - firstDownTime < TIME_OUT
            ) {
                onTwoFingerDoubleTap()
                firstDownTime = 0
                return true
            }
        }
        return false
    }

    abstract fun onTwoFingerDoubleTap()

    private fun reset(time: Long) {
        firstDownTime = time
        separateTouches = false
        twoFingerTapCount = 0
    }


}