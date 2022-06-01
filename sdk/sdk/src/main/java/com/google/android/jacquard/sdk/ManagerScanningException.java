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

/** Base class for all exceptions thrown by {@link JacquardManager}. */
public abstract class ManagerScanningException extends IllegalStateException {

  /**
   * Constructs an ManagerScanningException with the specified detail
   * message.  A detail message is a String that describes this particular
   * exception.
   *
   * @param message the String that contains a detailed message
   */
  public ManagerScanningException(String message) {
    super(message);
  }

  /** Exception thrown when the Bluetooth adapter is disabled. */
  public static class BluetoothUnavailableException extends ManagerScanningException {

    /** Constructs an BluetoothUnavailableException. */
    public BluetoothUnavailableException() {
      super("Bluetooth is not available");
    }
  }

  /** Exception throws when a Bluetooth device is not found. */
  public static class BluetoothDeviceNotFound extends ManagerScanningException {

    /**
     * Constructs an BluetoothDeviceNotFound with the address of the device requested.
     * message.
     *
     * @param address the bluetooth address of the requested device.
     */
    public BluetoothDeviceNotFound(String address) {
      super(String.format("Bluetooth device with address %s was not found", address));
    }
  }
}
