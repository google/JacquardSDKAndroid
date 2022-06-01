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

import com.google.auto.value.AutoValue;

/** Error reported by {@link com.google.android.jacquard.sdk.BleAdapter} when connection failed. */
@AutoValue
public abstract class FailedToConnect {

  /** Creates a new instance of FailedToConnect. */
  public static FailedToConnect of(Peripheral peripheral, JacquardError error) {
    return new AutoValue_FailedToConnect(peripheral, error);
  }

  /** The peripheral the connected failed to. */
  public abstract Peripheral peripheral();

  /** The error reported by {@link com.google.android.jacquard.sdk.BleAdapter}. */
  public abstract JacquardError error();
}
