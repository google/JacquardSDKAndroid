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

import android.bluetooth.BluetoothDevice;
import com.google.android.jacquard.sdk.JacquardManager;

/**
 * A Jacquard tag that has been discovered during a Bluetooth LE scan.
 * <p>
 * A connection to the tag can be established by calling
 * {@link JacquardManager#connect(AdvertisedJacquardTag)}.
 */
public interface AdvertisedJacquardTag extends JacquardTag {

  /** The tag as a {@link BluetoothDevice}. */
  BluetoothDevice bluetoothDevice();

  /** Serial number used during pairing. The last 4 digit of the UJT serial number */
  String pairingSerialNumber();
}
