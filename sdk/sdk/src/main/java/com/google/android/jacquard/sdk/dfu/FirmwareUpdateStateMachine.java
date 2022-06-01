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

import static com.google.android.jacquard.sdk.dfu.FirmwareUpdateState.Type.EXECUTING;
import static com.google.android.jacquard.sdk.dfu.FirmwareUpdateState.Type.TRANSFERRED;

import android.bluetooth.BluetoothGatt;
import android.text.TextUtils;
import com.google.android.jacquard.sdk.JacquardManager;
import com.google.android.jacquard.sdk.StateMachine;
import com.google.android.jacquard.sdk.command.BatteryStatus;
import com.google.android.jacquard.sdk.command.BatteryStatusCommand;
import com.google.android.jacquard.sdk.command.DfuExecuteUpdateNotificationSubscription;
import com.google.android.jacquard.sdk.connection.ConnectionState;
import com.google.android.jacquard.sdk.dfu.DFUInfo.UpgradeStatus;
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateEvents.ParamsPrepareToTransfer;
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState.Type;
import com.google.android.jacquard.sdk.dfu.execption.FirmwareUpdateStoppedException;
import com.google.android.jacquard.sdk.dfu.execption.InsufficientBatteryException;
import com.google.android.jacquard.sdk.dfu.execption.UpdateMismatchException;
import com.google.android.jacquard.sdk.dfu.execption.UpdatedFirmwareNotFoundException;
import com.google.android.jacquard.sdk.dfu.model.FileDescriptor;
import com.google.android.jacquard.sdk.dfu.model.PlatformSettings;
import com.google.android.jacquard.sdk.dfu.model.TransferState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

/** State machine for {@link DfuManager} which manages current state {@link FirmwareUpdateState}. */
final class FirmwareUpdateStateMachine implements StateMachine<FirmwareUpdateState, Void> {

  private static final String TAG = FirmwareUpdateStateMachine.class.getSimpleName();
  private static final PlatformSettings SETTINGS = new PlatformSettings();
  private final Signal<FirmwareUpdateState> stateSignal = Signal.<FirmwareUpdateState>create()
      .sticky();
  private final List<DFUInfo> readyToExecuteDfuList = new ArrayList<>();
  private FirmwareUpdateState state = FirmwareUpdateState.ofIdle();
  private Subscription applyFirmwareSubscription, executeFirmwareSubscription;
  private FirmwareImageWriterStateMachine writerStateMachine;

  FirmwareUpdateStateMachine() {
    updateState(FirmwareUpdateState.ofIdle());
  }

  @Override
  public Signal<FirmwareUpdateState> getState() {
    return stateSignal.distinctUntilChanged();
  }

  @Override
  public void onStateEvent(Void unused) {
    // empty
  }

  @Override
  public void destroy() {
    PrintLogger.d(TAG, "destroy");
    if (applyFirmwareSubscription != null) {
      applyFirmwareSubscription.unsubscribe();
    }
    if (executeFirmwareSubscription != null) {
      executeFirmwareSubscription.unsubscribe();
    }
  }

  /**
   * Initiate the process to apply and upload the firmware.
   *
   * @param dfuChecker instance of {@link DFUChecker}.
   * @param dfuInfos instance of {@link DFUInfo}.
   * @param connectedTag instance of {@link ConnectedJacquardTag}.
   */
  public void applyFirmware(DFUChecker dfuChecker, List<DFUInfo> dfuInfos,
      ConnectedJacquardTag connectedTag) {
    reset();
    handleEvent(FirmwareUpdateEvents.ofPrepareToTransfer(ParamsPrepareToTransfer
        .of(dfuChecker, dfuInfos, /* totalTransferSize= */0L, connectedTag)));
  }

  /**
   * Initiates the process of executing firmware.
   *
   * @param dfuChecker instance of {@link DFUChecker}.
   * @param connectedTag instance of {@link ConnectedJacquardTag}.
   */
  public void executeFirmware(DFUChecker dfuChecker, ConnectedJacquardTag connectedTag) {
    handleEvent(FirmwareUpdateEvents
        .ofExecuting(ParamsPrepareToTransfer
            .of(dfuChecker, /* dfuInfos= */null, /* totalTransferSize= */0, connectedTag)));
  }

