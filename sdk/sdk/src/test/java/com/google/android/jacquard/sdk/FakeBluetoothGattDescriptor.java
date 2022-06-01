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
package com.google.android.jacquard.sdk;

import android.Manifest.permission;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import java.util.UUID;

/**
 * Fake implementation of {@link BluetoothGattDescriptor}.
 */
public class FakeBluetoothGattDescriptor extends BluetoothGattDescriptor {

  BluetoothGattCharacteristic bluetoothGattCharacteristic;

  /**
   * Create a new BluetoothGattDescriptor.
   * <p>Requires {@link permission#BLUETOOTH} permission.
   *
   * @param uuid        The UUID for this descriptor
   * @param permissions Permissions for this descriptor
   */
  public FakeBluetoothGattDescriptor(UUID uuid, int permissions) {
    super(uuid, permissions);
  }

  @Override
  public BluetoothGattCharacteristic getCharacteristic() {
    return bluetoothGattCharacteristic;
  }

  public void setBluetoothGattCharacteristic(
      BluetoothGattCharacteristic bluetoothGattCharacteristic) {
    this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
  }
}
