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

import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.CONNECTED;
import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.FIRMWARE_TRANSFER_COMPLETE;
import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.FIRMWARE_UPDATE_INITIATED;
import static com.google.android.jacquard.sdk.dfu.FirmwareUpdateState.Type;

import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.android.jacquard.sdk.JacquardManager;
import com.google.android.jacquard.sdk.command.UnloadModuleCommand;
import com.google.android.jacquard.sdk.dfu.DFUChecker.CheckUpdateParams;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.JacquardError;
import com.google.android.jacquard.sdk.model.Module;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/** Concrete implementation of {@link DfuManager}. */
public class DfuManagerImpl implements DfuManager {

  private static final String TAG = DfuManagerImpl.class.getSimpleName();
  private final String identifier;
  private final FirmwareUpdateStateMachine fwUpdateStateMachine;
  private final DFUChecker dfuChecker;
  private final Signal<FirmwareUpdateState> stateSignal;
  // Holding the Tag for BadFirmware use case and should be used for this purpose only.
  private final ConnectedJacquardTag tagForBadFirmware;

  public DfuManagerImpl(ConnectedJacquardTag tag) {
    this.identifier = tag.address();
    this.tagForBadFirmware = tag;
    fwUpdateStateMachine = new FirmwareUpdateStateMachine();
    dfuChecker = new DFUChecker();
    stateSignal = Signal.<FirmwareUpdateState>create();
  }

  @VisibleForTesting
  DfuManagerImpl(ConnectedJacquardTag tag, DFUChecker dfuChecker, FirmwareUpdateStateMachine fwUpdateStateMachine,
      Signal<FirmwareUpdateState> signal) {
    this.identifier = tag.address();
    this.tagForBadFirmware = tag;
    this.fwUpdateStateMachine = fwUpdateStateMachine;
    this.dfuChecker = dfuChecker;
    this.stateSignal = signal;
  }

  @Override
  public Signal<FirmwareUpdateState> applyUpdates(List<DFUInfo> dfuInfos, boolean autoExecute) {
    PrintLogger.d(TAG, "applyUpdates size: " + dfuInfos.size());
    return apply(dfuInfos, autoExecute);
  }

  @Override
  public Signal<FirmwareUpdateState> applyModuleUpdate(List<DFUInfo> dfuInfo) {
    PrintLogger.d(TAG, "applyModuleUpdate");
    return apply(dfuInfo, /* autoExecute= */ false);
  }

  @Override
  public Signal<List<DFUInfo>> checkFirmware(
          List<Component> components, @Nullable String vid, @Nullable String pid,
          boolean forceUpdate) {
    PrintLogger.d(TAG, "checkFirmware");
    if (components.isEmpty()) {
      return Signal.empty(new IllegalArgumentException("Provided component list is empty."));
    }
    return getConnectedJacquardTag()
        .flatMap(
            connectedTag -> {
              List<CheckUpdateParams> checkUpdateParamsList = new ArrayList<>();
              String tagVersion = connectedTag.tagComponent().version().toZeroString();
              for (Component component : components) {
                CheckUpdateParams.Builder builder = CheckUpdateParams.builder();
                // New cloud implementation, for GMR TAG firmware check it require the GMR VID and
                // PID as input in cloud api.
                // If we have vid/pid details, we will need to overwrite tag vid/pid.
                if (component.componentId() == Component.TAG_ID
                    && !TextUtils.isEmpty(vid)
                    && !TextUtils.isEmpty(pid)) {
                  builder.vendorId(vid);
                  builder.productId(pid);
                } else {
                  builder.vendorId(component.vendor().id());
                  builder.productId(component.product().id());
                }
                builder.componentSerialNumber(component.serialNumber());
                builder.componentVersion(component.version().toZeroString());
                builder.tagVersion(tagVersion);
                checkUpdateParamsList.add(builder.build());
              }
              return dfuChecker.checkUpdate(checkUpdateParamsList, forceUpdate);
            })
        .map(
            dfuInfoList -> {
              // If we have vid/pid details, then replacing back to tag vid/pid.
              if (!TextUtils.isEmpty(vid) && !TextUtils.isEmpty(pid)) {
                for (int i = 0; i < dfuInfoList.size(); i++) {
                  DFUInfo info = dfuInfoList.get(i);
                  if (info.productId().equals(pid) && info.vendorId().equals(vid)) {
                    Component tagComponent = getTagComponent(components);
                    dfuInfoList.set(
                        i,
                        DFUInfo.create(
                            info.version().toZeroString(),
                            info.dfuStatus().name(),
                            info.downloadUrl(),
                            tagComponent.vendor().id(),
                            tagComponent.product().id(),
                            info.moduleId(),
                            info.downloadFilePath()));
                    break;
                  }
                }
              }
              return dfuInfoList;
            });
  }

