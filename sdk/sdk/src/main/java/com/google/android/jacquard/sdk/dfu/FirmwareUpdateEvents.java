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
package com.google.android.jacquard.sdk.dfu;

import androidx.annotation.Nullable;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;
import java.util.List;

@AutoOneOf(FirmwareUpdateEvents.Type.class)
abstract class FirmwareUpdateEvents {

  static FirmwareUpdateEvents ofIdle() {
    return AutoOneOf_FirmwareUpdateEvents.idle();
  }

  static FirmwareUpdateEvents ofPrepareToTransfer(ParamsPrepareToTransfer params) {
    return AutoOneOf_FirmwareUpdateEvents.prepareToTransfer(params);
  }

  static FirmwareUpdateEvents ofBeginTransfer(ParamsPrepareToTransfer params) {
    return AutoOneOf_FirmwareUpdateEvents.beginTransfer(params);
  }

  static FirmwareUpdateEvents ofTransferring(int percentage) {
    return AutoOneOf_FirmwareUpdateEvents.transferring(percentage);
  }

  static FirmwareUpdateEvents ofTransferred() {
    return AutoOneOf_FirmwareUpdateEvents.transferred();
  }

  static FirmwareUpdateEvents ofExecuting(ParamsPrepareToTransfer params) {
    return AutoOneOf_FirmwareUpdateEvents.executing(params);
  }

  static FirmwareUpdateEvents ofCompleted() {
    return AutoOneOf_FirmwareUpdateEvents.completed();
  }

  static FirmwareUpdateEvents ofError(Throwable error) {
    return AutoOneOf_FirmwareUpdateEvents.error(error);
  }

  abstract FirmwareUpdateEvents.Type getType();

  abstract void idle();

  abstract ParamsPrepareToTransfer prepareToTransfer();

  abstract ParamsPrepareToTransfer beginTransfer();

  abstract int transferring();

  abstract void transferred();

  abstract ParamsPrepareToTransfer executing();

  abstract void completed();

  abstract Throwable error();

  enum Type {
    IDLE,
    PREPARE_TO_TRANSFER,
    BEGIN_TRANSFER,
    TRANSFERRING,
    TRANSFERRED,
    EXECUTING,
    COMPLETED,
    ERROR
  }

  @AutoValue
  static abstract class ParamsPrepareToTransfer {

    static ParamsPrepareToTransfer of(DFUChecker dfuChecker, List<DFUInfo> dfuInfos,
        long totalTransferSize, ConnectedJacquardTag tag) {
      return new AutoValue_FirmwareUpdateEvents_ParamsPrepareToTransfer(dfuChecker, dfuInfos,
          totalTransferSize, tag);
    }

    abstract DFUChecker dfuChecker();

    @Nullable
    abstract List<DFUInfo> listDfuInfo();

    abstract long totalTransferSize();

    abstract ConnectedJacquardTag tag();
  }
}
