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

package com.google.android.jacquard.sdk.dfu.execption;

/** Exception will be thrown in case of low battery. */
public class InsufficientBatteryException extends Exception {

  private final int batteryLevel;
  private final int thresholdLevel;

  public InsufficientBatteryException(int batteryLevel, int thresholdLevel) {
    super(
        "UJT has "
            + batteryLevel
            + "% battery. Minimum "
            + thresholdLevel
            + "% battery is required to proceed.");
    this.batteryLevel = batteryLevel;
    this.thresholdLevel = thresholdLevel;
  }

  public int getBatteryLevel() {
    return batteryLevel;
  }

  public int getThresholdLevel() {
    return thresholdLevel;
  }
}
