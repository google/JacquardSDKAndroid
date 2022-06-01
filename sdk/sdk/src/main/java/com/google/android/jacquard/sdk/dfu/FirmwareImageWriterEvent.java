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

package com.google.android.jacquard.sdk.dfu;

import com.google.android.jacquard.sdk.dfu.model.TransferState;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;

/** The types of events that {@link FirmwareImageWriterStateMachine} reacts to. */
@AutoOneOf(FirmwareImageWriterEvent.Type.class)
abstract class FirmwareImageWriterEvent {

  public enum Type {
    START_CHECK_DFU_STATUS,
    RECEIVED_RESPONSE_WITH_ERROR,
    START_PREPARING_FOR_WRITE,
    START_WRITING,
    COMPLETE,
    STOP_WRITING
  }

  abstract Type getType();

  abstract ConnectedJacquardTag startCheckDfuStatus();

  abstract Throwable receivedResponseWithError();

  abstract ConnectedJacquardTag startPreparingForWrite();

  abstract ParamsFirmwareImageTransfer startWriting();

  abstract ConnectedJacquardTag complete();

  abstract void stopWriting();


  static FirmwareImageWriterEvent ofComplete(ConnectedJacquardTag tag) {
    return AutoOneOf_FirmwareImageWriterEvent.complete(tag);
  }

  static FirmwareImageWriterEvent ofStartCheckDfuStatus(ConnectedJacquardTag tag) {
    return AutoOneOf_FirmwareImageWriterEvent.startCheckDfuStatus(tag);
  }

  static FirmwareImageWriterEvent ofReceivedResponseWithError(Throwable throwable) {
    return AutoOneOf_FirmwareImageWriterEvent.receivedResponseWithError(throwable);
  }

  static FirmwareImageWriterEvent ofStartPreparingForWrite(ConnectedJacquardTag tag) {
    return AutoOneOf_FirmwareImageWriterEvent.startPreparingForWrite(tag);
  }

  static FirmwareImageWriterEvent ofStartWriting(ParamsFirmwareImageTransfer params) {
    return AutoOneOf_FirmwareImageWriterEvent.startWriting(params);
  }

  static FirmwareImageWriterEvent ofStopWriting() {
    return AutoOneOf_FirmwareImageWriterEvent.stopWriting();
  }

  @AutoValue
  static abstract class ParamsFirmwareImageTransfer {

    static ParamsFirmwareImageTransfer of(TransferState transferState, ConnectedJacquardTag tag) {
      return new AutoValue_FirmwareImageWriterEvent_ParamsFirmwareImageTransfer(transferState, tag);
    }

    abstract TransferState transferState();

    abstract ConnectedJacquardTag tag();
  }
}
