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
package com.google.android.jacquard.sdk.tag;

import android.bluetooth.BluetoothDevice;

import com.google.android.jacquard.sdk.rx.Signal;

/**
 * Fake implementation of {@link AdvertisedJacquardTag}.
 */
public class FakeAdvertisedJacquardTag implements AdvertisedJacquardTag {

  private final String displayName;

  private final BluetoothDevice bluetoothDevice;

  public FakeAdvertisedJacquardTag(String displayName) {
    this.displayName = displayName;
    bluetoothDevice = null;
  }

  public FakeAdvertisedJacquardTag(String displayName, BluetoothDevice bluetoothDevice) {
    this.displayName = displayName;
    this.bluetoothDevice = bluetoothDevice;
  }

  @Override
  public BluetoothDevice bluetoothDevice() {
    return bluetoothDevice;
  }

  @Override
  public String pairingSerialNumber() {
    return null;
  }

  @Override
  public Signal<Integer> rssiSignal() {
    return null;
  }

  @Override
  public String address() {
    return null;
  }

  @Override
  public String displayName() {
    return displayName;
  }
}