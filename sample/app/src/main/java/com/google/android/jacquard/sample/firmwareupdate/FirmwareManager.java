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

package com.google.android.jacquard.sample.firmwareupdate;

import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.CONNECTED;

import com.google.android.jacquard.sample.ConnectivityManager;
import com.google.android.jacquard.sdk.dfu.DFUInfo;
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import java.util.List;

/** Manager for providing access to the DFU updates. */
public class FirmwareManager {

  private static final String TAG = FirmwareManager.class.getSimpleName();
  private ConnectivityManager connectivityManager;

  public FirmwareManager(ConnectivityManager connectivityManager) {
    this.connectivityManager = connectivityManager;
  }

  /** Check for firmware updated of all components. */
  public Signal<List<DFUInfo>> checkFirmware(boolean forceUpdate) {
    return getConnectedJacquardTag()
        .flatMap(tag -> tag.dfuManager().checkFirmware(tag.getComponents(), forceUpdate));
  }

  /** Apply the firmware updated of provided DfuManager. */
  public Signal<FirmwareUpdateState> applyFirmware(List<DFUInfo> dfuInfos, boolean autoExecute) {
    return getConnectedJacquardTag()
        .flatMap(tag -> tag.dfuManager().applyUpdates(dfuInfos, autoExecute));
  }

  /** execute the firmware updated of provided DfuManager. */
  public Signal<FirmwareUpdateState> executeFirmware() {
    return getConnectedJacquardTag().flatMap(tag -> tag.dfuManager().executeUpdates());
  }

  /** Stops the dfu process. If the dfu process is going on and also reset the dfu process. */
  public void stop() {
    getConnectedJacquardTag().onNext(tag -> tag.dfuManager().stop());
  }

  private Signal<ConnectedJacquardTag> getConnectedJacquardTag() {
    return connectivityManager.getConnectionStateSignal().first().flatMap(connectionState -> {
      if (!connectionState.isType(CONNECTED)) {
        return Signal.empty(
            new IllegalStateException("Device is not connected."));
      }
      return Signal.from(connectionState.connected());
    });
  }
}