  /** Stops the dfu process. If the dfu process is going on and also reset the dfu process. */
  public void stop() {
    PrintLogger
        .d(TAG, "App has requested to stop current DFU. Current Firmware update state # " + state);
    if (state.getType().equals(TRANSFERRED) || state.getType().equals(EXECUTING)) {
      handleEvent(
          FirmwareUpdateEvents.ofError(
              new FirmwareUpdateStoppedException(
                  "Cancelling ble transfer, app has requested to stop/interrupt DFU.")));
      reset();
    } else {
      handleEvent(FirmwareUpdateEvents.ofStopped());
    }
    destroy();
  }

  /** Reset the State machine back to Idle. */
  private void reset() {
    PrintLogger.d(TAG, "reset");
    readyToExecuteDfuList.clear();
    updateState(FirmwareUpdateState.ofIdle());
  }

  private void handleEvent(FirmwareUpdateEvents event) {
    PrintLogger.d(TAG, "handleEvent: " + event.getType().name() + " currentState: " + state);
    switch (event.getType()) {
      case PREPARE_TO_TRANSFER:
        ParamsPrepareToTransfer params = event.prepareToTransfer();
        prepareToTransfer(params.dfuChecker(), params.listDfuInfo(), params.tag());
        break;
      case BEGIN_TRANSFER:
        ParamsPrepareToTransfer paramsBeginTransfer = event.beginTransfer();
        beginTransfer(paramsBeginTransfer.dfuChecker(), paramsBeginTransfer.listDfuInfo(),
            paramsBeginTransfer.totalTransferSize(), paramsBeginTransfer.tag());
        break;
      case TRANSFERRING:
        transferring(event.transferring());
        break;
      case TRANSFERRED:
        transferred();
        break;
      case EXECUTING:
        executing(event.executing());
        break;
      case COMPLETED:
        completed();
        break;
      case STOPPED:
        stopped();
        break;
      case ERROR:
        error(event.error());
        break;
    }
  }

  private void beginTransfer(DFUChecker dfuChecker, List<DFUInfo> dfuInfos, long totalTransferSize,
      ConnectedJacquardTag tag) {
    if (!state.getType().equals(Type.PREPARING_TO_TRANSFER)) {
      PrintLogger.d(TAG, "beginTransfer ignore state: " + state);
      return;
    }

    applyFirmwareSubscription = Signal.<Integer>create(signalParent -> {
      applyFirmware(tag, dfuChecker,
          dfuInfos, /* position= */  0,
          totalTransferSize,
          /* currentTransferSize= */ 0,
          signalParent).onError(error -> handleEvent(FirmwareUpdateEvents.ofError(error)));
      return new Subscription() {
        @Override
        protected void onUnsubscribe() {
          signalParent.complete();
        }
      };
    }).onNext(progressValue -> {
      handleEvent(FirmwareUpdateEvents.ofTransferring(progressValue));
      if (progressValue == 100) {
        handleEvent(FirmwareUpdateEvents.ofTransferred());
        // readyToExecuteDfuList did not contain loadable module DFUInfo object.
        // If user try to upload only loadable module then readyToExecuteDfuList will be empty.
        if (readyToExecuteDfuList.isEmpty()) {
          // If did not have any DFUInfo object to execute then sending completed event.
          handleEvent(FirmwareUpdateEvents.ofCompleted());
        }
      }
    });
  }

  private void transferring(int percentage) {
    if (!state.getType().equals(Type.PREPARING_TO_TRANSFER)
        && !state.getType().equals(Type.TRANSFER_PROGRESS)) {
      PrintLogger.d(TAG, "transferring ignore state: " + state);
      return;
    }
    updateState(FirmwareUpdateState.ofTransferProgress(percentage));
  }

