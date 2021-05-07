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

package com.google.android.jacquard.sample.firmwareupdate;

import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.CONNECTED;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;
import com.google.android.jacquard.sample.ConnectivityManager;
import com.google.android.jacquard.sdk.connection.ConnectionState;
import com.google.android.jacquard.sdk.dfu.DFUInfo;
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.GearState;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.ObservesNext;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.auto.value.AutoOneOf;
import java.util.List;

/** View model for the {@link FirmwareUpdateFragment}. */
public class FirmwareUpdateViewModel extends ViewModel {

  private static final String TAG = FirmwareUpdateViewModel.class.getSimpleName();

  private final FirmwareManager firmwareManager;
  private final NavController navController;
  private final ConnectivityManager connectivityManager;
  private List<DFUInfo> dfuInfoList;
  private Subscription connectionStateSub;
  /**
   * Signal State for {@link FirmwareUpdateViewModel}
   */
  public Signal<State> stateSignal = Signal.create();

  public FirmwareUpdateViewModel(FirmwareManager firmwareManager, NavController navController,
      ConnectivityManager connectivityManager) {
    this.firmwareManager = firmwareManager;
    this.navController = navController;
    this.connectivityManager = connectivityManager;
  }

  /**
   * State for {@link FirmwareUpdateViewModel}.
   */
  @AutoOneOf(State.Type.class)
  public abstract static class State {

    /**
     * Returns the Connected state.
     */
    public static State ofConnected(ConnectedJacquardTag tag) {
      return AutoOneOf_FirmwareUpdateViewModel_State.connected(tag);
    }

    /**
     * Returns the Disconnected state.
     */
    public static State ofDisconnected() {
      return AutoOneOf_FirmwareUpdateViewModel_State.disconnected();
    }

    /**
     * Returns the Error state.
     */
    public static State ofError(String error) {
      return AutoOneOf_FirmwareUpdateViewModel_State.error(error);
    }

    /**
     * Send connected state.
     */
    public abstract ConnectedJacquardTag connected();

    /**
     * Send disconnected state.
     */
    public abstract void disconnected();

    /**
     * Send error state.
     */
    public abstract String error();

    /**
     * Returns the type of Navigation.
     */
    public abstract State.Type getType();

    /**
     * Type of {@link State}.
     */
    public enum Type {
      CONNECTED,
      DISCONNECTED,
      ERROR
    }
  }

  /**
   * Notification emitted by this view model.
   */
  @AutoOneOf(Notification.Type.class)
  public abstract static class Notification {


    public static Notification ofGear(GearState gearState) {
      return AutoOneOf_FirmwareUpdateViewModel_Notification.gear(gearState);
    }

    public abstract Notification.Type getType();


    /**
     * Returns the state of the gear.
     */
    public abstract GearState gear();

    public enum Type {
      GEAR
    }
  }

  /**
   * Initialize view model, It should be called from onViewCreated.
   */
  public void init() {
    subscribeConnectionState();
  }

  /** Check for firmware updated of current connect tag. */
  public Signal<List<DFUInfo>> checkFirmware(boolean forceUpdate) {
    return firmwareManager.checkFirmware(forceUpdate)
        .tap(dfuInfos -> {
          PrintLogger.d(TAG, "checkFirmware result : " + dfuInfos);
          dfuInfoList = dfuInfos;
        })
        .tapError(error -> PrintLogger.d(TAG, "checkFirmware error : " + error.getMessage()));
  }

  /** apply firmware updated of current connect tag. */
  public Signal<FirmwareUpdateState> applyFirmware(boolean autoExecute) {
    return firmwareManager.applyFirmware(dfuInfoList, autoExecute).distinctUntilChanged()
        .tap(status -> PrintLogger.d(TAG, "applyFirmware response = " + status))
        .tapError(error -> PrintLogger.d(TAG, "applyFirmware error = " + error.getMessage()));
  }

  /** execute firmware updated of current connect tag. */
  public Signal<FirmwareUpdateState> executeFirmware() {
    return firmwareManager.executeFirmware();
  }

  /** Called when the back arrow in the toolbar is clicked. */
  public void backArrowClick() {
    navController.popBackStack();
  }

  /**
   * Clearance operation for ViewModel.
   */
  @Override
  protected void onCleared() {
    connectionStateSub.unsubscribe();
    super.onCleared();
  }

  private void subscribeConnectionState() {
    connectionStateSub =
        connectivityManager.getConnectionStateSignal().distinctUntilChanged()
            .observe(
                new ObservesNext<ConnectionState>() {
                  @Override
                  public void onNext(@NonNull ConnectionState connectionState) {
                    onConnectionState(connectionState);
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    onErrorState(t.getMessage());
                  }
                });
  }

  private void onConnectionState(ConnectionState connectionState) {
    if (connectionState.isType(CONNECTED)) {
      stateSignal.next(State.ofConnected(connectionState.connected()));
    } else {
      stateSignal.next(State.ofDisconnected());
    }
  }

  private void onErrorState(String message) {
    PrintLogger.d(TAG, /* message= */"Failed to connect : " + message);
    stateSignal.next(State.ofError(message));
  }

  /**
   * Connects to the Notification for the Tag.
   */
  public Signal<Notification> getGearNotifications(ConnectedJacquardTag tag) {
    return tag.getConnectedGearSignal().map(Notification::ofGear);
  }
}
