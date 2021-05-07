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
package com.google.android.jacquard.sdk;

import android.Manifest.permission;
import android.bluetooth.BluetoothGattCharacteristic;
import java.util.UUID;

/**
 * A fake implementation of {@link BluetoothGattCharacteristic}.
 */
public class FakeBluetoothGattCharacteristic extends BluetoothGattCharacteristic {

  private final UUID uuid;
  private byte[] value;

  /**
   * Create a new BluetoothGattCharacteristic.
   * <p>Requires {@link permission#BLUETOOTH} permission.
   *
   * @param uuid        The UUID for this characteristic
   * @param properties  Properties of this characteristic
   * @param permissions Permissions for this characteristic
   */
  public FakeBluetoothGattCharacteristic(UUID uuid, int properties, int permissions) {
    super(uuid, properties, permissions);
    this.uuid = uuid;
  }

  @Override
  public UUID getUuid() {
    return uuid;
  }

  @Override
  public byte[] getValue() {
    return value;
  }

  @Override
  public boolean setValue(byte[] value) {
    this.value = value;
    return true;
  }
}