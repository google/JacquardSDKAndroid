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
package com.google.android.jacquard.sample.musicalthreads.audio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.media.SoundPool
import android.view.animation.DecelerateInterpolator

/** Helper class for fading sounds.  */
class Fader(private val soundPool: SoundPool, private val fadeDurationInMillis: Long) {
  /** Fades out the provided stream.  */
  fun fadeOut(stream: Stream) {
    startFadeOut(stream)
  }

  private fun startFadeOut(stream: Stream) {
    ValueAnimator.ofFloat(stream.volume, 0f).apply {
      duration = fadeDurationInMillis
      interpolator = DecelerateInterpolator()
      addUpdateListener { valueAnimator: ValueAnimator ->
        val volume = valueAnimator.animatedValue as Float
        soundPool.setVolume(stream.streamId, volume, volume)
      }
      addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
          soundPool.stop(stream.streamId)
        }
      })
    }.start()
  }
}