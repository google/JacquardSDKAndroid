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
package com.google.android.jacquard.sample.musicalthreads.audio

import androidx.annotation.FloatRange

/** Data class for holding active playing streams.  */
class Stream private constructor(
    val streamId: Int, @FloatRange(from = 0.0, to = 1.0) val volume: Float
) {
    companion object {
        /**
         * Returns a new Stream object.
         *
         * @param streamId the streamId returned by [android.media.SoundPool.play]
         * @param volume the volume of the playback.
         */
        @JvmStatic
        fun of(streamId: Int, @FloatRange(from = 0.0, to = 1.0) volume: Float): Stream {
            return Stream(streamId, volume)
        }
    }
}