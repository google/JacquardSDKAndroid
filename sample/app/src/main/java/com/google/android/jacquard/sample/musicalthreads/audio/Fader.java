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

package com.google.android.jacquard.sample.musicalthreads.audio;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.media.SoundPool;
import android.view.animation.DecelerateInterpolator;

/** Helper class for fading sounds. */
public class Fader {

  private final SoundPool soundPool;
  private final long fadeDurationInMillis;

  public Fader(SoundPool soundPool, long fadeDurationInMillis) {
    this.soundPool = soundPool;
    this.fadeDurationInMillis = fadeDurationInMillis;
  }

  /** Fades out the provide stream. */
  public void fadeOut(Stream stream) {
    startFadeOut(stream);
  }

  private void startFadeOut(Stream stream) {
    ValueAnimator valueAnimator = ValueAnimator.ofFloat(stream.volume, 0f);
    valueAnimator.setDuration(fadeDurationInMillis);
    valueAnimator.setInterpolator(new DecelerateInterpolator());
    valueAnimator.addUpdateListener(
        animation -> {
          float volume = (float) animation.getAnimatedValue();
          soundPool.setVolume(stream.streamId, volume, volume);
        });
    valueAnimator.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            soundPool.stop(stream.streamId);
          }
        });
    valueAnimator.start();
  }
}
