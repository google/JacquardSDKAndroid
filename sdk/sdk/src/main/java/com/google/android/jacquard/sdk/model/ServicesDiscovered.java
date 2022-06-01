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
package com.google.android.jacquard.sdk.model;

import static com.google.android.jacquard.sdk.util.Lists.unmodifiableCopyOf;

import android.bluetooth.BluetoothGattService;
import com.google.auto.value.AutoValue;
import java.util.List;

/** Data class for holding {@link BluetoothGattService} discovered for a tag. */
@AutoValue
public abstract class ServicesDiscovered {

  /** Creates a new instance of {@link ServicesDiscovered}. */
  public static ServicesDiscovered of(Peripheral peripheral, List<BluetoothGattService> service) {
    return new AutoValue_ServicesDiscovered(peripheral, unmodifiableCopyOf(service));
  }

  /** The Peripheral the services belong to. */
  public abstract Peripheral peripheral();

  /** List of  {@link BluetoothGattService} discovered. */
  public abstract List<BluetoothGattService> services();
}
