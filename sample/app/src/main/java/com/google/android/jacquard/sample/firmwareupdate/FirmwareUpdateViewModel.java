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
import com.google.android.jacquard.sample.Preferences;
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

  private final NavController navController;
  private final ConnectivityManager connectivityManager;
  private final Preferences preferences;
  private Subscription connectionStateSub;
  private FirmwareManager firmwareManager;

  /**
   * Signal State for {@link FirmwareUpdateViewModel}
   */
  public Signal<State> stateSignal = Signal.create();

  public FirmwareUpdateViewModel(FirmwareManager firmwareManager, NavController navController,
      ConnectivityManager connectivityManager,
      Preferences preferences) {
    this.navController = navController;
    this.connectivityManager = connectivityManager;
    this.preferences = preferences;
    this.firmwareManager = firmwareManager;
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
    return firmwareManager.checkFirmware(preferences.getCurrentTag().address(), forceUpdate);
  }

  /** apply firmware updated of current connect tag. */
  public Signal<FirmwareUpdateState> applyFirmware(boolean autoExecute) {
    preferences.autoUpdateFlag(autoExecute);
    return firmwareManager.applyFirmware(preferences.getCurrentTag().address(), autoExecute);
  }

  /** execute firmware updated of current connect tag. */
  public Signal<FirmwareUpdateState> executeFirmware() {
    return firmwareManager.executeFirmware(preferences.getCurrentTag().address());
  }

  public Signal<FirmwareUpdateState> getState() {
    if (preferences.getCurrentTag() == null) {
      return Signal.empty();
    }
    return firmwareManager.getState(preferences.getCurrentTag().address());
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
        connectivityManager.getConnectionStateSignal(preferences.getCurrentTag().address())
            .distinctUntilChanged()
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

  public void saveAlmostReadyShown() {
    preferences.almostReadyDialogShown(/* isShown= */true);
  }

  public boolean isAlmostReadyShown() {
    return preferences.isAlmostReadyDialogShown();
  }

  public boolean isAutoUpdateChecked() {
    return preferences.isAutoUpdate();
  }
}
