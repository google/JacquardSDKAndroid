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
package com.google.android.jacquard.sdk.lm;

import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.CONNECTED;
import static com.google.android.jacquard.sdk.dfu.FirmwareUpdateState.Type.COMPLETED;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.jacquard.sdk.JacquardManager;
import com.google.android.jacquard.sdk.command.ListModulesCommand;
import com.google.android.jacquard.sdk.command.LoadModuleCommand;
import com.google.android.jacquard.sdk.command.LoadModuleNotificationSubscription;
import com.google.android.jacquard.sdk.command.NotificationSubscription;
import com.google.android.jacquard.sdk.command.ProtoCommandRequest;
import com.google.android.jacquard.sdk.command.UnloadModuleCommand;
import com.google.android.jacquard.sdk.dfu.DFUInfo;
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Module;
import com.google.android.jacquard.sdk.model.VidPidMid;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Base class for loadable modules. */
public abstract class LoadableModule {
  protected final String identifier;
  private final String tagSerialNumber;
  private boolean isInitialized;
  private VidPidMid targetUjtFirmwareVidPid;

  /** Extended class must implement this api to return loadable module skeleton. */
  public abstract Module getVidPidMid();

  public LoadableModule(@NonNull ConnectedJacquardTag tag) {
    this.identifier = tag.address();
    this.tagSerialNumber = tag.serialNumber();
    PrintLogger.i(this.getClass().getSimpleName(), "Don't forget to call initialize() api.");
  }

  /** Loads IMU module. */
  public Signal<Module> loadModule() {
    return enqueue(new LoadModuleCommand(getVidPidMid()))
        .flatMap(response -> subscribe(new LoadModuleNotificationSubscription()));
  }

  /** Unloads imu module. */
  public Signal<Boolean> unloadModule() {
    return enqueue(new UnloadModuleCommand(getVidPidMid()));
  }

  /**
   * Initializes loadable module(LM). If LM is not present on ujt, it will flash it to ujt as a dfu.
   * Before returning, this api will ensure that LM is loaded and ready for IMU sample collection.
   */
  public Signal<InitState> initialize() {
    PrintLogger.d(this.getClass().getSimpleName(), "Initialize #");
    if (isInitialized) {
      return Signal.from(InitState.ofInitialized());
    }
    return Signal.create(
        signal -> {
          final AtomicReference<Signal.Subscription> dfuSubscription = new AtomicReference<>();
          signal.next(InitState.ofInit());
          fetchLMDetails()
              .observe(
                  imuModule -> {
                    boolean isFound = false;
                    boolean isActive = false;
                    if (imuModule != null) {
                      isFound = true;
                      isActive = imuModule.isEnabled();
                    }
                    PrintLogger.d(
                        this.getClass().getSimpleName(),
                        "isFound # " + isFound + " # isActive # " + isActive);
                    if (isFound && !isActive) {
                      signal.next(InitState.ofActivate());
                      loadModule()
                          .map(ignore -> InitState.ofInitialized())
                          .first()
                          .observe(
                              loaded -> {
                                isInitialized = true;
                                signal.next(InitState.ofInitialized());
                                signal.complete();
                              },
                              error -> {
                                if (error != null) {
                                  PrintLogger.d(
                                      this.getClass().getSimpleName(),
                                      "module loading error# " + error.getMessage());
                                  dfuSubscription.set(performDfu(signal));
                                }
                              });
                    } else if (isActive) {
                      isInitialized = true;
                      signal.next(InitState.ofInitialized());
                      signal.complete();
                    } else {
                      dfuSubscription.set(performDfu(signal));
                    }
                  },
                  error -> {
                    if (error != null) {
                      signal.error(error);
                    }
                  });
          return new Signal.Subscription() {
            @Override
            protected void onUnsubscribe() {
              PrintLogger.d(this.getClass().getSimpleName(), "initialize # onUnsubscribe # ");
              getMyJacquardTag().onNext(tag -> tag.dfuManager().stop());
              if (dfuSubscription.get() != null) {
                dfuSubscription.get().unsubscribe();
              }
            }
          };
        });
  }

  /**
   * Fetch loadable module details from the connected tag.
   *
   * @return {@link Module}
   */
  public Signal<Module> fetchLMDetails() {
    PrintLogger.d(this.getClass().getSimpleName(), "fetchLoadableModuleDetails ## ");
    return enqueue(new ListModulesCommand())
        .map(
            loadableModules -> {
              Module module = null;
              for (Module des : loadableModules) {
                if (getVidPidMid().equals(des)) {
                  module = des;
                  break;
                }
              }
              return module;
            });
  }

  /** Returns associated ujt serial number. */
  protected final String tagSerialNumber() {
      return tagSerialNumber;
  }

  /** Enqueue the request to send to the connected tag. */
  protected final <Res, Request extends ProtoCommandRequest<Res>> Signal<Res> enqueue(Request request) {
    return getMyJacquardTag().flatMap(tag -> tag.enqueue(request));
  }

