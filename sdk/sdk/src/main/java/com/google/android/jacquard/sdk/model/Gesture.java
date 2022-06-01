/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk.model;

import com.google.atap.jacquard.protocol.JacquardProtocol.InferenceData;
import com.google.auto.value.AutoValue;

/** Class describing the different gestures supported by Jacquard. */
@AutoValue
public abstract class Gesture {

  public abstract GestureType gestureType();

  public static Gesture of(InferenceData inferenceData) {
    return new AutoValue_Gesture(GestureType.from(inferenceData.getEvent()));
  }

  public enum GestureType {
    NO_INFERENCE(0, "No Gesture"),
    DOUBLE_TAP(1, "Double Tap"),
    BRUSH_IN(2, "Brush In"),
    BRUSH_OUT(3, "Brush Out"),
    SHORT_COVER(7, "Cover");

    private final int id;
    private final String description;

    GestureType(int id, String description) {
      this.id = id;
      this.description = description;
    }

    /** Creates a GestureType from from an identifier. */
    public static GestureType from(int id) {
      for (GestureType type : GestureType.values()) {
        if (type.id == id) {
          return type;
        }
      }
      return GestureType.NO_INFERENCE;
    }

    /** Identifier for the gesture as reported by the tag. */
    public int getId() {
      return id;
    }

    /** A short description of the gesture. */
    public String getDescription() {
      return description;
    }
  }
}
