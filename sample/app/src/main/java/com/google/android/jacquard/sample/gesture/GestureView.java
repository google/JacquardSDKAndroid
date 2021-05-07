/*
 * Copyright 2021 Google LLC. All Rights Reserved.
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

package com.google.android.jacquard.sample.gesture;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.gesture.GestureViewModel.GestureViewItem;

/** Overlay for showing the performed gesture. */
public class GestureView extends RelativeLayout {

  private static final int ANIMATION_DURATION_IN_MS = 1000;

  private ImageView gestureImageView;
  private TextView gestureTextView;

  public GestureView(Context context) {
    super(context);
  }

  public GestureView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public GestureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    setBackgroundResource(R.color.gesture_overlay_color);
    inflate(getContext(), R.layout.gesture_overlay, this);
    gestureImageView = findViewById(R.id.gesture_overlay_imageview);
    gestureTextView = findViewById(R.id.gesture_overlay_textview);
  }

  /** Animated in/out this view while showing the provided gesture. */
  public void show(GestureViewItem item) {
    gestureImageView.setImageResource(item.drawableResId());
    gestureTextView.setText(item.gesture().gestureType().getDescription());
    ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
    animator.setDuration(ANIMATION_DURATION_IN_MS);
    animator.setInterpolator(new AccelerateInterpolator());
    animator.setRepeatCount(1);
    animator.setRepeatMode(ValueAnimator.REVERSE);
    animator.addUpdateListener(
        animation -> {
          float alpha = (float) animation.getAnimatedValue();
          setAlpha(alpha);
        });

    animator.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            setVisibility(View.GONE);
          }

          @Override
          public void onAnimationStart(Animator animation) {
            setVisibility(View.VISIBLE);
          }
        });

    animator.start();
  }
}
