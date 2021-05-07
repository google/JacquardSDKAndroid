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
package com.google.android.jacquard.sdk;

import static com.google.android.jacquard.sdk.util.Lists.unmodifiableCopyOf;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import com.google.android.jacquard.sdk.BleQueue.Command;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.CharacteristicUpdate;
import com.google.android.jacquard.sdk.model.JacquardError;
import com.google.android.jacquard.sdk.model.Peripheral;
import com.google.android.jacquard.sdk.rx.Signal;

/**
 * A class to receive {@link BluetoothGattCallback} callbacks.
 */
class BleGattCallback extends BluetoothGattCallback {

  private static final String TAG = BleGattCallback.class.getSimpleName();
  private final Signal<ConnectState> signal;
  private final BleQueue bleQueue;
  private final BluetoothDevice bluetoothDevice;

  /**
   * Creates new instance of BleGattCallback.
   *
   * @param signal a emitting {@link ConnectState}.
   * @param bluetoothDevice a paired {@link BluetoothDevice}.
   */
  BleGattCallback(Signal<ConnectState> signal, BleQueue bleQueue, BluetoothDevice bluetoothDevice) {
    this.signal = signal;
    this.bleQueue = bleQueue;
    this.bluetoothDevice = bluetoothDevice;
  }

  @Override
  public void onCharacteristicRead(BluetoothGatt gatt,
      BluetoothGattCharacteristic characteristic, int status) {
    PrintLogger
        .d(TAG, "onCharacteristicRead for: " + characteristic.getUuid() + " # status # " + status);
    signal.next(ConnectState
        .ofCharacteristicUpdated(
            CharacteristicUpdate
                .of(new Peripheral(gatt, bleQueue), cloneCharacteristic(characteristic))));
    bleQueue.completedCommand(Command.Type.READ_CHARACTERISTIC);
  }

  @Override
  public void onCharacteristicWrite(BluetoothGatt gatt,
      BluetoothGattCharacteristic characteristic, int status) {
    PrintLogger.d(TAG, "onCharacteristicWrite # status # " + status);
    if (isDevicePaired(status)) {
      PrintLogger.d(TAG, "onCharacteristicWrite success for: " + characteristic.getUuid());

      signal.next(ConnectState
          .ofValueWritten(
              CharacteristicUpdate
                  .of(new Peripheral(gatt, bleQueue), cloneCharacteristic(characteristic))));
      bleQueue.completedCommand(Command.Type.WRITE_CHARACTERISTIC);
    }
  }

  @Override
  public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
      int status) {
    PrintLogger.d(TAG, "onDescriptorWrite status # " + status);
    if (isDevicePaired(status)) {
      PrintLogger.d(TAG, "onDescriptorWrite success for: " + descriptor.getUuid());
      signal.next(ConnectState
          .ofValueWritten(CharacteristicUpdate.of(new Peripheral(gatt, bleQueue),
              descriptor.getCharacteristic())));
      bleQueue.completedCommand(Command.Type.WRITE_DESCRIPTOR);
    }
  }

  @Override
  public void onCharacteristicChanged(BluetoothGatt gatt,
      BluetoothGattCharacteristic characteristic) {
    PrintLogger.d(TAG, "onCharacteristicChanged for " + characteristic.getUuid());
    signal.next(ConnectState
        .ofCharacteristicUpdated(
            CharacteristicUpdate
                .of(new Peripheral(gatt, bleQueue), cloneCharacteristic(characteristic))));
  }

  @Override
  public void onServicesDiscovered(BluetoothGatt gatt, int status) {
    PrintLogger.d(TAG, "onServicesDiscovered status # " + status);
    signal.next(ConnectState
        .ofServicesDiscovered(new Peripheral(gatt, bleQueue),
            unmodifiableCopyOf(gatt.getServices())));
    bleQueue.completedCommand(Command.Type.DISCOVER_SERVICES);
  }

  @Override
  public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
    PrintLogger.d(TAG, "onConnectionStateChange # status # " + status + " # new state # " + newState);
    switch (newState) {
      case BluetoothProfile.STATE_CONNECTED:
        PrintLogger.d(TAG, "onConnectionStateChange STATE_CONNECTED");
        signal.next(ConnectState.ofConnected(new Peripheral(gatt, bleQueue)));
        break;
      case BluetoothProfile.STATE_DISCONNECTED:
        PrintLogger.d(TAG, "onConnectionStateChange STATE_DISCONNECTED");
        signal.next(ConnectState.ofDisconnected(new Peripheral(gatt, bleQueue),
            JacquardError.ofUnknownCoreBluetoothError()));
        break;
    }
  }

  @Override
  public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
    if (status == BluetoothGatt.GATT_SUCCESS) {
      signal.next(ConnectState.ofValueRssi(new Peripheral(gatt, bleQueue), rssi));
    }
  }

  private boolean isDevicePaired(int status) {
    if (status != BluetoothGatt.GATT_SUCCESS) {
      BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
      if (adapter == null || !adapter.getBondedDevices().contains(bluetoothDevice)) {
        return false;
      }
    }
    return true;
  }

  private BluetoothGattCharacteristic cloneCharacteristic(
      BluetoothGattCharacteristic characteristic) {
    BluetoothGattCharacteristic gattCharacteristic = new BluetoothGattCharacteristic(
        characteristic.getUuid(),
        characteristic.getProperties(), characteristic.getPermissions());
    gattCharacteristic.setValue(characteristic.getValue());
    return gattCharacteristic;
  }
}
