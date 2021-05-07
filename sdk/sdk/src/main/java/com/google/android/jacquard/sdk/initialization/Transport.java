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
package com.google.android.jacquard.sdk.initialization;

import android.bluetooth.BluetoothGatt;
import android.util.Pair;
import com.google.android.jacquard.sdk.model.CharacteristicUpdate;
import com.google.android.jacquard.sdk.model.Peripheral.WriteType;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.atap.jacquard.protocol.JacquardProtocol.Notification;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;

/** Class for managing request and notification flowing in and out of the tag. */
public interface Transport {

  /**
   * Default timeout duration for ble commands.
   */
  int DEFAULT_TIMEOUT = 8000; // 8 seconds

  /** Returns the identifier for the peripheral. */
  String getPeripheralIdentifier();

  /** Returns the of the peripheral from ble cache. */
  String getDefaultDisplayName();

  /** Returns a Signal emitting notification from the peripheral. */
  Signal<Notification> getNotificationSignal();

  /** Signal emitting when values are written to the peripheral. */
  Signal<CharacteristicUpdate> getValueWrittenSignal();

  /**
   * Returns a signal emitting data received on ujt data channel.
   * @return
   */
  Signal<Pair<Integer, byte[]>> getDataTransport();

  /**
   * Enqueues a request to be send to the peripheral.
   *
   * @param request the request to send to the peripheral
   * @param writeType the {@link WriteType}.
   * @return a Signal emitting a single response
   */
  Signal<Response> enqueue(Request request, WriteType writeType, int retries);

  /**
   * Enqueues a request to be send to the peripheral.
   *
   * @param request the request to send to the peripheral
   * @param writeType the {@link WriteType}.
   * @param timeout duration in milliseconds
   * @return a Signal emitting a single response
   */
  Signal<Response> enqueue(Request request, WriteType writeType, int retries, long timeout);

  /**
   * Callback reporting the result of a write operation.
   * @param characteristicUpdate object holding the characteristic that was written to the device.
   */
  void valueWritten(CharacteristicUpdate characteristicUpdate);

  /**
   * Notify or response the updated characteristic.
   * @param characteristicUpdate object holding the characteristic that was written to the device.
   */
  void characteristicUpdated(CharacteristicUpdate characteristicUpdate);

  /** Returns pending requests count. */
  int getPendingRequestSize();

  /**
   * This function will send a connection parameter update request to the remote device.
   *
   * @param priority Request a specific connection priority. Must be one of {@link
   *                 BluetoothGatt#CONNECTION_PRIORITY_BALANCED}, {@link BluetoothGatt#CONNECTION_PRIORITY_HIGH}
   *                 or {@link BluetoothGatt#CONNECTION_PRIORITY_LOW_POWER}.
   * @throws IllegalArgumentException If the parameters are outside of their specified range.
   */
  void requestConnectionPriority(int priority);

  /**
   * Notify the requested rssi value for the ujt.
   * @param rssiValue the signal strength from the ujt.
   */
  void onRSSIValueUpdated(int rssiValue);

  /** Fetch the current tag's signal strength (i.e. rssi value). */
  Signal<Integer> fetchRSSIValue();

  /** Stop fetching current tag's signal strength (i.e. rssi value). */
  void stopRSSIValue();
}
