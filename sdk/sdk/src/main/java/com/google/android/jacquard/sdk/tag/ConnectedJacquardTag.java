/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk.tag;

import android.bluetooth.BluetoothGatt;
import android.util.Pair;
import com.google.android.jacquard.sdk.JacquardManager;
import com.google.android.jacquard.sdk.command.CommandRequest;
import com.google.android.jacquard.sdk.command.NotificationSubscription;
import com.google.android.jacquard.sdk.dfu.DfuManager;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.GearState;
import com.google.android.jacquard.sdk.model.TouchMode;
import com.google.android.jacquard.sdk.rx.Signal;
import java.util.List;

/**
 * This is a tag in connected state.
 * <p>
 * A connected tag is obtained by calling {@link JacquardManager#connect(AdvertisedJacquardTag)}.
 */
public interface ConnectedJacquardTag extends JacquardTag {

  /** The tags serial number. */
  String serialNumber();

  /** Signal for observing the name of the tag. */
  Signal<String> getCustomAdvName();

  /** The component of the tag. */
  Component tagComponent();

  /** The component of the gear. */
  Component gearComponent();

  /** Returns the list of all component which are associated to current tag. */
  List<Component> getComponents();

  /** The DfuManger for firmware update. */
  DfuManager dfuManager();

  /** Signal for observing the connected gear. */
  Signal<GearState> getConnectedGearSignal();

  /**
   * Sets the tag in the specified {@link TouchMode} mode. For this command, tag should be
   * attached to the gear.
   * @param gearComponent the attached gear component
   * @param touchMode the mode need to be set
   *
   * @return True if command is successful. False otherwise.
   */
  Signal<Boolean> setTouchMode(Component gearComponent, TouchMode touchMode);

  /**
   * Sends a request to the tag.
   * @param request the request to execute
   * @param <Res> type of the result emitted
   * @param <Request> type of the request
   * @param retries number of retries if the request fails
   * @param timeout timeout duration in milliseconds
   * @return a Signal emitting a single response
   */
  <Res, Request extends CommandRequest<Res>> Signal<Res> enqueue(Request request, int retries,
      long timeout);

  /**
   * Sends a request to the tag.
   * @param request the request to execute
   * @param retries number of retries if the request fails
   * @param <Res> type of the result emitted
   * @param <Request> type of the request
   * @return a Signal emitting a single response
   */
  <Res, Request extends CommandRequest<Res>> Signal<Res> enqueue(Request request,
      int retries);

  /**
   * Sends a request to the tag.
   * @param request the request to execute
   * @param <Res> type of the result emitted
   * @param <Request> type of the request
   * @return a Signal emitting a single response
   */
  <Res, Request extends CommandRequest<Res>> Signal<Res> enqueue(Request request);

  /**
   * Subscribes to notification.
   * @param notificationSubscription the notification to subscribe to
   * @param <Res> the type of the response
   * @return a Signal emitting the responses
   */
  <Res> Signal<Res> subscribe(NotificationSubscription<Res> notificationSubscription);

  Signal<Pair<Integer, byte[]>> getDataTransport();

  Signal<byte[]> getRawData();

  /**
   * This function will send a connection parameter update request to the remote device.
   *
   * @param priority Request a specific connection priority. Must be one of {@link
   *                 BluetoothGatt#CONNECTION_PRIORITY_BALANCED}, {@link BluetoothGatt#CONNECTION_PRIORITY_HIGH}
   *                 or {@link BluetoothGatt#CONNECTION_PRIORITY_LOW_POWER}.
   * @throws IllegalArgumentException If the parameters are outside of their specified range.
   */
  void requestConnectionPriority(int priority);
}
