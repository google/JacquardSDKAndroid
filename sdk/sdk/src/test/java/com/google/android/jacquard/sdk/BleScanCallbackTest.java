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

import static android.bluetooth.le.ScanCallback.SCAN_FAILED_INTERNAL_ERROR;
import static com.google.common.truth.Truth.assertThat;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link BleScanCallback}.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class BleScanCallbackTest {

  private static final String ADDRESS = "C2:04:1C:6F:02:BA";

  private BleScanCallback scanCallback;
  private ScanResult scanResult;
  private String errorMessage;

  @Before
  public void setUp() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    Signal<ScanResult> signal = Signal.create();
    scanCallback = new BleScanCallback(signal);
    signal.onNext(result -> this.scanResult = result);
    signal.onError(error -> errorMessage = error.getMessage());
  }

  @Test
  public void onScanResult_emitsScanResult() {
    // Arrange
    int callbackType = 1;
    ScanResult scanResult = createScanResult();
    // Act
    scanCallback.onScanResult(callbackType, scanResult);
    // Assert
    assertThat(this.scanResult.getDevice()).isEqualTo(scanResult.getDevice());
  }

  @Test
  public void onBatchScanResults_emitsScanResult() {
    // Arrange
    ScanResult scanResult = createScanResult();
    // Act
    scanCallback.onBatchScanResults(ImmutableList.of(scanResult));
    // Assert
    assertThat(this.scanResult.getDevice()).isEqualTo(scanResult.getDevice());
  }

  @Test
  public void onScanFailed_emitsError() {
    // Act
    scanCallback.onScanFailed(SCAN_FAILED_INTERNAL_ERROR);
    // Assert
    assertThat(errorMessage).contains(String.valueOf(SCAN_FAILED_INTERNAL_ERROR));
  }

  private static ScanResult createScanResult() {
    int eventType = 1;
    int primaryPhy = 2;
    int secondaryPhy = 3;
    int advertisingSid = 4;
    int txPower = 5;
    int rssi = 6;
    int periodicAdvertisingInterval = 7;
    long timestampNanos = 8;
    BluetoothDevice device = new BleAdapter(ApplicationProvider.getApplicationContext())
        .getDevice(ADDRESS);
    return new ScanResult(device, eventType, primaryPhy, secondaryPhy,
        advertisingSid, txPower, rssi,
        periodicAdvertisingInterval, /* scanRecord= */null, timestampNanos);
  }
}
