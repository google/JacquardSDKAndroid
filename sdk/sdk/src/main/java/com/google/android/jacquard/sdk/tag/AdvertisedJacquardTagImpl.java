/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.jacquard.sdk.tag;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import androidx.annotation.VisibleForTesting;

import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.util.BluetoothSig;
import com.google.auto.value.AutoValue;

/** Concrete implementation of {@link AdvertisedJacquardTag}. */
@AutoValue
public abstract class AdvertisedJacquardTagImpl implements AdvertisedJacquardTag {

  private final Signal<Integer> rssiSignal = Signal.create();

  public static AdvertisedJacquardTagImpl of(ScanResult scanResult) {
    return new AutoValue_AdvertisedJacquardTagImpl(
        scanResult.getDevice(),
        getPairingSerialNumber(
            scanResult.getScanRecord().getManufacturerSpecificData(BluetoothSig.JQ_MFG_ID)),
        scanResult.getDevice().getAddress(),
        scanResult.getScanRecord().getDeviceName()
    );
  }

  @Override
  public Signal<Integer> rssiSignal() {
    return rssiSignal;
  }

  /** Extracts the pairing serial number from the scan result. */
  @VisibleForTesting
  static String getPairingSerialNumber( byte[] data) {
    return mfgDataToSerial(data);
  }

  /** Decodes the pairing serial number from the manufacturer specific data. */
  private static String decodeSerialNumberFromData(byte[] d) {
    long accum = 0; // Accumulator to aggregate multiple bytes' worth of bits
    int bitsLeft = 0; // How many bits of valid data are in the LSB of the accumulator
    int bytesUsed = 0; // How many bytes from the input data have been shifted into the accumulator

    int nchars =
        d.length * 8
            / 6; // It's a 6-bit encoding, so this is how many output characters are encoded

    StringBuilder outputString = new StringBuilder();
    for (int i = 0; i < nchars; i++) {
      // Check if we need to load more bits into the accumulator
      if (bitsLeft < 6) {
        if (bytesUsed == d.length) {
          // Used all the bytes from the input! Finished!
          break;
        }

        // Load the next byte in, shifted to the left to avoid bits already in the accumulator
        // (Java does not do unsigned math, so this is horrible.)
        accum = ((accum & 0xFFFFFFFFL) + (((d[bytesUsed] & 0xFF) << bitsLeft) & 0xFFFFFFFFL));
        bytesUsed += 1; // Mark one more byte used
        bitsLeft += 8; // Mark 8 bits available
      }

      int b = (int) (accum & 0x3F); // Take the lowest 6 bits of the accumulator

      // Decode the encoded character into [0-9A-Za-z-]
      if (b <= 9) {
        b += '0';
      } else if (b <= 35) {
        b += 'A' - 10;
      } else if (b <= 61) {
        b += 'a' - 36;
      } else if (b == 62) {
        b = '-';
      } else {
        continue; // Invalid characters are skipped
      }
      accum >>= 6;
      bitsLeft -= 6; // Mark those bits as used

      // Add it to the output string
      outputString.append((char) b);
    }

    return outputString.toString();
  }

  /** Decodes the pairing serial number from the manufacturer specific data. */
  private static String mfgDataToSerial(byte[] bs) {
    if (null != bs) {
      return decodeSerialNumberFromData(bs);
    }
    return "???";
  }

  @Override
  public abstract BluetoothDevice bluetoothDevice();

  @Override
  public abstract String pairingSerialNumber();

  @Override
  public abstract String address();

  @Override
  public abstract String displayName();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AdvertisedJacquardTagImpl)) return false;
    AdvertisedJacquardTagImpl that = (AdvertisedJacquardTagImpl) o;
    return bluetoothDevice().equals(that.bluetoothDevice()) &&
            pairingSerialNumber().equals(that.pairingSerialNumber()) &&
            address().equals(that.address()) &&
            displayName().equals(that.displayName());
  }
}
