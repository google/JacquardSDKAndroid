/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.jacquard.sdk.imu.exception;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionMode;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatus;

/**
 * Exception thrown when tag is already collecting data and you tried to start again.
 */
public class InprogressDCException extends DCException {

  public InprogressDCException(
      @NonNull DataCollectionStatus status,
      @Nullable DataCollectionMode mode) {
    super(status, mode);
  }

  @Nullable
  @Override
  public String getMessage() {
    return "Data collection is already in progress. " + super.toString();
  }
}
