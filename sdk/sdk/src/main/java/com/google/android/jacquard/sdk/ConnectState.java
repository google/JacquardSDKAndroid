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
package com.google.android.jacquard.sdk;

import android.bluetooth.BluetoothGattService;
import com.google.android.jacquard.sdk.model.CharacteristicUpdate;
import com.google.android.jacquard.sdk.model.FailedToConnect;
import com.google.android.jacquard.sdk.model.JacquardError;
import com.google.android.jacquard.sdk.model.Peripheral;
import com.google.android.jacquard.sdk.model.RssiUpdate;
import com.google.android.jacquard.sdk.model.ServicesDiscovered;
import com.google.auto.value.AutoOneOf;
import java.util.List;

/** Data class holding the different state the bluetooth connection can be in. */
@AutoOneOf(ConnectState.Type.class)
public abstract class ConnectState {

  /**
   * Created a new ConnectState connected instance
   * @param peripheral the peripheral connected to.
   * @return a ConnectState object.
   */
  static ConnectState ofConnected(Peripheral peripheral) {
    return AutoOneOf_ConnectState.connected(peripheral);
  }

  /**
   * Created a new ConnectState failedToConnect instance
   * @param peripheral the peripheral that failed to connected.
   * @param error the error reported.
   * @return a ConnectState object.
   */
  static ConnectState ofFailedToConnect(Peripheral peripheral, JacquardError error) {
    return AutoOneOf_ConnectState
        .failedToConnect(FailedToConnect.of(peripheral, error));
  }

  /**
   * Created a new ConnectState disconnected instance
   * @param peripheral the peripheral disconnected to.
   * @param error the error reported.
   * @return a ConnectState object.
   */
  static ConnectState ofDisconnected(Peripheral peripheral, JacquardError error) {
    return AutoOneOf_ConnectState.disconnected(FailedToConnect.of(peripheral, error));
  }

  /**
   * Created a new ConnectState disconnected instance
   * @param peripheral the peripheral connected to.
   * @param services the services discovered from the peripheral.
   * @return a ConnectState object.
   */
  static ConnectState ofServicesDiscovered(
      Peripheral peripheral, List<BluetoothGattService> services) {
    return AutoOneOf_ConnectState
        .servicesDiscovered(ServicesDiscovered.of(peripheral, services));
  }

  /**
   * Created a new ConnectState characteristicUpdated instance
   * @param update information about update.
   * @return a ConnectState object.
   */
  static ConnectState ofCharacteristicUpdated(CharacteristicUpdate update) {
    return AutoOneOf_ConnectState.characteristicUpdated(update);
  }

  /**
   * Created a new ConnectState valueWritten instance
   * @param update information about update.
   * @return a ConnectState object.
   */
  static ConnectState ofValueWritten(CharacteristicUpdate update) {
    return AutoOneOf_ConnectState.valueWritten(update);
  }

  /**
   * Created a new ConnectState valueRssi instance
   * @param peripheral the peripheral valueRssi to.
   * @param value information about rssi.
   * @return a ConnectState object.
   */
  static ConnectState ofValueRssi(Peripheral peripheral, int value) {
    return AutoOneOf_ConnectState.valueRssi(RssiUpdate.of(peripheral, value));
  }

  public boolean isType(Type type) {
    return getType() == type;
  }

  /** Return the type associated with this class. */
  public abstract Type getType();

  /** Returns a {@link Peripheral} from the connection.  */
  public abstract Peripheral connected();

  /** Returns a {@link FailedToConnect} error.  */
  public abstract FailedToConnect failedToConnect();

  /** Returns a {@link FailedToConnect} error. */
  public abstract FailedToConnect disconnected();

  /** Returns a {@link ServicesDiscovered}. */
  public abstract ServicesDiscovered servicesDiscovered();

  /** Returns a {@link CharacteristicUpdate}. */
  public abstract CharacteristicUpdate characteristicUpdated();

  /** Returns a {@link CharacteristicUpdate}. */
  public abstract CharacteristicUpdate valueWritten();

  /** Returns a {@link RssiUpdate}. */
  public abstract RssiUpdate valueRssi();

  public enum Type {
    CONNECTED, FAILED_TO_CONNECT, DISCONNECTED, SERVICES_DISCOVERED, CHARACTERISTIC_UPDATED, VALUE_WRITTEN, VALUE_RSSI
  }
}
