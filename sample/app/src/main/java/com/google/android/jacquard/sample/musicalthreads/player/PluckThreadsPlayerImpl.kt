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

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.google.android.jacquard.sample.ResourceLocator
import com.google.android.jacquard.sample.musicalthreads.audio.Fader
import com.google.android.jacquard.sample.musicalthreads.audio.SoundPlayer
import com.google.android.jacquard.sample.musicalthreads.audio.SoundPlayer.Note.Companion.getNode
import com.google.android.jacquard.sample.musicalthreads.audio.SoundPoolPlayer
import com.google.android.jacquard.sample.musicalthreads.player.Constants.VELOCITY_THRESHOLD
import com.google.android.jacquard.sample.musicalthreads.player.Constants.getVolume

/**
 * Implementation of [ThreadsPlayer] that converts interposer touch output to sound when a
 * finger is lifted from a line.
 */
class PluckThreadsPlayerImpl(private val context: Context) : ThreadsPlayer {
    private var soundPlayer: SoundPoolPlayer? = null
    private val downLines: MutableMap<Int, Int> = mutableMapOf() // lineIndex, velocity.

    companion object {
        private val TAG = PluckThreadsPlayerImpl::class.java.simpleName
        private const val NOTE_FADE_DURATION_IN_MILLIS: Long = 500
    }

    override fun init() {
        val soundPool = getSoundPool()
        val fader = Fader(soundPool, NOTE_FADE_DURATION_IN_MILLIS)
        soundPlayer = SoundPoolPlayer(context, soundPool, fader)
    }

    private fun getSoundPool(): SoundPool {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
        return SoundPool.Builder()
            .setMaxStreams(SoundPlayer.Note.values().size)
            .setAudioAttributes(audioAttributes)
            .build()
    }

    override fun destroy() {
        soundPlayer?.destroy()
        soundPlayer = null
    }

    override fun play(lines: List<Int>) {
        for ((index, velocity) in lines.withIndex()) {
            playLine(index, velocity)
        }
    }

    private fun playLine(line: Int, velocity: Int) {
        if (!downLines.containsKey(line)) { /*The line is pressed down.*/
            onLineDown(line, velocity)
        } else if (velocity == 0) { /*Finger has been lifted.*/
            downLines[line]?.let { onLineUp(line, it) }
        }
    }

    // The line it currently down.
    private fun onLineDown(line: Int, velocity: Int) {
        if (velocity < VELOCITY_THRESHOLD) {
            return
        }
        // Finger is down - mute the line if it is playing.
        soundPlayer?.let {
            it.noteOff(getNode(line))
            // Mark the note as being pressed down.
            downLines[line] = velocity
        } ?: Log.d(TAG, "SoundPoolPlayer not initialized")
    }

    // Finger is lifted - note is on
    private fun onLineUp(line: Int, velocity: Int) {
        soundPlayer?.let {
            it.noteOn(getNode(line), getVolume(velocity))
            downLines.remove(line)
        } ?: Log.d(TAG, "SoundPoolPlayer not initialized")
    }
}