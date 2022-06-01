/*
 * Copyright 2021 Google LLC
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

package com.google.android.jacquard.sdk.dfu.model;

import com.google.auto.value.AutoValue;

/** Binary upload/download operation status */
@AutoValue
public abstract class TransferState {

  /** Current transferred offset value in bytes. */
  public abstract int offset();

  /** Total download size in bytes. */
  public abstract int total();

  /** Returns if download is complete. */
  public abstract boolean done();

  public static TransferState create(int progress, int total) {
    return new AutoValue_TransferState(progress, total, total != 0 && progress == total);
  }
}

