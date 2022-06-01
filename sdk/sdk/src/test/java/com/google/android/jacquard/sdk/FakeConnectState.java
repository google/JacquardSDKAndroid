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
 *
 */

package com.google.android.jacquard.sdk;

import android.bluetooth.BluetoothGattService;
import com.google.android.jacquard.sdk.model.CharacteristicUpdate;
import com.google.android.jacquard.sdk.model.FailedToConnect;
import com.google.android.jacquard.sdk.model.JacquardError;
import com.google.android.jacquard.sdk.model.Peripheral;
import com.google.android.jacquard.sdk.model.RssiUpdate;
import com.google.android.jacquard.sdk.model.ServicesDiscovered;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Fake implementation of {@link ConnectState}.
 */
public class FakeConnectState extends ConnectState {

  private Type type;
  private Peripheral peripheral;
  private CharacteristicUpdate characteristicUpdate;
  private List<BluetoothGattService> services;

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public Peripheral connected() {
    return peripheral;
  }

  @Override
  public FailedToConnect failedToConnect() {
    return null;
  }

  @Override
  public FailedToConnect disconnected() {
    return FailedToConnect.of(peripheral, JacquardError.ofInternalError());
  }

  @Override
  public ServicesDiscovered servicesDiscovered() {
    return ServicesDiscovered.of(peripheral,
        ImmutableList.of(new BluetoothGattService(/* uuid= */ null, /* serviceType= */0)));
  }

  @Override
  public CharacteristicUpdate characteristicUpdated() {
    return characteristicUpdate;
  }

  @Override
  public CharacteristicUpdate valueWritten() {
    return null;
  }

  @Override
  public RssiUpdate valueRssi() {
    return RssiUpdate.of(peripheral, 0);
  }

  /**
   * Returns disconnected state {@link Type#DISCONNECTED}.
   */
  public ConnectState getDisConnectedState() {
    return ofDisconnected(peripheral, JacquardError.ofUnknownCoreBluetoothError());
  }

  /**
   * Returns connected state {@link Type#CONNECTED}.
   */
  public ConnectState getConnectedState() {
    return ofConnected(peripheral);
  }

  /**
   * Returns service discovered state {@link Type#SERVICES_DISCOVERED}.
   */
  public ConnectState getServiceDiscoveredState() {
    return ofServicesDiscovered(peripheral, services);
  }

  /**
   * Returns value written state {@link Type#VALUE_WRITTEN}.
   */
  public ConnectState getValueWrittenState() {
    return ofValueWritten(characteristicUpdate);
  }

  /**
   * Returns characteristics updated state {@link Type#CHARACTERISTIC_UPDATED}.
   */
  public ConnectState getCharacteristicsUpdatedState() {
    return ofCharacteristicUpdated(characteristicUpdate);
  }

  /**
   * Returns failed to connect error state {@link Type#FAILED_TO_CONNECT}.
   */
  public ConnectState getFailedToConnectState() {
    return ofFailedToConnect(peripheral, JacquardError.ofUnknownCoreBluetoothError());
  }

  /**
   * Sets current state of connect.
   *
   * @param type value of connect state
   */
  public void setType(Type type) {
    this.type = type;
  }

  public void setPeripheral(Peripheral peripheral) {
    this.peripheral = peripheral;
  }

  /**
   * Sets characteristicUpdate as connect state.
   *
   * @param characteristicUpdate value of connect state
   */
  public void setCharacteristicUpdate(CharacteristicUpdate characteristicUpdate) {
    this.characteristicUpdate = characteristicUpdate;
  }

  public static ConnectState ofValueWritten(CharacteristicUpdate update) {
    return ConnectState.ofValueWritten(update);
  }

  public void setBluetoothGattServices(List<BluetoothGattService> services) {
    this.services = services;
  }
}