  @Override
  public Signal<List<DFUInfo>> checkFirmware(List<Component> components, boolean forceUpdate) {
    return checkFirmware(components, null, null, forceUpdate);
  }

  @Override
  public Signal<List<DFUInfo>> checkModuleUpdate(List<Module> modules, boolean forceUpdate) {
    PrintLogger.d(TAG, "checkModuleUpdate");
    if (modules.isEmpty()) {
      return Signal.just(Collections.emptyList());
    }
    return getConnectedJacquardTag()
        .flatMap(
            connectedTag -> {
              List<CheckUpdateParams> checkUpdateParamsList = new ArrayList<>();
              Component tagComponent = connectedTag.tagComponent();
              for (Module module : modules) {
                CheckUpdateParams.Builder builder = CheckUpdateParams.builder();
                builder.vendorId(module.vendorId());
                builder.productId(module.productId());
                builder.componentSerialNumber(module.moduleId());
                builder.componentVersion(module.version().toZeroString());
                builder.moduleId(module.moduleId());
                builder.tagVersion(tagComponent.version().toZeroString());
                checkUpdateParamsList.add(builder.build());
              }
              return dfuChecker.checkUpdate(checkUpdateParamsList, /* forceUpdate= */ forceUpdate);
            });
  }

  @Override
  public Signal<List<DFUInfo>> checkModuleUpdate(boolean forceUpdate) {
    return getConnectedJacquardTag()
        .flatMap(ConnectedJacquardTag::getRemoteModules)
        .flatMap(modules -> checkModuleUpdate(modules, forceUpdate));
  }

  @Override
  public void executeUpdates() {
    PrintLogger.d(TAG, "executeFirmware");
    fwUpdateStateMachine.getState().first().flatMap(state -> {
      if (!state.getType().equals(Type.TRANSFERRED)) {
        return Signal.empty(
            new IllegalStateException(
                "FirmwareUpdateStateMachine is not in Transferred state, state: " + state));
      }
      return executeFirmware();
    }).tapError(error -> stateSignal.next(FirmwareUpdateState.ofError(error)))
        .tap(stateSignal::next).consume();
  }

  @Override
  public void stop() {
    PrintLogger.d(TAG, "stop");
    fwUpdateStateMachine.stop();
  }

  @Override
  public Signal<FirmwareUpdateState> getCurrentState() {
    return stateSignal.distinctUntilChanged();
  }

  private Component getTagComponent(List<Component> components) {
    for (Component component : components){
      if (component.componentId() == Component.TAG_ID){
        return component;
      }
    }
    return null;
  }

  private Signal<FirmwareUpdateState> apply(List<DFUInfo> dfuInfos, boolean autoExecute) {
    PrintLogger.d(TAG, "calling applyUpdates");
    return Signal.create(
        localSignal -> {
          PrintLogger.d(TAG, "applyFirmware");
          boolean[] isOnlyModule = {false};
          getConnectedJacquardTag()
              .flatMap(ConnectedJacquardTag::getRemoteModules)
              .flatMap(this::deactivateModule)
              .flatMap(ignores -> fwUpdateStateMachine.getState())
              .flatMap(
                  stateCheck -> {
                    if (stateCheck.getType().equals(Type.PREPARING_TO_TRANSFER)
                        || stateCheck.getType().equals(Type.TRANSFER_PROGRESS)
                        || stateCheck.getType().equals(Type.EXECUTING)) {
                      return Signal.empty(
                          new IllegalStateException(
                              "FirmwareUpdateStateMachine is in" + stateCheck + "  state."));
                    }
                    return fwUpdateStateMachine.getState();
                  })
              .tap(state -> PrintLogger.d(TAG, "State: " + state.getType()))
              .flatMap(
                  ignore -> {
                    if (dfuInfos == null || dfuInfos.isEmpty()) {
                      return Signal.empty(new IllegalStateException("DfuInfo list is empty."));
                    }
                    PrintLogger.d(TAG, "dfu list size: " + dfuInfos.size());
                    isOnlyModule[0] = checkIfOnlyModule(dfuInfos);
                    return getConnectedJacquardTag()
                        .flatMap(
                            connectedTag -> {
                              fwUpdateStateMachine.applyFirmware(
                                  dfuChecker, dfuInfos, connectedTag);
                              return fwUpdateStateMachine.getState();
                            });
                  })
              .tapError(
                  error -> {
                    stateSignal.next(FirmwareUpdateState.ofError(error));
                    localSignal.next(FirmwareUpdateState.ofError(error));
                  })
              .onNext(
                  state -> {
                    PrintLogger.d(TAG, "applyFirmware State: " + state);
                    localSignal.next(state);
                    updateState(state, autoExecute, isOnlyModule[0]);
                  });

          return new Subscription();
        });
  }

