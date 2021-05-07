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
package com.google.android.jacquard.sdk.model;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import com.google.android.jacquard.sdk.BleQueue;
import java.util.UUID;

/**
 * Fake implementation of {@link Peripheral}.
 */
public final class FakePeripheral extends Peripheral {

  private static final String IDENTIFIER = "C2:04:1C:6F:02:BA";
  private UUID bluetoothSigUuid;

  public FakePeripheral(BleQueue bleQueue) {
    super(/* bluetoothGatt= */ null, bleQueue);
  }

  @Override
  public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
    return true;
  }

  @Override
  public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic,
      WriteType writeType, byte[] payload) {
    return true;
  }

  @Override
  public boolean enableNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
    return true;
  }

  @Override
  public boolean discoverServices() {
    return true;
  }

  @Override
  public String getTagIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public String getDefaultDisplayName() {
    return "Fake-Jacquard Tag";
  }

  @Override
  public BluetoothGattService getJacquardService() {
    return new BluetoothGattService(/* uuid= */ bluetoothSigUuid, /* serviceType= */0);
  }

  public void setBluetoothSigUuid(UUID uuid) {
    bluetoothSigUuid = uuid;
  }

  @Override
  public void requestConnectionPriority(int priority) {
  }

  @Override
  public boolean requestRssi() {
    return true;
  }
}
