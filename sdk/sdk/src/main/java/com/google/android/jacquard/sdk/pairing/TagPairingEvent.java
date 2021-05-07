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
package com.google.android.jacquard.sdk.pairing;

import com.google.android.jacquard.sdk.model.CharacteristicUpdate;
import com.google.android.jacquard.sdk.model.FailedToConnect;
import com.google.android.jacquard.sdk.model.Peripheral;
import com.google.android.jacquard.sdk.model.ServicesDiscovered;
import com.google.auto.value.AutoOneOf;

/** The types of events that {@link TagPairingStateMachine} reacts to. */
@AutoOneOf(TagPairingEvent.Type.class)
public abstract class TagPairingEvent {

  public static TagPairingEvent ofConnected(Peripheral peripheral) {
    return AutoOneOf_TagPairingEvent.connected(peripheral);
  }

  public static TagPairingEvent ofFailedToConnect(FailedToConnect failedToConnect) {
    return AutoOneOf_TagPairingEvent.failedToConnect(failedToConnect);
  }

  public static TagPairingEvent ofMiscCoreBluetoothError(Throwable error) {
    return AutoOneOf_TagPairingEvent.miscCoreBluetoothError(error);
  }

  public static TagPairingEvent ofServicesDiscovered(ServicesDiscovered services) {
    return AutoOneOf_TagPairingEvent.servicesDiscovered(services);
  }

  public static TagPairingEvent ofNotificationStateUpdated(
      CharacteristicUpdate update) {
    return AutoOneOf_TagPairingEvent.notificationStateUpdated(update);
  }

  public abstract Type getType();

  public abstract Peripheral connected();

  public abstract FailedToConnect failedToConnect();

  public abstract Throwable miscCoreBluetoothError();

  public abstract CharacteristicUpdate notificationStateUpdated();

  public abstract ServicesDiscovered servicesDiscovered();

  public enum Type {
    CONNECTED,
    FAILED_TO_CONNECT,
    MISC_CORE_BLUETOOTH_ERROR,
    SERVICES_DISCOVERED,
    NOTIFICATION_STATE_UPDATED,
  }
}
