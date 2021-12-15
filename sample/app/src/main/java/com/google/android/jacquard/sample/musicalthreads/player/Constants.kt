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
package com.google.android.jacquard.sample.musicalthreads.player

/** Holds constant values used by implementations of [ThreadsPlayer].  */
object Constants {
  /** A line's velocity is between 0 and 127.  */
  private const val MAX_VELOCITY = 127f

  /** The minimum velocity required for a note to be audible.  */
  const val VELOCITY_THRESHOLD = 15
  @JvmStatic
  fun getVolume(velocity: Int): Float {
    return velocity / MAX_VELOCITY
  }
}