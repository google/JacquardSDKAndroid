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

import android.content.Context;
import android.media.SoundPool;
import androidx.annotation.FloatRange;
import androidx.annotation.RawRes;
import com.google.android.jacquard.sample.R;
import com.google.common.collect.ImmutableMap;
import java.util.EnumMap;
import java.util.Map;
import timber.log.Timber;

/** Implementation of {@link SoundPlayer} that uses a {@link SoundPool}. */
public class SoundPoolPlayer implements SoundPlayer {

  private final SoundPool soundPool;
  private final Fader fader;
  private final ImmutableMap<Note, Integer> noteSoundIdMap; // Note, SoundId
  private final Map<Note, Stream> streams = new EnumMap<>(Note.class);

  public SoundPoolPlayer(Context applicationContext, SoundPool soundPool, Fader fader) {
    this.soundPool = soundPool;
    this.fader = fader;
    noteSoundIdMap = createNoteSoundIdMap(applicationContext);
  }

  @Override
  public void noteOn(Note note, float volume) {
    Integer soundId = noteSoundIdMap.get(note);
    if (soundId == null) {
      Timber.e("No sound found for note %s", note);
      return;
    }
    Timber.d("noteOn " + note + " volume: " + volume);
    streams.put(note, playNote(soundId, volume));
  }

  @Override
  public void noteOff(Note note) {
    Stream stream = streams.get(note);
    if (stream == null) {
      Timber.d("No sound is playing for note %s", note);
      return;
    }
    Timber.d("noteOff %s", note);
    fader.fadeOut(stream);
    streams.remove(note);
  }

  @Override
  public void destroy() {
    soundPool.release();
    streams.clear();
  }

  /** Plays the sounds associated with soundId. */
  private Stream playNote(int soundId, @FloatRange(from = 0.0, to = 1.0) float volume) {
    int steamId =
        soundPool.play(
            soundId,
            /*leftVolume=*/ volume,
            /*rightVolume=*/ volume,
            /*priority=*/ 0,
            /*loop=*/ 0,
            /*rate=*/ 1f);
    return Stream.of(steamId, volume);
  }

  /** Loads all notes into the {@link SoundPool} and return a map of notes and soundIds. */
  private ImmutableMap<Note, Integer> createNoteSoundIdMap(Context context) {
    return ImmutableMap.<Note, Integer>builder()
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
        .put(Note.NOTE_12, loadNote(context, R.raw.g4_12))
        .build();
  }

  /** Loads the provided soundsResId and returns a note Id. */
  private int loadNote(Context context, @RawRes int soundResId) {
    return soundPool.load(context, soundResId, /* priority=*/ 1);
  }
}
