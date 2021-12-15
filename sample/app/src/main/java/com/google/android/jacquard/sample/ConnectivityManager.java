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

package com.google.android.jacquard.sample;

import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.CONNECTED;

import android.content.Context;
import android.content.IntentSender;
import com.google.android.jacquard.sdk.JacquardManager;
import com.google.android.jacquard.sdk.ManagerScanningException.BluetoothUnavailableException;
import com.google.android.jacquard.sdk.connection.ConnectionState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.GearState;
import com.google.android.jacquard.sdk.model.GearState.Type;
import com.google.android.jacquard.sdk.model.SdkConfig;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.AdvertisedJacquardTag;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.android.jacquard.sdk.util.BluetoothUtils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Manager for providing access to the jacquard tag. */
public class ConnectivityManager {

  private static final String TAG = ConnectivityManager.class.getSimpleName();

  /**
   * Gear and tag events
   */
  public enum Events {
    TAG_DISCONNECTED,
    TAG_DETACHED,
    TAG_CONNECTED,
    TAG_ATTACHED
  }

  private final JacquardManager jacquardManager;
  private final Map<String, Signal<ConnectionState>> connectionStateSignalMap = new ConcurrentHashMap<>();
  // For now, it will have max one entry at any point of time.
  private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();

  public ConnectivityManager(JacquardManager jacquardManager) {
    this.jacquardManager = jacquardManager;
  }

  /**
   * Initialized the sdk with provided SdkConfig object.
   *
   * @param config a {@link SdkConfig} to initialize the sdk.
   */
  public void init(SdkConfig config) {
    jacquardManager.init(config);
  }

  /** Starts scanning for jacquard devices. */
  public Signal<AdvertisedJacquardTag> startScanning() {
    return jacquardManager.startScanning();
  }

  /** Connects to the provided address. */
  public Signal<ConnectionState> connect(Context activityContext, String address,
      Fn<IntentSender, Signal<Boolean>> senderHandler) {
    PrintLogger.d(TAG, /* message= */"connect: " + address);
    if (!BluetoothUtils.isBluetoothEnabled()) {
      PrintLogger.d(TAG, /* message= */"Bluetooth not enabled while connect");
      return Signal.empty(new BluetoothUnavailableException());
    }
    clearSubscriptions(address);
    Signal<ConnectionState> connectionStateSignal = getConnectionStateSignal(address);
    subscriptions
        .put(address, createBond(activityContext, address, senderHandler, connectionStateSignal));
    return connectionStateSignal;
  }

  /**
   * Emits connection state for provided address. Due to the nature of sticky it will emit every time the
   * signal is subscribed so use distinctUntilChange().
   */
  public Signal<ConnectionState> getConnectionStateSignal(String address) {
    Signal<ConnectionState> connectionState = connectionStateSignalMap.get(address);
    if (connectionState == null) {
      connectionState = Signal.<ConnectionState>create().sticky();
      connectionStateSignalMap.put(address, connectionState);
    }
    return connectionState;
  }

  /**
   * Emits a provided address connectedTag and may emit multiple values so for one-off operations
   * use first() or distinctUntilChange().
   */
  public Signal<ConnectedJacquardTag> getConnectedJacquardTag(String address) {
    return getConnectionStateSignal(address)
        .filter(state -> state.isType(CONNECTED))
        .map(ConnectionState::connected);
  }

  /**
   * Emits {@link Events} for provided address tag paired/unpaired, attached/detached events.
   */
  public Signal<Events> getEventsSignal(String address) {
    return Signal
        .merge(getConnectionStateSignal(address)
                .map(state -> state.isType(CONNECTED) ? Events.TAG_CONNECTED
                    : Events.TAG_DISCONNECTED),
            getGearNotification(address)
                .map(state -> state.getType() == Type.ATTACHED ? Events.TAG_ATTACHED
                    : Events.TAG_DETACHED));
  }

  /**
   * Removes connection references of the tag.
   * @param address
   */
  public void forget(String address) {
    clearSubscriptions(address);
    closeConnectionSignal(address);
    jacquardManager.forget(address);
  }

  /**
   * Releases all allocation resources.
   */
  public void destroy() {
    clearAllSubscriptions();
    closeAllConnectionSignal();
    jacquardManager.destroy();
  }

  private void closeAllConnectionSignal() {
    for (Signal<ConnectionState> connectionStateSignal : connectionStateSignalMap.values()) {
      connectionStateSignal.complete();
    }
    connectionStateSignalMap.clear();
  }

  private void closeConnectionSignal(String address) {
    Signal<ConnectionState> connectionStateSignal = connectionStateSignalMap.remove(address);
    if (connectionStateSignal != null) {
      connectionStateSignal.complete();
    }
  }

  private Subscription createBond(Context activityContext, String address,
      Fn<IntentSender, Signal<Boolean>> senderHandler,
      Signal<ConnectionState> connectionStateSignal) {
    return jacquardManager.connect(activityContext, address, senderHandler)
        .tap(state -> PrintLogger.d(TAG, "createBond: " + state))
        .forward(connectionStateSignal);
  }

  private Signal<GearState> getGearNotification(String address) {
    return getConnectedJacquardTag(address)
        .distinctUntilChanged()
        .switchMap(
            (Fn<ConnectedJacquardTag, Signal<GearState>>)
                ConnectedJacquardTag::getConnectedGearSignal);
  }

  private void clearAllSubscriptions() {
    for (Subscription subscription : subscriptions.values()) {
      subscription.unsubscribe();
    }
    subscriptions.clear();
  }

  /**
   * Remove connectivityManager subscription.
   */
  private void clearSubscriptions(String address) {
    Subscription subscription = subscriptions.remove(address);
    if (subscription == null) {
      return;
    }
    subscription.unsubscribe();
  }
}
