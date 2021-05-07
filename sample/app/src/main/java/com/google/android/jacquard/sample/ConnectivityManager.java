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

import com.google.android.jacquard.sdk.JacquardManager;
import com.google.android.jacquard.sdk.ManagerScanningException.BluetoothUnavailableException;
import com.google.android.jacquard.sdk.connection.ConnectionState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.GearState;
import com.google.android.jacquard.sdk.model.GearState.Type;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.AdvertisedJacquardTag;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.android.jacquard.sdk.util.BluetoothUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

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

  private final Signal<ConnectionState> connectionStateSignal =
      Signal.<ConnectionState>create().sticky();

  private final Signal<ConnectedJacquardTag> connectedJacquardTag =
      connectionStateSignal
          .filter(state -> state.isType(CONNECTED))
          .map(ConnectionState::connected);

  private final Signal<Events> eventsSignal = Signal.<Events>create();
  // For now, it will have max one entry at any point of time.
  private final Map<String, List<Subscription>> subscriptions = new ConcurrentHashMap<>();

  public ConnectivityManager(JacquardManager jacquardManager) {
    this.jacquardManager = jacquardManager;
  }

  /** Starts scanning for jacquard devices. */
  public Signal<AdvertisedJacquardTag> startScanning() {
    return jacquardManager.startScanning();
  }

  /** Connects to the provided address. */
  public Signal<ConnectionState> connect(String address) {
    PrintLogger.d(TAG, /* message= */"connect: " + address);
    if (!BluetoothUtils.isBluetoothEnabled()) {
      PrintLogger.d(TAG, /* message= */"Bluetooth not enabled while connect");
      return Signal.empty(new BluetoothUnavailableException());
    }
    clearSubscriptions(address);
    List<Subscription> subscriptionList = new ArrayList<>();
    subscriptionList.add(createBond(address));
    subscriptionList.add(observeEvents());
    subscriptions.put(address, subscriptionList);
    return connectionStateSignal;
  }

  /**
   * Emits connection state of current tag. Due to the nature of sticky it will emit every time the
   * signal is subscribed so use distinctUntilChange().
   */
  public Signal<ConnectionState> getConnectionStateSignal() {
    return connectionStateSignal;
  }

  /**
   * Emits a connected tag and may emit multiple values so for one-off operations use first() or
   * distinctUntilChange().
   */
  public Signal<ConnectedJacquardTag> getConnectedJacquardTag() {
    return connectedJacquardTag;
  }

  /**
   * Emits {@link Events} for tag paired/unpaired, attached/detached events.
   */
  public Signal<Events> getEventsSignal() {
    return eventsSignal;
  }

  /**
   * Removes connection references of the tag.
   * @param address
   */
  public void forget(String address) {
    clearSubscriptions(address);
    jacquardManager.forget(address);
  }

  /**
   * Releases all allocation resources.
   */
  public void destroy() {
    clearAllSubscriptions();
    jacquardManager.destroy();
  }

  @NotNull
  private Subscription createBond(String address) {
    return jacquardManager.connect(address).forward(connectionStateSignal);
  }

  private Subscription observeEvents() {
    return Signal
        .merge(connectionStateSignal
                .map(state -> state.isType(CONNECTED) ? Events.TAG_CONNECTED
                    : Events.TAG_DISCONNECTED),
            getGearNotification()
                .map(state -> state.getType() == Type.ATTACHED ? Events.TAG_ATTACHED
                    : Events.TAG_DETACHED))
        .forward(eventsSignal);
  }

  private Signal<GearState> getGearNotification() {
    return connectedJacquardTag
        .distinctUntilChanged()
        .switchMap(
            (Fn<ConnectedJacquardTag, Signal<GearState>>)
                ConnectedJacquardTag::getConnectedGearSignal);
  }

  private void clearAllSubscriptions() {
    for (List<Subscription> list : subscriptions.values()) {
      for (Subscription subscription : list) {
        subscription.unsubscribe();
      }
    }
    subscriptions.clear();
  }

  /**
   * Remove connectivityManager subscription.
   */
  private void clearSubscriptions(String address) {
    List<Subscription> subscriptionsList = subscriptions.remove(address);
    if (subscriptionsList == null) {
      return;
    }
    for (Subscription subscription : subscriptionsList) {
      subscription.unsubscribe();
    }
    subscriptionsList.clear();
  }
}
