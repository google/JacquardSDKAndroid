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

package com.google.android.jacquard.sdk.tag;

import static com.google.common.truth.Truth.assertThat;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.FakeBleAdapter;
import com.google.android.jacquard.sdk.log.PrintLogger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class AdvertisedJacquardTagImplTest {
  private static final String ADDRESS = "C2:04:1C:6F:02:BA";
  private static final String SERIAL_NUMBER = "03WU";

  @Before
  public void setUp() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
  }

  @Test
  public void getPairingSerialNumber_returnsSerialNumber() {
    // Arrange
    byte[] data = {-64, 0, 122, 63,};
    // Act
    String serialNumber = AdvertisedJacquardTagImpl.getPairingSerialNumber(data);
    // Assert
    assertThat(serialNumber).isEqualTo(SERIAL_NUMBER);
  }

  @Test
  public void getPairingSerialNumber_dataNull_returnsSpecialCharacters() {
    // Act
    String serialNumber = AdvertisedJacquardTagImpl.getPairingSerialNumber(null);
    // Assert
    assertThat(serialNumber).isEqualTo("???");
  }

  @Test(expected = NullPointerException.class)
  public void of_scanRecordNull_throwsException() {
    // Arrange
    int eventType = 1;
    int primaryPhy = 2;
    int secondaryPhy = 3;
    int advertisingSid = 4;
    int txPower = 5;
    int rssi = 6;
    int periodicAdvertisingInterval = 7;
    long timestampNanos = 8;
    BluetoothDevice device = new FakeBleAdapter(ApplicationProvider.getApplicationContext())
        .getDevice(ADDRESS);
    ScanResult scanResult = new ScanResult(device, eventType, primaryPhy, secondaryPhy,
        advertisingSid, txPower, rssi,
        periodicAdvertisingInterval, /* scanRecord= */null, timestampNanos);
    // Act
    AdvertisedJacquardTagImpl.of(scanResult);
  }
}
