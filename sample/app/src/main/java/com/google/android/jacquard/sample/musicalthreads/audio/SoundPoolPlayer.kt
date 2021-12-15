/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sample.musicalthreads.audio

import android.content.Context
import android.media.SoundPool
import androidx.annotation.FloatRange
import androidx.annotation.RawRes
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.musicalthreads.audio.SoundPlayer.Note
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.common.collect.ImmutableMap
import java.util.EnumMap

class SoundPoolPlayer(
  applicationContext: Context,
  private val soundPool: SoundPool,
  private val fader: Fader
) : SoundPlayer {

  companion object {
    private val TAG: String = SoundPoolPlayer::class.java.simpleName
  }

  private val noteSoundIdMap = lazy { createNoteSoundIdMap(applicationContext) }
  private val streams: EnumMap<Note, Stream> = EnumMap(Note::class.java)

  override fun noteOn(note: Note, volume: Float) {
    val soundId = noteSoundIdMap.value[note]
    soundId?.let {
      PrintLogger.d(
        TAG,
        "noteOn $note volume: $volume"
      )
      streams[note] = playNote(it, volume)
    } ?: PrintLogger.e(TAG, "No sound found for note: $note")
  }

  override fun noteOff(note: Note) {
    streams[note]?.let {
      PrintLogger.d(TAG, "noteOff: $note")
      fader.fadeOut(it)
      streams.remove(note)
    } ?: PrintLogger.d(
      TAG,
      "No sound is playing for note $note"
    )
  }

  override fun destroy() {
    soundPool.release()
    streams.clear()
  }

  private fun createNoteSoundIdMap(context: Context): ImmutableMap<Note, Int> {
    return ImmutableMap.builder<Note, Int>()
      .put(Note.NOTE_1, loadNote(context, R.raw.g2_1))
      .put(Note.NOTE_2, loadNote(context, R.raw.g3_2))
      .put(Note.NOTE_3, loadNote(context, R.raw.b2_3))
      .put(Note.NOTE_4, loadNote(context, R.raw.b3_4))
      .put(Note.NOTE_5, loadNote(context, R.raw.d3_5))
      .put(Note.NOTE_6, loadNote(context, R.raw.d4_6))
      .put(Note.NOTE_7, loadNote(context, R.raw.g3_7))
      .put(Note.NOTE_8, loadNote(context, R.raw.g4_8))
      .put(Note.NOTE_9, loadNote(context, R.raw.d4_9))
      .put(Note.NOTE_10, loadNote(context, R.raw.d4_10))
      .put(Note.NOTE_11, loadNote(context, R.raw.g4_11))
      .put(Note.NOTE_12, loadNote(context, R.raw.g4_12)).build()
  }

  private fun loadNote(context: Context, @RawRes soundResId: Int): Int {
    return soundPool.load(context, soundResId, /* priority=*/  1)
  }

  private fun playNote(soundId: Int, @FloatRange(from = 0.0, to = 1.0) volume: Float): Stream {
    return Stream.of(
      soundPool.play(
        soundId, /*leftVolume=*/ volume,
        /*rightVolume=*/ volume,
        /*priority=*/ 0,
        /*loop=*/ 0,
        /*rate=*/ 1f
      ), volume
    )
  }
}