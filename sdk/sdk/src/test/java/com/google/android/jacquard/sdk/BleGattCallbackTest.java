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

import static android.bluetooth.BluetoothGatt.GATT_FAILURE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static com.google.android.jacquard.sdk.ConnectState.Type.CHARACTERISTIC_UPDATED;
import static com.google.android.jacquard.sdk.ConnectState.Type.CONNECTED;
import static com.google.android.jacquard.sdk.ConnectState.Type.DISCONNECTED;
import static com.google.android.jacquard.sdk.ConnectState.Type.SERVICES_DISCOVERED;
import static com.google.android.jacquard.sdk.ConnectState.Type.VALUE_WRITTEN;
import static com.google.android.jacquard.sdk.util.BluetoothSig.RESPONSE_UUID;
import static com.google.common.truth.Truth.assertThat;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBluetoothDevice;

/**
 * Unit tests for {@link BleGattCallback}.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class BleGattCallbackTest {

  private static final String ADDRESS = "C2:04:1C:6F:02:BA";

  private BleGattCallback bleGattCallback;
  private ConnectState connectState;

  @Before
  public void setUp() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    Signal<ConnectState> signal = Signal.create();
    BleQueue bleQueue = new BleQueue();
    bleGattCallback = new BleGattCallback(signal, bleQueue,
        ShadowBluetoothDevice.newInstance(ADDRESS));
    signal.onNext(state -> connectState = state);
  }

  @Test
  public void onCharacteristicRead_statusGattSuccess_emitsCharacteristicUpdated() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID, /* properties= */0, /* permissions= */ 0);
    // Act
    bleGattCallback
        .onCharacteristicRead(/* gatt= */ null, bluetoothGattCharacteristic, GATT_SUCCESS);
    // Assert
    assertThat(connectState.getType()).isEqualTo(CHARACTERISTIC_UPDATED);
    assertThat(connectState.characteristicUpdated().characteristic())
        .isNotEqualTo(bluetoothGattCharacteristic);
  }

  @Test
  public void onCharacteristicWrite_statusGattSuccess_emitsValueWritten() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID, /* properties= */0, /* permissions= */ 0);
    // Act
    bleGattCallback
        .onCharacteristicWrite(/* gatt= */ null, bluetoothGattCharacteristic, GATT_SUCCESS);
    // Assert
    assertThat(connectState.getType()).isEqualTo(VALUE_WRITTEN);
    assertThat(connectState.valueWritten().characteristic())
        .isNotEqualTo(bluetoothGattCharacteristic);
  }

  @Test
  public void onCharacteristicWrite_statusNotGattSuccess_emitsFailedToConnect() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID, /* properties= */0, /* permissions= */ 0);
    // Act
    bleGattCallback
        .onCharacteristicWrite(/* gatt= */ null, bluetoothGattCharacteristic, GATT_FAILURE);
    // Assert
    assertThat(connectState).isNull();
  }

  @Test
  public void onDescriptorWrite_statusGattSuccess_emitsValueWritten() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID, /* properties= */0, /* permissions= */ 0);
    FakeBluetoothGattDescriptor fakeBluetoothGattDescriptor = new FakeBluetoothGattDescriptor(
        RESPONSE_UUID,/* permissions= */ 0);
    fakeBluetoothGattDescriptor.setBluetoothGattCharacteristic(bluetoothGattCharacteristic);
    // Act
    bleGattCallback
        .onDescriptorWrite(/* gatt= */ null, fakeBluetoothGattDescriptor, GATT_SUCCESS);
    // Assert
    assertThat(connectState.getType()).isEqualTo(VALUE_WRITTEN);
  }

  @Test
  public void onDescriptorWrite_statusNotGattSuccess_emitsFailedToConnect() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID, /* properties= */0, /* permissions= */ 0);
    FakeBluetoothGattDescriptor fakeBluetoothGattDescriptor = new FakeBluetoothGattDescriptor(
        RESPONSE_UUID,/* permissions= */ 0);
    fakeBluetoothGattDescriptor.setBluetoothGattCharacteristic(bluetoothGattCharacteristic);
    // Act
    bleGattCallback
        .onDescriptorWrite(/* gatt= */ null, fakeBluetoothGattDescriptor, GATT_FAILURE);
    // Assert
    assertThat(connectState).isNull();
  }

  @Test
  public void onCharacteristicChanged_statusGattSuccess_emitsCharacteristicsUpdated() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID, /* properties= */0, /* permissions= */ 0);
    // Act
    bleGattCallback
        .onCharacteristicChanged(/* gatt= */ null, bluetoothGattCharacteristic);
    // Assert
    assertThat(connectState.getType()).isEqualTo(CHARACTERISTIC_UPDATED);
    assertThat(connectState.characteristicUpdated().characteristic())
        .isNotEqualTo(bluetoothGattCharacteristic);
  }

  @Test
  public void onServicesDiscovered_statusGattSuccess_emitsCharacteristicsUpdated() {
    // Arrange
    BluetoothDevice device = new BleAdapter(ApplicationProvider.getApplicationContext())
        .getDevice(ADDRESS);
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID, /* properties= */0, /* permissions= */ 0);
    // Act
    bleGattCallback
        .onServicesDiscovered(
            device.connectGatt(ApplicationProvider.getApplicationContext(), true, bleGattCallback),
            GATT_SUCCESS);
    // Assert
    assertThat(connectState.getType()).isEqualTo(SERVICES_DISCOVERED);
  }

  @Test
  public void onConnectionStateChange_newStateConnected_emitsConnected() {
    // Arrange
    BluetoothDevice device = new BleAdapter(ApplicationProvider.getApplicationContext())
        .getDevice(ADDRESS);
    BluetoothGatt gatt = device
        .connectGatt(ApplicationProvider.getApplicationContext(), /* autoConnect= */true,
            bleGattCallback);
    // Act
    bleGattCallback
        .onConnectionStateChange(gatt, GATT_SUCCESS, STATE_CONNECTED);
    // Assert
    assertThat(connectState.getType()).isEqualTo(CONNECTED);
  }

  @Test
  public void onConnectionStateChange_newStateDisconnected_emitsDisconnected() {
    // Arrange
    BluetoothDevice device = new BleAdapter(ApplicationProvider.getApplicationContext())
        .getDevice(ADDRESS);
    BluetoothGatt gatt = device
        .connectGatt(ApplicationProvider.getApplicationContext(), /* autoConnect= */true,
            bleGattCallback);
    // Act
    bleGattCallback
        .onConnectionStateChange(gatt, GATT_SUCCESS, STATE_DISCONNECTED);
    // Assert
    assertThat(connectState.getType()).isEqualTo(DISCONNECTED);
  }

  @Test
  public void onConnectionStateChange_statusNotGattSuccess_emitsDisconnected() {
    // Arrange
    BluetoothDevice device = new BleAdapter(ApplicationProvider.getApplicationContext())
        .getDevice(ADDRESS);
    BluetoothGatt gatt = device
        .connectGatt(ApplicationProvider.getApplicationContext(), /* autoConnect= */true,
            bleGattCallback);
    // Act
    bleGattCallback
        .onConnectionStateChange(gatt, GATT_FAILURE, STATE_DISCONNECTED);
    // Assert
    assertThat(connectState.getType()).isEqualTo(DISCONNECTED);
  }
}