  /** Enqueue the request to send to the connected tag. */
  protected final <Res, Request extends ProtoCommandRequest<Res>> Signal<Res> enqueue(
      Request request, int retries, long timeout) {
    return getMyJacquardTag().flatMap(tag -> tag.enqueue(request, retries, timeout));
  }

  /** Subscribe to the specified tag notification. */
  protected final <Res> Signal<Res> subscribe(
      NotificationSubscription<Res> notificationSubscription) {
    return getMyJacquardTag().flatMap(tag -> tag.subscribe(notificationSubscription));
  }

  /**
   * Returns the associated {@link ConnectedJacquardTag}, throws {@link IllegalStateException} if
   * disconnected.
   */
  protected final Signal<ConnectedJacquardTag> getMyJacquardTag() {
    return JacquardManager.getInstance()
        .getConnectionStateSignal(identifier)
        .flatMap(
            state -> {
              if (state.getType() != CONNECTED) {
                return Signal.empty(
                    new IllegalStateException("Tag Disconnected. Identifier # " + identifier));
              }
              return Signal.from(state.connected());
            })
        .first();
  }

  /** Sets the BLE connection priority. */
  protected void setConnectionPriority(int priority) {
    getMyJacquardTag().onNext(tag -> tag
        .requestConnectionPriority(priority));
  }

  /** Specify vendor id and product id if you are looking for ujt firmware specific to your app. */
  protected void setTargetUjtFirmwareVidPid(@Nullable VidPidMid targetUjtFirmwareVidPid) {
    this.targetUjtFirmwareVidPid = targetUjtFirmwareVidPid;
  }

  private Signal.Subscription performDfu(Signal<InitState> signal) {
    PrintLogger.d(this.getClass().getSimpleName(), "perform DFU # ");
    return performUjtDfu(signal) // tag dfu done
        .flatMap(tagUpdateDone -> performLmDfu(signal)) // dfu done
        .flatMap(
            ignore -> {
              signal.next(InitState.ofActivate());
              return loadModule();
            })
        .first()
        .observe(
            loaded -> {
              isInitialized = true;
              signal.next(InitState.ofInitialized());
              signal.complete();
            },
            error -> {
              if (error != null) {
                signal.error(error);
              }
            });
  }

  private Signal<FirmwareUpdateState> performLmDfu(Signal<InitState> signal) {
    PrintLogger.d(this.getClass().getSimpleName(), "Perform Module Dfu # ");
    signal.next(InitState.ofCheckForUpdates());
    List<Module> moduleList = new ArrayList<>();
    moduleList.add(getVidPidMid());
    return getMyJacquardTag()
        .flatMap(
            tag ->
                tag.dfuManager()
                    .checkModuleUpdate(moduleList, true)
                    .flatMap(dfuInfo -> tag.dfuManager().applyModuleUpdate(dfuInfo)))
        .tap(
            firmwareUpdateState -> {
              PrintLogger.d(
                  this.getClass().getSimpleName(), "LM DFU # State # " + firmwareUpdateState);
              if (firmwareUpdateState.getType().equals(FirmwareUpdateState.Type.ERROR)) {
                PrintLogger.e(
                    this.getClass().getSimpleName(),
                    "Error in module dfu #",
                    firmwareUpdateState.error());
                signal.error(firmwareUpdateState.error());
              } else {
                signal.next(InitState.ofModuleDfu(firmwareUpdateState));
              }
            })
        .filter(status -> status.getType().equals(COMPLETED));
  }

  private Signal<FirmwareUpdateState> performUjtDfu(Signal<InitState> signal) {
    PrintLogger.d(this.getClass().getSimpleName(), "Perform Ujt DFU # ");
    signal.next(InitState.ofCheckForUpdates());
    return getMyJacquardTag()
        .flatMap(
            tag ->
                tag.dfuManager()
                    .checkFirmware(
                        ImmutableList.of(tag.tagComponent()),
                        targetUjtFirmwareVidPid == null ? null : targetUjtFirmwareVidPid.vid(),
                        targetUjtFirmwareVidPid == null ? null : targetUjtFirmwareVidPid.pid(),
                        false)
                    .flatMap(
                        updates -> {
                          if (updates.isEmpty()
                              || updates
                                  .get(0)
                                  .dfuStatus()
                                  .equals(DFUInfo.UpgradeStatus.NOT_AVAILABLE)) {
                            return Signal.from(FirmwareUpdateState.ofCompleted());
                          }
                          return tag.dfuManager().applyUpdates(updates, true);
                        }))
        .tap(
            firmwareUpdateState -> {
              PrintLogger.d(
                  this.getClass().getSimpleName(),
                  "Ujt Dfu # " + firmwareUpdateState.getType().name());
              if (firmwareUpdateState.getType().equals(FirmwareUpdateState.Type.ERROR)) {
                PrintLogger.e(
                    this.getClass().getSimpleName(),
                    "Error in ujt dfu #",
                    firmwareUpdateState.error());
                signal.error(firmwareUpdateState.error());
              } else {
                signal.next(InitState.ofTagDfu(firmwareUpdateState));
              }
            })
        .filter(status -> status.getType().equals(COMPLETED))
        .first();
  }
}
