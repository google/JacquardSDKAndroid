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
package com.google.android.jacquard.sdk.model;

import com.google.auto.value.AutoOneOf;

/** The different errors the SDK can report. */
@AutoOneOf(JacquardError.Type.class)
public abstract class JacquardError {

  public static JacquardError ofInternalError() {
    return AutoOneOf_JacquardError.internalError();
  }

  public static JacquardError ofEmptyResponseError() {
    return AutoOneOf_JacquardError.emptyResponseError();
  }

  public static JacquardError ofMalformedResponseError() {
    return AutoOneOf_JacquardError.malformedResponseError();
  }

  public static JacquardError ofUnknownCoreBluetoothError() {
    return AutoOneOf_JacquardError.unknownCoreBluetoothError();
  }

  public static JacquardError ofBluetoothConnectionError(Throwable throwable) {
    return AutoOneOf_JacquardError.bluetoothConnectionError(throwable);
  }

  public static JacquardError ofBluetoothNotificationUpdateError(Throwable cause) {
    return AutoOneOf_JacquardError.bluetoothNotificationUpdateError(cause);
  }

  public static JacquardError ofServiceDiscoveryError() {
    return AutoOneOf_JacquardError.serviceDiscoveryError();
  }

  public static JacquardError ofJacquardInitializationError(Throwable cause) {
    return AutoOneOf_JacquardError.jacquardInitializationError(cause);
  }

  public static JacquardError ofBluetoothOffError() {
    return AutoOneOf_JacquardError.bluetoothOffError();
  }

  public abstract Type getType();

  public abstract void internalError();

  public abstract void emptyResponseError();

  public abstract void malformedResponseError();

  public abstract void unknownCoreBluetoothError();

  // A CoreBluetooth error was encountered during connection or service/characteristic discovery.
  public abstract Throwable bluetoothConnectionError();

  public abstract Throwable bluetoothNotificationUpdateError();

  public abstract void bluetoothOffError();

  public abstract void serviceDiscoveryError();

  public abstract void characteristicDiscoveryError();

  public abstract Throwable jacquardInitializationError();

  public enum Type {
    INTERNAL_ERROR,
    EMPTY_RESPONSE_ERROR,
    MALFORMED_RESPONSE_ERROR,
    UNKNOWN_CORE_BLUETOOTH_ERROR,
    BLUETOOTH_CONNECTION_ERROR,
    BLUETOOTH_OFF_ERROR,
    BLUETOOTH_NOTIFICATION_UPDATE_ERROR,
    SERVICE_DISCOVERY_ERROR,
    CHARACTERISTIC_DISCOVERY_ERROR,
    JACQUARD_INITIALIZATION_ERROR
  }
}