  private void prepareToTransfer(DFUChecker dfuChecker, List<DFUInfo> dfuInfos,
      ConnectedJacquardTag tag) {
    if (!state.getType().equals(Type.IDLE)) {
      PrintLogger.d(TAG, "prepareToTransfer ignore state: " + state);
      return;
    }
    updateState(FirmwareUpdateState.ofPreparingToTransfer());

    readyToExecuteDfuList.clear();
    ImmutableSet<DFUInfo> updatedDfuInfoList = checkForUpdates(tag, dfuInfos);
    PrintLogger.d(TAG, "updatedDfuInfoList size = " + updatedDfuInfoList.size());
    if (updatedDfuInfoList.isEmpty()) {
      handleEvent(FirmwareUpdateEvents
          .ofError(
              new UpdatedFirmwareNotFoundException("Does not have any update.")));
      return;
    }
    PrintLogger.d(TAG, "updatedDfuInfoList: " + updatedDfuInfoList);
    tag.enqueue(new BatteryStatusCommand()).flatMap(batteryStatus -> {
      if (!isSufficientBattery(batteryStatus)) {
        return Signal.empty(new InsufficientBatteryException(
            batteryStatus.batteryLevel(), SETTINGS.getMinimumBatteryDFU()));
      }
      return Signal.just(batteryStatus);
    }).flatMap(ignore -> {
      long totalTransferSize = calculateTotalTransferSize(dfuChecker, updatedDfuInfoList);
      PrintLogger.d(TAG, /* message= */"totalTransferSize = " + totalTransferSize);
      if (totalTransferSize == 0) {
        return Signal.empty(new FileNotFoundException("Did not find the firmware files."));
      }
      handleEvent(FirmwareUpdateEvents.ofBeginTransfer(
          ParamsPrepareToTransfer.of(dfuChecker, new ArrayList<>(updatedDfuInfoList), totalTransferSize, tag)));
      return Signal.just(ignore);
    }).onError(error -> handleEvent(FirmwareUpdateEvents.ofError(error)));
  }

  private void transferred() {
    if (!state.getType().equals(Type.TRANSFER_PROGRESS)) {
      PrintLogger.d(TAG, "transferred ignore state: " + state);
      return;
    }
    updateState(FirmwareUpdateState.ofTransferred());
  }

  private void executing(ParamsPrepareToTransfer params) {
    if (!state.getType().equals(TRANSFERRED)) {
      PrintLogger.d(TAG, "executing ignore state: " + state);
      return;
    }
    updateState(FirmwareUpdateState.ofExecuting());
    execute(params.dfuChecker(), params.tag());
  }

  private void completed() {
    // In cases of only loadable transfer state can be TRANSFERRED.
    if (!state.getType().equals(Type.EXECUTING) && !state.getType().equals(TRANSFERRED)) {
      PrintLogger.d(TAG, "completed ignore state: " + state);
      return;
    }
    updateState(FirmwareUpdateState.ofCompleted());
    // When Firmware update is completed, we reset the State-Machine.
    reset();
  }

  private void stopped() {
    if (writerStateMachine != null) {
      writerStateMachine.cancel();
    }
    updateState(FirmwareUpdateState.ofStopped());
  }

  private void error(Throwable error) {
    PrintLogger.e(TAG, "FirmwareUpdateStateMachine error: ", error);
    updateState(FirmwareUpdateState.ofError(error));
  }

  private void updateState(FirmwareUpdateState state) {
    this.state = state;
    stateSignal.next(state);
  }

  private ImmutableSet<DFUInfo> checkForUpdates(ConnectedJacquardTag tag, List<DFUInfo> dfuInfos) {
    ImmutableSet.Builder<DFUInfo> dfuInfoList = new ImmutableSet.Builder<>();
    List<Component> components = tag.getComponents();
    // Moving the gear component on top, so gear firmware will be executed first.
    Collections.sort(components, (o1, o2) -> o2.componentId() - o1.componentId());
    for (Component component : components) {
      for (DFUInfo dfuInfo : dfuInfos) {
        if (dfuInfo.dfuStatus().equals(UpgradeStatus.NOT_AVAILABLE)) {
          PrintLogger.d(TAG, "Found dfu which is not available.");
          continue;
        }
        if (dfuInfo.isApplicableTo(component.vendor().id(), component.product().id()) && !dfuInfo
            .version().equals(component.version())) {
          PrintLogger.d(TAG, "adding dfu into list");
          dfuInfoList.add(dfuInfo);
        } else if (dfuInfo.moduleId() != null) {
          PrintLogger.d(TAG, "adding module dfu into list");
          dfuInfoList.add(dfuInfo);
        }
      }
    }
    return dfuInfoList.build();
  }

  private boolean isSufficientBattery(BatteryStatus batteryStatus) {
    return batteryStatus.batteryLevel() >= SETTINGS.getMinimumBatteryDFU();
  }

