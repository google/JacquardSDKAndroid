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

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class JacquardRelativeLayout extends RelativeLayout {

  private final TwoFingerDoubleTapDetector twoFingerDoubleTapDetector =
      new TwoFingerDoubleTapDetector() {
        @Override
        public void onTwoFingerDoubleTap() {
          tapListener.onTwoFingerDoubleTap();
        }
      };
  private OnDoubleListener tapListener;

  public JacquardRelativeLayout(Context context) {
    super(context);
  }

  public JacquardRelativeLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public JacquardRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public JacquardRelativeLayout(
      Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    return this.twoFingerDoubleTapDetector.onInterceptTouchEvent(ev);
  }

  public void setDoubleTouchListener(OnDoubleListener tapListener) {
    this.tapListener = tapListener;
  }

  public interface OnDoubleListener {
    void onTwoFingerDoubleTap();
  }
}
