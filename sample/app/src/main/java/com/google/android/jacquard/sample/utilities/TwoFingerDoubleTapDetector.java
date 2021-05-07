/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sample.utilities;

import android.view.MotionEvent;
import android.view.ViewConfiguration;

/*
 * Class to detect Two Fingers double tap on a particular view.
 *  */
public abstract class TwoFingerDoubleTapDetector {
  private static final int TIMEOUT = ViewConfiguration.getDoubleTapTimeout() + 100;
  private long firstDownTime = 0;
  private boolean separateTouches = false;
  private byte twoFingerTapCount = 0;

  private void reset(long time) {
    firstDownTime = time;
    separateTouches = false;
    twoFingerTapCount = 0;
  }

  public boolean onInterceptTouchEvent(MotionEvent event) {
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        if (firstDownTime == 0 || event.getEventTime() - firstDownTime > TIMEOUT)
          reset(event.getDownTime());
        break;
      case MotionEvent.ACTION_POINTER_UP:
        if (event.getPointerCount() == 2)
          twoFingerTapCount++;
        else
          firstDownTime = 0;
        break;
      case MotionEvent.ACTION_UP:
        if (!separateTouches)
          separateTouches = true;
        else if (twoFingerTapCount == 2 && event.getEventTime() - firstDownTime < TIMEOUT) {
          onTwoFingerDoubleTap();
          firstDownTime = 0;
          return true;
        }
    }

    return false;
  }

  public abstract void onTwoFingerDoubleTap();
}