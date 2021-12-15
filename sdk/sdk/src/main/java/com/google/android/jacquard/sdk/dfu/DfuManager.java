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

package com.google.android.jacquard.sdk.dfu;

import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.Module;
import com.google.android.jacquard.sdk.rx.Signal;
import java.util.List;

/** Interface for device firmware update of connect Jacquard tag. */
public interface DfuManager {

  /** Check for firmware updated of provided components. */
  Signal<List<DFUInfo>> checkFirmware(List<Component> components, boolean forceUpdate);

  /**
   * Apply the firmware updated.
   *
   * @param dfuInfos - List of DFUInfo object.
   * @param autoExecute - true if want to auto execute after successfully firmware upload.
   */
  Signal<FirmwareUpdateState> applyUpdates(List<DFUInfo> dfuInfos, boolean autoExecute);

  /** execute the firmware updated. */
  void executeUpdates();

  /**
   * Apply the module updated.
   *
   * @param dfuInfo - Loadable module DFUInfo object.
   */
  Signal<FirmwareUpdateState> applyModuleUpdate(DFUInfo dfuInfo);

  /**
   * Check for loadable module updated of provided module id.
   *
   * @param module - Module object.
   */
  Signal<DFUInfo> checkModuleUpdate(Module module);

  /** Stops the dfu process. If the dfu process is going on and also reset the dfu process. */
  void stop();

  /**
   * Returns the current state of {@link FirmwareUpdateStateMachine}.
   */
  Signal<FirmwareUpdateState> getCurrentState();
}
