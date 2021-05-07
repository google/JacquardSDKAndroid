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
package com.google.android.jacquard.sdk.model;

import com.google.auto.value.AutoValue;

/** Data class for reporting pairing process to the UI. */
@AutoValue
public abstract class Progress {

  public static Progress of(int totalSteps) {
    return new AutoValue_Progress(0, totalSteps);
  }

  /** The current step. */
  public abstract int currentStep();

  /** The total number of steps. */
  public abstract int totalStep();

  /** Increments the current steps and returns a new instance. */
  public Progress increment() {
    return new AutoValue_Progress(currentStep() + 1, totalStep());
  }
}
