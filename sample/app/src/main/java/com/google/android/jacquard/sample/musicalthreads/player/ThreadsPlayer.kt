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
package com.google.android.jacquard.sample.musicalthreads.player

/** Interface for providing player implementations.  */
interface ThreadsPlayer {

    /** Initialize all allocated resources.  */
    fun init()

    /** Releases all allocated resources.  */
    fun destroy()

    /**
     * Plays based on a thread output. Each entry in the array is a thread. The value is the velocity.
     * This function will be called by the interposer multiple times per second.
     *
     * @param lines touch lines from the interposer.
     */
    fun play(lines: List<Int>)
}