  private long calculateTotalTransferSize(DFUChecker dfuChecker, ImmutableSet<DFUInfo> dfuInfos) {
    long totalSize = 0;
    for (DFUInfo dfuInfo : dfuInfos) {
      Optional<FileDescriptor> optional = dfuChecker.getFirmwareStream(dfuInfo);
      if (!optional.isPresent()) {
        continue;
      }
      totalSize += optional.get().totalSize();
    }
    return totalSize;
  }

  private Signal<TransferState> applyFirmware(ConnectedJacquardTag tag, DFUChecker dfuChecker,
      List<DFUInfo> dfuInfos, int indexPos, long totalTransferSize, long previousTransferSize,
      Signal<Integer> signalParent) {
    return uploadBinary(tag, dfuChecker, dfuInfos.get(indexPos))
        .tapError(error -> {
          if (JacquardProtocol.Status.ERROR_UNKNOWN.name().equalsIgnoreCase(error.getMessage())) {
            PrintLogger.e(TAG, /* message= */
                "Corrupt firmware usecase: " + error + " Deleting the firmware file.");
            DFUInfo dfuInfo = dfuInfos.get(indexPos);
            Component component = getComponent(tag, dfuInfo);
          }
        }).flatMap(transferState -> {
          long transferFileSize = previousTransferSize + transferState.offset();
          int percentage = (int) (transferFileSize * 100 / totalTransferSize);
          PrintLogger.d(TAG, "transferState : " + transferState + " percentage : " + percentage);
          if (percentage == 100) {
            // Remove the loadable module from dfuInfos list before adding to them on readyToExecuteDfuList.
            // As loadable module did not required to execute command to install.
            readyToExecuteDfuList.addAll(removeLoadableModule(dfuChecker, dfuInfos));
            signalParent.next(/* percentage= */ 100);
            signalParent.complete();
            return Signal.from(transferState);
          } else {
            signalParent.next(percentage);
          }
          if (transferState.done()) {
            return applyFirmware(tag, dfuChecker, dfuInfos, indexPos + 1,
                totalTransferSize, transferFileSize, signalParent);
          }
          return Signal.from(transferState);
        });
  }

  private List<DFUInfo> removeLoadableModule(DFUChecker dfuChecker, List<DFUInfo> dfuInfos) {
    List<DFUInfo> dfuInfoList = new ArrayList<>(dfuInfos);
    Iterator<DFUInfo> dfuInfoIterator = dfuInfoList.listIterator();
    while (dfuInfoIterator.hasNext()) {
      DFUInfo dfuInfo = dfuInfoIterator.next();
      if (!TextUtils.isEmpty(dfuInfo.moduleId())) {
        PrintLogger.d(TAG, "Removed loadable module dfuInfo object.");
        dfuInfoIterator.remove();
      }
    }
    return dfuInfoList;
  }

  private Signal<TransferState> uploadBinary(ConnectedJacquardTag tag, DFUChecker dfuChecker,
      DFUInfo dfuInfo) {
    return Signal.create(signal -> {
      Optional<FileDescriptor> descriptorOptional = dfuChecker.getFirmwareStream(dfuInfo);
      if (!descriptorOptional.isPresent()) {
        signal.error(new FileNotFoundException("Firmware file does not exist in cache."));
        return new Subscription();
      }
      if (state.getType() == Type.STOPPED) {
        signal.error(new IllegalStateException("Firmware Update Stopped."));
        return new Subscription();
      }
      int componentId = Component.TAG_ID;
      if (TextUtils.isEmpty(dfuInfo.moduleId())) {
        Component component = getComponent(tag, dfuInfo);
        if (component == null) {
          PrintLogger.d(TAG, "Component is null, dfuInfo = " + dfuInfo);
          signal.error(new IllegalStateException(
              "Device Firmware Update does not match with any of the connected component."));
          return new Subscription();
        }
        componentId = component.componentId();
      }
      writerStateMachine = new FirmwareImageWriterStateMachine(
          dfuInfo.vendorId(), dfuInfo.productId(), componentId);
      writerStateMachine.getState()
          .onNext(state -> {
            switch (state.getType()) {
              case WRITING:
                signal.next(state.writing());
                break;
              case CANCEL:
                PrintLogger.d(TAG, "App has requested to stop/interrupt DFU.");
                tag.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
                readyToExecuteDfuList.clear();
                break;
              case ERROR:
                tag.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
                signal.error(state.error());
                break;
            }
          });
      writerStateMachine.uploadBinary(tag, descriptorOptional.get().inputStream());
      return new Subscription(){
        @Override
        protected void onUnsubscribe() {
          tag.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
          writerStateMachine.destroy();
        }
      };
    });
  }

