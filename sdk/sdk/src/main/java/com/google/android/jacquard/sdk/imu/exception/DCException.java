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

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatus;

/**
 * Exception occurred during IMU Sample collection.
 */
public class DCException extends IllegalStateException {

  private DataCollectionStatus dataCollectionStatus;

  public DCException(String message) {
    super(message);
    dataCollectionStatus = DataCollectionStatus.DATA_COLLECTION_UNKNOWN;
  }

  public DCException(@NonNull DataCollectionStatus status) {
    super(status.name());
    this.dataCollectionStatus = status;
  }

  public DataCollectionStatus getDataCollectionStatus() {
    return dataCollectionStatus;
  }
}
