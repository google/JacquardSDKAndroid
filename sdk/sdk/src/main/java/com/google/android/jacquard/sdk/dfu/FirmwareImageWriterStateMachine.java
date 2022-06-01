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

import static com.google.android.jacquard.sdk.dfu.FirmwareImageWriterState.Type.CHECKING_STATUS;
import static com.google.android.jacquard.sdk.dfu.FirmwareImageWriterState.Type.IDLE;
import static com.google.android.jacquard.sdk.dfu.FirmwareImageWriterState.Type.WRITING;

import android.bluetooth.BluetoothGatt;
import com.google.android.jacquard.sdk.StateMachine;
import com.google.android.jacquard.sdk.dfu.FirmwareImageWriterEvent.ParamsFirmwareImageTransfer;
import com.google.android.jacquard.sdk.dfu.model.TransferState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.ObservesNext;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.atap.jacquard.protocol.JacquardProtocol.DFUStatusResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.DFUWriteResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

final class FirmwareImageWriterStateMachine implements
    StateMachine<FirmwareImageWriterState, Void> {

  private static final String TAG = FirmwareImageWriterStateMachine.class.getSimpleName();

  private static final int DFU_BLOCK_SIZE = 128;
  private final Signal<FirmwareImageWriterState> stateSignal = Signal.create();
  private final String vid;
  private final String pid;
  private final int componentId;
  private FirmwareImageWriterState state = FirmwareImageWriterState.ofIdle();
  private byte[] firmwareByteArray;

  FirmwareImageWriterStateMachine(String vid, String pid, int componentId) {
    this.vid = vid;
    this.pid = pid;
    this.componentId = componentId;
    stateSignal.next(state);
  }

  /**
   * Upload firmware binaries to remote device.
   *
   * @param tag - current ConnectedJacquardTag object.
   * @param inputStream - InputStream of firmware binary file with need to upload.
   */
  public void uploadBinary(ConnectedJacquardTag tag, InputStream inputStream) {
    PrintLogger.d(TAG, "applyFirmware");
    firmwareByteArray = getByteArray(inputStream);
    if (firmwareByteArray.length == 0) {
      handleEvent(
          FirmwareImageWriterEvent.ofReceivedResponseWithError(new FileNotFoundException()));
      return;
    }
    handleEvent(FirmwareImageWriterEvent.ofStartCheckDfuStatus(tag));
  }

  @Override
  public Signal<FirmwareImageWriterState> getState() {
    return stateSignal;
  }

  @Override
  public void onStateEvent(Void state) {
    //Empty
  }

  @Override
  public void destroy() {
    handleEvent(FirmwareImageWriterEvent
        .ofReceivedResponseWithError(new Exception("FirmwareImageWriter destroy called.")));
  }

  public void cancel() {
    handleEvent(FirmwareImageWriterEvent.ofStopWriting());
  }

  /**
   * Called when an event is received.
   * @param event the {@link FirmwareImageWriterEvent} that will trigger a state change.
   */
  private void handleEvent(FirmwareImageWriterEvent event) {
    PrintLogger.d(TAG, "handleEvent : " + event.getType().name());
    if (state.isTerminal()) {
      PrintLogger.d(TAG, "State machine is already terminal, ignoring event");
      return;
    }
    switch (event.getType()) {
      case START_CHECK_DFU_STATUS:
        onCheckingStatus(event.startCheckDfuStatus());
        break;
      case START_PREPARING_FOR_WRITE:
        onPreparingForWrite(event.startPreparingForWrite());
        break;
      case START_WRITING:
        onStartWriting(event);
        break;
      case STOP_WRITING:
        onStopped();
        break;
      case COMPLETE:
        onComplete(event.complete());
        break;
      case RECEIVED_RESPONSE_WITH_ERROR:
        onReceivedResponseWithError(event);
        break;
    }
  }

  /** Checking firmware status by sending the DfuCheckStatus request. */
  private void onCheckingStatus(ConnectedJacquardTag tag) {
    PrintLogger.d(TAG, "onCheckingStatus");
    if (!this.state.isType(IDLE)) {
      PrintLogger.d(TAG, "onCheckingStatus ignore state: " + state);
      return;
    }
    updateState(FirmwareImageWriterState.ofCheckingStatus());
    sendDfuStatus(tag);
  }

  /** Preparing tag for firmware writing by sending the DfuPrepare request. */
  private void onPreparingForWrite(ConnectedJacquardTag tag) {
    PrintLogger.d(TAG, "onPreparingForWrite");
    if (!this.state.isType(CHECKING_STATUS)) {
      PrintLogger.d(TAG, "onPreparingForWrite ignore state: " + state);
      return;
    }
    updateState(FirmwareImageWriterState.ofPreparingForWrite());
    tag.enqueue(new DfuPrepareCommand(vid, pid, componentId, firmwareByteArray),/* retries= */0)
        .observe(
            new ObservesNext<Response>() {
              @Override
              public void onNext(Response response) {
                tag.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                handleEvent(FirmwareImageWriterEvent.ofStartWriting(ParamsFirmwareImageTransfer
                    .of(TransferState.create(/* offset= */0, firmwareByteArray.length), tag)));
              }

              @Override
              public void onError(Throwable t) {
                handleEvent(FirmwareImageWriterEvent.ofReceivedResponseWithError(t));
              }
            });
  }

  /** Writing firmware binary by sending the DfuWrite request. */
  private void onStartWriting(FirmwareImageWriterEvent event) {
    if (state.isType(FirmwareImageWriterState.Type.CANCEL)) {
      PrintLogger.d(TAG, "Writing process stopped.");
      return;
    }
    ParamsFirmwareImageTransfer params = event.startWriting();
    TransferState transferState = params.transferState();
    updateState(FirmwareImageWriterState.ofWriting(transferState));
    if (transferState.done()) {
      handleEvent(FirmwareImageWriterEvent.ofComplete(params.tag()));
    } else {
      writeFirmware(transferState.offset(), params.tag());
    }
  }

  /** Called when firmware binary write completed. */
  private void onComplete(ConnectedJacquardTag tag) {
    PrintLogger.d(TAG, "onComplete");
    tag.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
    if (!this.state.isType(CHECKING_STATUS) && !this.state.isType(WRITING)) {
      PrintLogger.d(TAG, "onComplete ignore state: " + state);
      return;
    }
    updateState(FirmwareImageWriterState.ofComplete());
  }

  private void onStopped() {
    PrintLogger.d(TAG, "onStopped");
    updateState(FirmwareImageWriterState.ofCancel());
  }

  /** Called when response with an error has been received. */
  private void onReceivedResponseWithError(FirmwareImageWriterEvent event) {
    PrintLogger.d(TAG, "onReceivedResponseWithError: " + event);
    updateState(FirmwareImageWriterState.ofError(event.receivedResponseWithError()));
  }

  /**
   * Called to update the state.
   * <p>
   * The state is emitted to observers of the state machine.
   *
   * @param state the new state of this state machine.
   */
  private void updateState(FirmwareImageWriterState state) {
    PrintLogger.d(TAG, "updateState: " + state);
    this.state = state;
    stateSignal.next(state);
  }

  private void writeFirmware(int offset, ConnectedJacquardTag tag) {
    int blockSize = Math.min(DFU_BLOCK_SIZE, firmwareByteArray.length - offset);
    tag.enqueue(new DfuWriteCommand(componentId, offset, firmwareByteArray), /* retries= */0)
        .observe(
            new ObservesNext<DFUWriteResponse>() {
              @Override
              public void onNext(DFUWriteResponse dfuWriteResponse) {
                processDfuWriteResponse(dfuWriteResponse.getCrc(),
                    dfuWriteResponse.hasOffset() ? dfuWriteResponse.getOffset()
                        : offset + blockSize,
                    tag);
              }

              @Override
              public void onError(Throwable t) {
                handleEvent(FirmwareImageWriterEvent.ofReceivedResponseWithError(t));
              }
            });
  }

  private void sendDfuStatus(ConnectedJacquardTag tag) {
    tag.enqueue(new DfuStatusCommand(vid, pid, componentId),/* retries= */0).observe(
        new ObservesNext<DFUStatusResponse>() {
          @Override
          public void onNext(DFUStatusResponse dfuStatusResponse) {
            processDfuStatusResponse(dfuStatusResponse, tag);
          }

          @Override
          public void onError(Throwable t) {
            handleEvent(FirmwareImageWriterEvent.ofReceivedResponseWithError(t));
          }
        });
  }

  private void processDfuStatusResponse(DFUStatusResponse status, ConnectedJacquardTag tag) {
    // 2^32 possible crc if evenly distributed, low chance of collisions when sizes
    // are equal, and not done. 2^16 if done, collisions are somewhat possible when
    // sizes are equal
    int currentSize = status.getCurrentSize();
    int currentCrc = status.getCurrentCrc();
    int finalSize = status.getFinalSize();
    int finalCrc = status.getFinalCrc();
    if (currentSize <= firmwareByteArray.length
        && currentCrc == DfuUtil.crc16(firmwareByteArray, currentSize)
        && finalCrc == DfuUtil.crc16(firmwareByteArray, firmwareByteArray.length)
        && finalSize == firmwareByteArray.length) {
      int report = currentSize * 100 / firmwareByteArray.length;
      PrintLogger.i(TAG, "Resume existing DFU transfer at " + report);
      if (finalSize != currentSize) {
        tag.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
      }
      handleEvent(FirmwareImageWriterEvent.ofStartWriting(
          ParamsFirmwareImageTransfer.of(TransferState.create(currentSize, finalSize), tag)));
      return;
    }
    PrintLogger.i(TAG, "Preparing new DFU");
    handleEvent(FirmwareImageWriterEvent.ofStartPreparingForWrite(tag));
  }

  private void processDfuWriteResponse(int crc, int offset, ConnectedJacquardTag tag) {
    int crc1 = DfuUtil.crc16(firmwareByteArray, offset);
    int report = offset * 100 / firmwareByteArray.length;
    PrintLogger.i(TAG, "Sending firmware " + report + "%");
    if (crc != crc1) {
      handleEvent(FirmwareImageWriterEvent.ofReceivedResponseWithError(new IllegalStateException(
          String.format("CRC failure 0x%04x != 0x%04x at %d", crc, crc1, offset))));
    }
    handleEvent(FirmwareImageWriterEvent.ofStartWriting(ParamsFirmwareImageTransfer
        .of(TransferState.create(offset, firmwareByteArray.length), tag)));
  }

  private byte[] getByteArray(InputStream inputStream) {
    try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
        InputStream binary = new BufferedInputStream(inputStream)) {
      int read;
      byte[] buf = new byte[32768];
      while ((read = binary.read(buf, 0, 32768)) != -1) {
        bout.write(buf, 0, read);
      }
      return bout.toByteArray();
    } catch (IOException ioException) {
      PrintLogger.e(TAG, ioException.getMessage());
    }
    return new byte[0];
  }
}
