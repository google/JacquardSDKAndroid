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

package com.google.android.jacquard.sdk.dfu.model;

/** Representation of response for the SDK Settings rest api. */
public class PlatformSettings {

  private int minimumBatteryDFU = 10;

  public int getMinimumBatteryDFU() {
    return minimumBatteryDFU;
  }

  public void setMinimumBatteryDFU(int minimumBatteryDFU) {
    this.minimumBatteryDFU = minimumBatteryDFU;
  }
}
