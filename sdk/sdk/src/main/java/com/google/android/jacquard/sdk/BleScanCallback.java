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

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import com.google.android.jacquard.sdk.rx.Signal;
import java.util.List;

/**
 * A class to receive {@link ScanCallback} callbacks.
 */
class BleScanCallback extends ScanCallback {

  private final Signal<ScanResult> signal;

  /**
   * Creates new instance of BleScanCallback.
   *
   * @param signal a emitting {@link ScanResult} when devices are found
   */
  BleScanCallback(Signal<ScanResult> signal) {
    this.signal = signal;
  }

  @Override
  public void onScanResult(int callbackType, ScanResult result) {
    signal.next(result);
  }

  @Override
  public void onBatchScanResults(List<ScanResult> results) {
    for (ScanResult result : results) {
      signal.next(result);
    }
  }

  @Override
  public void onScanFailed(int errorCode) {
    signal.error(new Exception("Scan failed with errorCode" + errorCode));
  }
}
