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
package com.google.android.jacquard.sdk.util;

import android.bluetooth.BluetoothAssignedNumbers;
import java.util.UUID;

/**
 * A representation of GATT services assigned by the Bluetooth SIG
 *
 * <p>values from https://www.bluetooth.com/specifications/gatt/services
 */
public final class BluetoothSig {

  private static final long UUID_HEAD = 0x1000L;
  private static final long UUID_LAST = 0x800000805f9b34fbL;
  private static final long V2_UUID_HEAD = 0xd2f20000d165445cL;
  private static final long V2_UUID_LAST = 0xb0e12d6b642ec57bL;

  public static final UUID SERVICE_GENERIC_ACCESS = fromAssignedNumber(0x1800);
  public static final UUID SERVICE_BATTERY_SERVICE = fromAssignedNumber(0x180f);

  public static final UUID CHARACTERISTIC_GAP_DEVICE_NAME = fromAssignedNumber(0x2a00);
  public static final UUID CHARACTERISTIC_BATTERY_LEVEL = fromAssignedNumber(0x2a19);

  // The Jacquard V2 protocol service
  public static final UUID JQ_SERVICE_2 = UUID.fromString("D2F2BF0D-D165-445C-B0E1-2D6B642EC57B");
  public static final int JQ_MFG_ID = BluetoothAssignedNumbers.GOOGLE;
  // Used to receive and process responses to commands given to the device.
  public static final UUID RESPONSE_UUID = UUID.fromString("D2F2B8D0-D165-445C-B0E1-2D6B642EC57B");
  // Used to get and react to notifications produced by the device.
  public static final UUID NOTIFY_UUID = UUID.fromString("D2F2B8D1-D165-445C-B0E1-2D6B642EC57B");
  public static final UUID RAW_UUID = UUID.fromString("D2F2B8D2-D165-445C-B0E1-2D6B642EC57B");
  public static final UUID JQ_CHARACTERISTIC_COMMAND = fromV2AssignedNumber(0xeabb);
  public static final UUID JQ_CHARACTERISTIC_RESPONSE = fromV2AssignedNumber(0xb8d0);
  public static final UUID JQ_CHARACTERISTIC_NOTIFICATION = fromV2AssignedNumber(0xb8d1);
  public static final UUID JQ_RAW_CHARACTERISTIC  = fromV2AssignedNumber(0xb8d2);

  public static final UUID DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION = fromAssignedNumber(
      0x2902);

  private BluetoothSig() {
    // Empty
  }

  private static UUID fromAssignedNumber(long number) {
    return new UUID(UUID_HEAD | (number << 32), UUID_LAST);
  }

  private static UUID fromV2AssignedNumber(long number) {
    return new UUID(V2_UUID_HEAD | (number << 32), V2_UUID_LAST);
  }
}