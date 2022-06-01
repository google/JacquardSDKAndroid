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
package com.google.android.jacquard.sdk.pairing;

import androidx.core.util.Pair;
import com.google.android.jacquard.sdk.model.JacquardError;
import com.google.android.jacquard.sdk.model.Peripheral;
import com.google.auto.value.AutoOneOf;

/** The states the Pairing state machine can be in. */
@AutoOneOf(TagPairingState.Type.class)
public abstract class TagPairingState {

  public static TagPairingState ofPreparingToConnect() {
    return AutoOneOf_TagPairingState.preparingToConnect();
  }

  public static TagPairingState ofDisconnected() {
    return AutoOneOf_TagPairingState.disconnected();
  }

  public static TagPairingState ofBluetoothConnected() {
    return AutoOneOf_TagPairingState.bluetoothConnected();
  }

  public static TagPairingState ofAwaitingNotificationUpdates() {
    return AutoOneOf_TagPairingState.awaitingNotificationUpdates();
  }

  public static TagPairingState ofServicesDiscovered() {
    return AutoOneOf_TagPairingState.servicesDiscovered();
  }

  public static TagPairingState ofError(JacquardError error) {
    return AutoOneOf_TagPairingState.error(error);
  }

  public static TagPairingState ofTagPaired(Peripheral peripheral,
      RequiredCharacteristics requiredCharacteristics) {
    return AutoOneOf_TagPairingState.tagPaired(Pair.create(peripheral, requiredCharacteristics));
  }

  public abstract Type getType();

  public boolean isTerminal() {
    return getType() == Type.TAG_PAIRED;
  }

  public abstract void preparingToConnect();

  public abstract void disconnected();

  public abstract void bluetoothConnected();

  public abstract void servicesDiscovered();

  public abstract void awaitingNotificationUpdates();

  public abstract Pair<Peripheral, RequiredCharacteristics> tagPaired();

  public abstract JacquardError error();

  public enum Type {
    PREPARING_TO_CONNECT,
    DISCONNECTED,
    BLUETOOTH_CONNECTED,
    SERVICES_DISCOVERED,
    AWAITING_NOTIFICATION_UPDATES,
    TAG_PAIRED,
    ERROR
  }
}
