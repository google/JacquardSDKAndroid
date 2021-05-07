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
package com.google.android.jacquard.sdk.util;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import androidx.annotation.RequiresPermission;

/** Holds utility methods for checking the state of the {@link BluetoothAdapter}.  */
public class BluetoothUtils {

  /**
   * Returns true if the {@link BluetoothAdapter} is available and turned on.
   * @return true if the local adapter is turned on
   */
  @RequiresPermission(Manifest.permission.BLUETOOTH)
  public static boolean isBluetoothEnabled() {
    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    return adapter != null && adapter.isEnabled();
  }

}
