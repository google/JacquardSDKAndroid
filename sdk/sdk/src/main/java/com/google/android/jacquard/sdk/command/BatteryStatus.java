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
package com.google.android.jacquard.sdk.command;

import com.google.atap.jacquard.protocol.JacquardProtocol.BatteryStatusNotification;
import com.google.atap.jacquard.protocol.JacquardProtocol.BatteryStatusResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.ChargingStatus;
import com.google.auto.value.AutoValue;

/** Data class for holding the tags battery status. */
@AutoValue
public abstract class BatteryStatus {

  public enum ChargingState {
    CHARGING, NOT_CHARGING
  }

  /** Returns the charging state. */
  public abstract ChargingState chargingState();

  /** Returns the battery level from 0 to 100. */
  public abstract int batteryLevel();

  static BatteryStatus of(BatteryStatusNotification notification) {
    ChargingState chargingState = getChargingState(notification.getChargingStatus());
    return new AutoValue_BatteryStatus(chargingState, notification.getBatteryLevel());
  }

  static BatteryStatus of(BatteryStatusResponse batteryStatusResponse) {
    ChargingState chargingState = getChargingState(batteryStatusResponse.getChargingStatus());
    return new AutoValue_BatteryStatus(chargingState, batteryStatusResponse.getBatteryLevel());
  }

  private static ChargingState getChargingState(ChargingStatus chargingStatus) {
    switch (chargingStatus) {
      case CHARGING:
        return ChargingState.CHARGING;
      case NOT_CHARGING:
        return ChargingState.NOT_CHARGING;
    }
    return ChargingState.NOT_CHARGING;
  }
}
