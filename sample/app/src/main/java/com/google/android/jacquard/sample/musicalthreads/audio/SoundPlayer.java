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

import static java.lang.Math.min;

import androidx.annotation.FloatRange;

/** Provides an interface for playing notes. */
public interface SoundPlayer {

  /** Plays the note. */
  void noteOn(Note note, @FloatRange(from = 0.0, to = 1.0) float volume);

  /** Releases the note. */
  void noteOff(Note note);

  /** Releases all allocated resources. */
  void destroy();

  /** Ids for notes. Currently 12 notes are supported. */
  enum Note {
    NOTE_1,
    NOTE_2,
    NOTE_3,
    NOTE_4,
    NOTE_5,
    NOTE_6,
    NOTE_7,
    NOTE_8,
    NOTE_9,
    NOTE_10,
    NOTE_11,
    NOTE_12;

    private static final Note[] NOTES = Note.values();

    /** Return the {@link Note} for line. Will cap at length of notes. */
    public static Note getNode(int line) {
      int index = min(line, NOTES.length - 1);
      return NOTES[index];
    }
  }
}
