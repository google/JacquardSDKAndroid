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
package com.google.android.jacquard.sdk.model;

import android.bluetooth.BluetoothGattCharacteristic;
import com.google.auto.value.AutoValue;

/**
 * Data class emitted by {@link com.google.android.jacquard.sdk.BleAdapter} when a
 * read, write or notification callback is received.
 **/
@AutoValue
public abstract class CharacteristicUpdate {

  /** Creates a new instance of CharacteristicUpdate. */
  public static CharacteristicUpdate of(Peripheral peripheral,
      BluetoothGattCharacteristic characteristic) {
    return new AutoValue_CharacteristicUpdate(peripheral, characteristic);
  }

  /** The peripheral that reported the event. */
  public abstract Peripheral peripheral();

  /** The characteristic that was updated. */
  public abstract BluetoothGattCharacteristic characteristic();
}