  @Nullable
  private Component getComponent(ConnectedJacquardTag tag, DFUInfo dfuInfo) {
    for (Component component : tag.getComponents()) {
      if (dfuInfo.isApplicableTo(component.vendor().id(), component.product().id())) {
        return component;
      }
    }
    return null;
  }

  private void execute(DFUChecker dfuChecker, ConnectedJacquardTag tag) {
    PrintLogger
        .d(TAG, " executeFirmware dfuInfoUploadedList size = " + readyToExecuteDfuList.size());
    if (readyToExecuteDfuList.isEmpty()) {
      handleEvent(FirmwareUpdateEvents
          .ofError(new UpdatedFirmwareNotFoundException("No update pending for execution.")));
      return;
    }
    executeFirmwareSubscription =
        tag.enqueue(new BatteryStatusCommand()).flatMap(batteryStatus -> {
          if (!isSufficientBattery(batteryStatus)) {
            return Signal.empty(new InsufficientBatteryException(
                batteryStatus.batteryLevel(), SETTINGS.getMinimumBatteryDFU()));
          }
          return Signal.from(batteryStatus);
            }).flatMap(ignore -> {
              JacquardManager.getInstance().getMemoryCache().putDeviceInfo(tag.address(), null);
              return recursiveExecuteFirmware(tag, dfuChecker);
            }
        ).tapError(value -> handleEvent(FirmwareUpdateEvents.ofError(value)))
            .onNext(ignore -> handleEvent(FirmwareUpdateEvents.ofCompleted()));
  }

  private Signal<Boolean> recursiveExecuteFirmware(ConnectedJacquardTag tag, DFUChecker dfuChecker) {
    int indexPos = 0;
    return executeFirmware(readyToExecuteDfuList.get(indexPos), tag, dfuChecker)
        .flatMap(ignore -> {
          readyToExecuteDfuList.remove(indexPos);
          if (readyToExecuteDfuList.isEmpty()) {
            PrintLogger.d(TAG, "ExecuteFirmware completed");
            return Signal.from(ignore);
          }
          return recursiveExecuteFirmware(tag, dfuChecker);
        });
  }

  private Signal<Boolean> executeFirmware(DFUInfo dfuInfo, ConnectedJacquardTag tag, DFUChecker dfuChecker) {
    PrintLogger.d(TAG, "executeFirmware dfuInfo : " + dfuInfo);
    Component component = getComponent(tag, dfuInfo);
    if (component == null) {
      return Signal.empty(
          new UpdateMismatchException(
              "DfuInfo does not match with the ConnectedJacquardTAG, dfuInfo : " + dfuInfo));
    }
    return tag.enqueue(
        new DfuExecuteCommand(component.vendor().id(), component.product().id(),
            component.componentId())).flatMap(ignore -> {
      PrintLogger.d(TAG,
          "DfuExecuteCommand response : " + ignore + " componentId : " + component
              .componentId());
      if (component.componentId() == Component.TAG_ID) {
        // ConnectionStateSignal is sticky signal and it send last connectionState value.
        // Before rebooting process start we are observing the signal and getting connected state,
        // so added drop to ignore the first connectionState from this signal.
        return JacquardManager.getInstance().getConnectionStateSignal(tag.address())
            .filter(connectionState -> connectionState.isType(
                ConnectionState.Type.CONNECTED)).drop(1).first().map(ignoreState -> {
              PrintLogger.d(TAG, "UJT firmware updated");
              return ignore;
            });
      } else {
        return Signal
            .merge(Signal.just(ignore).delay(/* millis= */60000), // fallback delay in millis.
                tag.subscribe(new DfuExecuteUpdateNotificationSubscription())
                    .map(notification -> {
                      PrintLogger.d(TAG, "DfuExecuteUpdateNotification : " + notification);
                      return ignore;
                    })).first();
      }
    });
  }
}