  private Signal<Boolean> deactivateModule(List<Module> modules) {
    for (Module module : modules) {
      if (module.isEnabled()) {
        PrintLogger.d(TAG, "active module found, deactivating!");
        return getConnectedJacquardTag()
            .flatMap(tag -> tag.enqueue(new UnloadModuleCommand(module)));
      }
    }
    return Signal.from(true);
  }

  private boolean checkIfOnlyModule(List<DFUInfo> dfuInfos) {
    for (DFUInfo dfuInfo : dfuInfos) {
      if (dfuInfo.moduleId() == null
          && !dfuInfo.dfuStatus().equals(DFUInfo.UpgradeStatus.NOT_AVAILABLE)) {
        PrintLogger.d(TAG, "Non module dfu found.");
        return false;
      }
    }
    PrintLogger.d(TAG, "Only module dfu present.");
    return true;
  }

  private Signal<FirmwareUpdateState> executeFirmware() {
    return Signal.create(signal -> {
      Subscription subscription = getConnectedJacquardTag()
          .tapError(signal::error)
          .onNext(connectedTag -> {
            fwUpdateStateMachine.executeFirmware(dfuChecker, connectedTag);
            fwUpdateStateMachine.getState().tapError(signal::error)
                .onNext(firmwareUpdateState -> {
                  signal.next(firmwareUpdateState);
                  if (firmwareUpdateState.getType().equals(Type.COMPLETED)) {
                    signal.complete();
                  }
                });
          });
      return new Subscription() {
        @Override
        protected void onUnsubscribe() {
          fwUpdateStateMachine.destroy();
          subscription.unsubscribe();
          signal.complete();
        }
      };
    });
  }

  private void updateState(FirmwareUpdateState state, boolean autoExecute, boolean isOnlyModule) {
    if (state.getType().equals(Type.TRANSFERRED) && isOnlyModule) {
      // completed is internally called via state machine in this case.
      PrintLogger.d(TAG, "only module updated.");
      return;
    }
    stateSignal.next(state);
    if (state.getType().equals(Type.TRANSFERRED)) {
      PrintLogger.d(TAG, "TRANSFERRED State Trigger: " + autoExecute);
      if (!autoExecute) {
        return;
      }
      executeFirmware().tapError(error -> stateSignal.next(FirmwareUpdateState.ofError(error)))
          .onNext(stateSignal::next);
    }
  }

  private Signal<ConnectedJacquardTag> getConnectedJacquardTag() {
    return JacquardManager.getInstance()
        .getConnectionStateSignal(identifier)
        .first()
        .flatMap(
            connectionState -> {
              if (connectionState.isType(FIRMWARE_UPDATE_INITIATED)
                  || connectionState.isType(FIRMWARE_TRANSFER_COMPLETE)) {
                // We need Tag in methods checkFirmware, applyUpdate, executeFirmware.
                // Current state would be FIRMWARE_UPDATE_INITIATED while executing checkFirmware,
                // applyUpdate and FIRMWARE_TRANSFER_COMPLETE while executing executeFirmware.
                // Needed to avoid exposing the Tag in any of the Firmware related states, to achieve
                // that we had to hold the Tag so that we can return it from her.
                return Signal.from(tagForBadFirmware);
              }

              if (connectionState.isType(CONNECTED)) {
                return Signal.from(connectionState.connected());
              }
              return Signal.empty(
                  JacquardError.ofBluetoothConnectionError(new IllegalStateException("Device is not connected, address: " + identifier)));
            });
  }
}