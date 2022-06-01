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

/** Provides an interface for playing notes.  */
interface SoundPlayer {
    /** Plays the note.  */
    fun noteOn(note: Note, @FloatRange(from = 0.0, to = 1.0) volume: Float)

    /** Releases the note.  */
    fun noteOff(note: Note)

    /** Releases all allocated resources.  */
    fun destroy()

    /** Ids for notes. Currently 12 notes are supported.  */
    enum class Note {
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

        companion object {
            private val NOTES = values()

            /** Return the [Note] for line. Will cap at length of notes.  */
            @JvmStatic
            fun getNode(line: Int): Note {
                return NOTES[Math.min(line, NOTES.size - 1)]
            }
        }
    }
}