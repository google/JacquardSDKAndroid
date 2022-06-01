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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentSender;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;

/**
 * Fake implementation of {@link BleAdapter}.
 */
public class FakeBleAdapter extends BleAdapter {

  Signal<ConnectState> stateSignal = Signal.create();

  /**
   * Creates a new instance of BleAdapter/
   *
   * @param context an Android context used to get access to the {@link BluetoothAdapter}.
   */
  public FakeBleAdapter(Context context) {
    super(context);
  }

  @Override
  public Signal<ConnectState> connect(Context activityContext, BluetoothDevice bluetoothDevice,
      Fn<IntentSender, Signal<Boolean>> senderHandler) {
    return stateSignal;
  }
}
