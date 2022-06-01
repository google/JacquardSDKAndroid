/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.jacquard.sdk.imu;

/**
 * Sensors used for data collection.
 */
public enum Sensors {
  IMU(0),
  GEAR(1);
  private int id;

  Sensors(int id) {
    this.id = id;
  }

  public int id() {
    return id;
  }

  public static Sensors forId(int id) {
    switch (id) {
      case 1:
        return GEAR;
      case 0:
      default:
        return IMU;
    }
  }
}
