/*
 * Copyright 2021 Google LLC
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

package com.google.android.jacquard.sdk.dfu;

import com.google.android.jacquard.sdk.dfu.model.TransferState;
import com.google.auto.value.AutoOneOf;

/** The states of {@link FirmwareImageWriterStateMachine}. */
@AutoOneOf(FirmwareImageWriterState.Type.class)
abstract class FirmwareImageWriterState {

  enum Type {
    IDLE,
    CHECKING_STATUS,
    PREPARING_FOR_WRITE,
    WRITING,
    COMPLETE,
    ERROR
  }

  static FirmwareImageWriterState ofIdle() {
    return AutoOneOf_FirmwareImageWriterState.idle();
  }

  static FirmwareImageWriterState ofCheckingStatus() {
    return AutoOneOf_FirmwareImageWriterState.checkingStatus();
  }

  static FirmwareImageWriterState ofPreparingForWrite() {
    return AutoOneOf_FirmwareImageWriterState.preparingForWrite();
  }

  static FirmwareImageWriterState ofWriting(TransferState state) {
    return AutoOneOf_FirmwareImageWriterState.writing(state);
  }

  static FirmwareImageWriterState ofComplete() {
    return AutoOneOf_FirmwareImageWriterState.complete();
  }

  static FirmwareImageWriterState ofError(Throwable throwable) {
    return AutoOneOf_FirmwareImageWriterState.error(throwable);
  }

  boolean isTerminal() {
    return getType() == Type.COMPLETE || getType() == Type.ERROR;
  }

  boolean isType(Type type) {
    return getType() == type;
  }

  abstract Type getType();

  abstract void idle();

  abstract void checkingStatus();

  abstract void preparingForWrite();

  abstract TransferState writing();

  abstract void complete();

  abstract Throwable error();
}
