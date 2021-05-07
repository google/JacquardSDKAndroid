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

package com.google.android.jacquard.sdk;

import androidx.annotation.Nullable;
import com.google.android.jacquard.sdk.command.DeviceInfo;
import java.util.HashMap;
import java.util.Map;

public final class MemoryCache {

  private final Map<String, DeviceInfo> deviceInfoMap = new HashMap<>();

  MemoryCache() {
  }

  /** Returns the device info if present in the memory. */
  @Nullable
  public DeviceInfo getDeviceInfo(String identifier) {
    return deviceInfoMap.get(identifier);
  }

  /** Save the DeviceInfo in the memory. */
  public void putDeviceInfo(String identifier, DeviceInfo deviceInfo) {
    deviceInfoMap.put(identifier, deviceInfo);
  }

}
