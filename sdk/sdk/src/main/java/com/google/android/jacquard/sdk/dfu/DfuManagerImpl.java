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

import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.CONNECTED;
import static com.google.android.jacquard.sdk.dfu.FirmwareUpdateState.Type;

import androidx.annotation.VisibleForTesting;
import com.google.android.jacquard.sdk.JacquardManager;
import com.google.android.jacquard.sdk.dfu.DFUChecker.CheckUpdateParams;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.Module;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;


/** Concrete implementation of {@link DfuManager}. */
public class DfuManagerImpl implements DfuManager {

  private static final String TAG = DfuManagerImpl.class.getSimpleName();
  private final String identifier;
  private final FirmwareUpdateStateMachine fwUpdateStateMachine;
  private final DFUChecker dfuChecker;

  public DfuManagerImpl(String identifier) {
    this.identifier = identifier;
    fwUpdateStateMachine = new FirmwareUpdateStateMachine();
    dfuChecker = new DFUChecker();
  }

  @VisibleForTesting
  DfuManagerImpl(String identifier, DFUChecker dfuChecker, FirmwareUpdateStateMachine fwUpdateStateMachine) {
    this.identifier = identifier;
    this.fwUpdateStateMachine = fwUpdateStateMachine;
    this.dfuChecker = dfuChecker;
  }

  @Override
  public Signal<FirmwareUpdateState> applyUpdates(List<DFUInfo> dfuInfos, boolean autoExecute) {
    PrintLogger.d(TAG, "applyFirmware");
    return applyUpdates(dfuInfos, autoExecute, /* isOnlyModule= */false);
  }

  @Override
  public Signal<FirmwareUpdateState> applyModuleUpdate(DFUInfo dfuInfo) {
    PrintLogger.d(TAG, "applyModuleUpdate");
    return applyUpdates(Lists.newArrayList(dfuInfo),/* autoExecute= */ false, /* isOnlyModule= */
        true);
  }

  @Override
  public Signal<List<DFUInfo>> checkFirmware(List<Component> components, boolean forceUpdate) {
    PrintLogger.d(TAG, "checkFirmware");
    if (components.isEmpty()) {
      return Signal.empty(new IllegalArgumentException("Provided component list is empty."));
    }
    return getConnectedJacquardTag().flatMap(connectedTag -> {
      List<CheckUpdateParams> checkUpdateParamsList = new ArrayList<>();
      String tagVersion = connectedTag.tagComponent().version().toZeroString();
      for (Component component : components) {
        CheckUpdateParams.Builder builder = CheckUpdateParams.builder();
        builder.vendorId(component.vendor().id());
        builder.productId(component.product().id());
        builder.componentSerialNumber(component.serialNumber());
        builder.componentVersion(component.version().toZeroString());
        builder.tagVersion(tagVersion);
        checkUpdateParamsList.add(builder.build());
      }
      return dfuChecker.checkUpdate(checkUpdateParamsList, forceUpdate);
    });
  }

  @Override
  public Signal<DFUInfo> checkModuleUpdate(Module module) {
    PrintLogger.d(TAG, "checkModuleUpdate");
    if (module == null) {
      return Signal.empty(new IllegalArgumentException("Provided Module is empty."));
    }
    return getConnectedJacquardTag().flatMap(connectedTag -> {
      Component tagComponent = connectedTag.tagComponent();
      CheckUpdateParams.Builder builder = CheckUpdateParams.builder();
      builder.vendorId(module.vendorId());
      builder.productId(module.productId());
      builder.componentSerialNumber(module.moduleId());
      builder.componentVersion(module.version().toZeroString());
      builder.moduleId(module.moduleId());
      builder.tagVersion(tagComponent.version().toZeroString());
      return dfuChecker.checkUpdate(builder.build(), /* forceUpdate= */false);
    });
  }

  @Override
  public Signal<FirmwareUpdateState> executeUpdates() {
    PrintLogger.d(TAG, "executeFirmware");
    return fwUpdateStateMachine.getState().first().flatMap(state -> {
      if (!state.getType().equals(Type.TRANSFERRED)) {
        return Signal.empty(
            new IllegalStateException(
                "FirmwareUpdateStateMachine is not in Transferred state, state: " + state));
      }
      return executeFirmware();
    });
  }

  @Override
  public void stop() {
    PrintLogger.d(TAG, "stop");
    fwUpdateStateMachine.stop();
  }

  private Signal<FirmwareUpdateState> applyUpdates(List<DFUInfo> dfuInfos, boolean autoExecute,
      boolean isOnlyModule) {
    PrintLogger.d(TAG, "applyFirmware");
    return Signal.create(signal -> {
      Subscription subscription = fwUpdateStateMachine.getState().first().flatMap(stateCheck -> {
        if (stateCheck.getType().equals(Type.PREPARING_TO_TRANSFER) || stateCheck.getType()
            .equals(Type.TRANSFER_PROGRESS) || stateCheck.getType().equals(Type.EXECUTING)) {
          return Signal.empty(new IllegalStateException(
              "FirmwareUpdateStateMachine is in" + stateCheck + "  state."));
        }
        return fwUpdateStateMachine.getState();
      }).flatMap(ignore -> {
        if (dfuInfos == null || dfuInfos.isEmpty()) {
          return Signal.empty(new IllegalStateException("DfuInfo list is empty."));
        }
        return getConnectedJacquardTag().flatMap(connectedTag -> {
          fwUpdateStateMachine.applyFirmware(dfuChecker, dfuInfos, connectedTag);
          return fwUpdateStateMachine.getState();
        });
      })
      .tapError(signal::error)
      .onNext(state -> {
        PrintLogger.d(TAG, "applyFirmware State: " + state);
        updateState(state, signal, autoExecute, isOnlyModule);
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

  private void updateState(FirmwareUpdateState state, Signal<FirmwareUpdateState> signal,
      boolean autoExecute, boolean isOnlyModule) {
    signal.next(state);
    if (isOnlyModule){
      if (state.getType().equals(Type.COMPLETED)) {
        signal.complete();
      }
      return;
    }
    if (state.getType().equals(Type.TRANSFERRED)) {
      if (!autoExecute) {
        signal.complete();
        return;
      }
      executeFirmware().tapError(signal::error).onNext(fwState -> {
        signal.next(fwState);
        if (fwState.getType().equals(Type.COMPLETED)) {
          signal.complete();
        }
      });
    }
  }

  private Signal<ConnectedJacquardTag> getConnectedJacquardTag() {
    return JacquardManager.getInstance().getConnectionStateSignal(identifier).first()
        .flatMap(connectionState -> {
          if (!connectionState.isType(CONNECTED)) {
            return Signal.empty(new IllegalStateException("Device is not connected, address: " + identifier));
          }
          return Signal.from(connectionState.connected());
        });
  }
}