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
package com.google.android.jacquard.sdk.dfu;

import androidx.annotation.NonNull;
import com.google.auto.value.AutoOneOf;

/** The states of {@link FirmwareUpdateStateMachine}. */
@AutoOneOf(FirmwareUpdateState.Type.class)
public abstract class FirmwareUpdateState {

  public static FirmwareUpdateState ofIdle() {
    return AutoOneOf_FirmwareUpdateState.idle();
  }

  public static FirmwareUpdateState ofPreparingToTransfer() {
    return AutoOneOf_FirmwareUpdateState.preparingToTransfer();
  }

  public static FirmwareUpdateState ofTransferProgress(int percentage) {
    return AutoOneOf_FirmwareUpdateState.transferProgress(percentage);
  }

  public static FirmwareUpdateState ofTransferred() {
    return AutoOneOf_FirmwareUpdateState.transferred();
  }

  public static FirmwareUpdateState ofExecuting() {
    return AutoOneOf_FirmwareUpdateState.executing();
  }

  public static FirmwareUpdateState ofCompleted() {
    return AutoOneOf_FirmwareUpdateState.completed();
  }

  public static FirmwareUpdateState ofStopped() {
    return AutoOneOf_FirmwareUpdateState.stopped();
  }

  public static FirmwareUpdateState ofError(Throwable error) {
    return AutoOneOf_FirmwareUpdateState.error(error);
  }

  @NonNull
  public abstract Type getType();

  public abstract void idle();

  public abstract void preparingToTransfer();

  public abstract int transferProgress();

  public abstract void transferred();

  public abstract void executing();

  public abstract void completed();

  public abstract void stopped();

  public abstract Throwable error();

  public enum Type {
    IDLE,
    PREPARING_TO_TRANSFER,
    TRANSFER_PROGRESS,
    TRANSFERRED,
    EXECUTING,
    COMPLETED,
    STOPPED,
    ERROR
  }
}
