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

package com.google.android.jacquard.sample.musicalthreads.player;

import static com.google.android.jacquard.sample.musicalthreads.player.Constants.VELOCITY_THRESHOLD;
import static com.google.android.jacquard.sample.musicalthreads.player.Constants.getVolume;

import com.google.android.jacquard.sample.musicalthreads.audio.SoundPlayer;
import com.google.android.jacquard.sample.musicalthreads.audio.SoundPlayer.Note;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link ThreadsPlayer} that converts interposer touch output to sound when a
 * finger is lifted from a line.
 */
public class PluckTreadsPlayerImpl implements ThreadsPlayer {

  private final Map<Integer, Integer> downLines = new HashMap<>(); // lineIndex, velocity.
  private final SoundPlayer soundPlayer;

  public PluckTreadsPlayerImpl(SoundPlayer soundPlayer) {
    this.soundPlayer = soundPlayer;
  }

  @Override
  public void destroy() {
    soundPlayer.destroy();
  }

  @Override
  public void play(List<Integer> lines) {
    for (int i = 0; i < lines.size(); i++) {
      int velocity = lines.get(i);
      playLine(i, velocity);
    }
  }

  private void playLine(int line, int velocity) {
    if (downLines.containsKey(line)) { // The line is pressed down.
      if (velocity == 0) { // Finger has been lifted.
        onLineUp(line, downLines.get(line));
      }
    } else {
      onLineDown(line, velocity);
    }
  }

  // The line it currently down.
  private void onLineDown(int line, int velocity) {
    if (velocity < VELOCITY_THRESHOLD) {
      return;
    }
    // Finger is down - mute the line if it is playing.
    Note note = Note.getNode(line);
    soundPlayer.noteOff(note);
    // Mark the note as being pressed down.
    downLines.put(line, velocity);
  }

  // Finger is lifted - note is on
  private void onLineUp(int line, int velocity) {
    Note note = Note.getNode(line);
    float volume = getVolume(velocity);
    soundPlayer.noteOn(note, volume);
    downLines.remove(line);
  }
}
