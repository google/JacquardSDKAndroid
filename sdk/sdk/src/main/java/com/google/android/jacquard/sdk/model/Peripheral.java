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

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_SIGNED;
import static android.bluetooth.BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
import static android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
import static com.google.android.jacquard.sdk.util.BluetoothSig.DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import androidx.annotation.RequiresPermission;
import com.google.android.jacquard.sdk.BleQueue;
import com.google.android.jacquard.sdk.BleQueue.Command;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.util.BluetoothSig;
import java.util.Arrays;

/** Wrapper around {@link BluetoothGatt} that executes BLE commands using a queue. */
public class Peripheral {

  /**
   * Create a new Peripheral for the provided {@link BluetoothGatt}.
   * Since we can only do one asynchronous operation at a time we have a command queue to control
   * execution.
   * @param bluetoothGatt the underlying gatt to communicate with.
   * @param bleQueue a queue for executing commands.
   */
  public Peripheral(BluetoothGatt bluetoothGatt, BleQueue bleQueue) {
    this.gatt = bluetoothGatt;
    this.bleQueue = bleQueue;
  }

  private static final String TAG = Peripheral.class.getSimpleName();
  private final BluetoothGatt gatt;
  private final BleQueue bleQueue;

  /**
   * Reads from the characteristic.
   * Results are emitted from {@link com.google.android.jacquard.sdk.BleAdapter}.
   * @param characteristic the characteristic to read from.
   * @return true, if the read operation was initiated successfully
   */
  public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {

    if ((characteristic.getProperties() & PROPERTY_READ) == 0) {
      PrintLogger.e(TAG, "Characteristic " + characteristic + " cannot be read");
      return false;
    }

    return bleQueue.enqueue(new Command(Command.Type.READ_CHARACTERISTIC) {
      @Override
      public void run() {
        boolean readCharacteristic = gatt.readCharacteristic(characteristic);

        if (readCharacteristic) {
          PrintLogger.d(TAG, "readCharacteristic for " + characteristic.getUuid());
        } else {
          PrintLogger.e(TAG, String.format("ReadCharacteristic failed for characteristic: %s",
              characteristic.getUuid()));
          bleQueue.completedCommand(type);
        }
      }
    });
  }

  /**
   * Writes the payload to the {@link BluetoothGattCharacteristic} with the {@link WriteType}.
   * Results are emitted from {@link com.google.android.jacquard.sdk.BleAdapter}.
   * @param characteristic the characteristic to write to.
   * @param writeType the write type. One of {@link WriteType}.
   * @param payload the byte data to write.
   * @return true, if the write operation was initiated successfully
   */
  public boolean writeCharacteristic(
      BluetoothGattCharacteristic characteristic,
      WriteType writeType,
      byte[] payload) {

    if ((characteristic.getProperties() & writeType.property) == 0) {
      PrintLogger.e(TAG,
          "Characteristic" + characteristic + " does not support write type "
              + writeType.property);
      return false;
    }

    return bleQueue.enqueue(new Command(Command.Type.WRITE_CHARACTERISTIC) {

      @Override
      public void run() {
        PrintLogger.d(TAG, "writeCharacteristic for:" + characteristic.getUuid());

        characteristic.setValue(payload);
        characteristic.setWriteType(writeType.writeType);

        if (gatt.writeCharacteristic(characteristic)) {
          PrintLogger.d(TAG, String
              .format("writing %s to characteristic %s", Arrays.toString(payload),
                  characteristic.getUuid()));
        } else {
          PrintLogger.e(TAG, String.format("WriteCharacteristic failed for characteristic: %s",
              characteristic.getUuid()));
          bleQueue.completedCommand(type);
        }
      }
    });
  }

  /**
   * Enabled or disabled notifications for the characteristic.
   * Results are emitted from {@link com.google.android.jacquard.sdk.BleAdapter}.
   * @param characteristic the characteristic to toggle notification for.
   * @param enable wether to enable or disable notifications.
   * @return true, if the operation was initiated successfully
   */
  public boolean enableNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
    return bleQueue.enqueue(new Command(Command.Type.WRITE_DESCRIPTOR) {

      @Override
      public void run() {
        PrintLogger.d(TAG, "Enabling notifications for: " + characteristic.getUuid());
        boolean notificationSet = gatt.setCharacteristicNotification(characteristic, enable);
        PrintLogger.d(TAG, "Setting characteristic notification: " + notificationSet);

        BluetoothGattDescriptor descriptor =
            characteristic.getDescriptor(DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION);
        byte[] payload = getPayload();
        descriptor.setValue(payload);
        if (gatt.writeDescriptor(descriptor)) {
          PrintLogger.d(TAG, String
              .format("writing %s to descriptor %s", Arrays.toString(payload),
                  characteristic.getUuid()));
        } else {
          PrintLogger.e(TAG, String.format("Failed to enable notifications for characteristic: %s",
              characteristic.getUuid()));
          bleQueue.completedCommand(type);
        }
      }

      private byte[] getPayload() {
        byte[] payload = null;
        byte[] disable = {0, 0};
        if ((characteristic.getProperties() & PROPERTY_INDICATE) != 0) {
          payload = enable ? ENABLE_INDICATION_VALUE : disable;
        } else if ((characteristic.getProperties() & PROPERTY_NOTIFY)
            != 0) {
          payload = enable ? ENABLE_NOTIFICATION_VALUE : disable;
        }
        return payload;
      }
    });
  }

  /**
   * Starts discovering services.
   * Results are emitted from {@link com.google.android.jacquard.sdk.BleAdapter}.
   * @return true, if the operation was initiated successfully
   */
  public boolean discoverServices() {
    return bleQueue.enqueue(new Command(Command.Type.DISCOVER_SERVICES) {

      @Override
      public void run() {
        PrintLogger.d(TAG, "Discover services");
        gatt.discoverServices();
      }
    });
  }

  /** Returns the tags identifier. */
  public String getTagIdentifier() {
    return gatt.getDevice().getAddress();
  }

  /** Returns the tag name from Bluetooth cache. */
  @RequiresPermission(Manifest.permission.BLUETOOTH)
  public String getDefaultDisplayName() {
    return gatt.getDevice().getName();
  }

  /** Returns the BluetoothGattService for accessing jacquard services. */
  public BluetoothGattService getJacquardService() {
    return gatt.getService(BluetoothSig.JQ_SERVICE_2);
  }

  public boolean requestRssi() {
    return gatt.readRemoteRssi();
  }

  /**
   * Sets Bluetooth  connection priority for ble data transfer.
   */
  public void requestConnectionPriority(int priority) {
    gatt.requestConnectionPriority(priority);
  }

  /**
   * WriteType describes the type of writes are supported.
   */
  public enum WriteType {
    /**
     * Write characteristic and requesting acknowledgement by the remote peripheral
     */
    WITH_RESPONSE(WRITE_TYPE_DEFAULT, PROPERTY_WRITE),

    /**
     * Write characteristic without requiring a response by the remote peripheral
     */
    WITHOUT_RESPONSE(WRITE_TYPE_NO_RESPONSE, PROPERTY_WRITE_NO_RESPONSE),

    /**
     * Write characteristic including authentication signature
     */
    SIGNED(WRITE_TYPE_SIGNED, PROPERTY_SIGNED_WRITE);

    private final int writeType;
    private final int property;

    WriteType(int writeType, int property) {
      this.writeType = writeType;
      this.property = property;
    }
  }
}
