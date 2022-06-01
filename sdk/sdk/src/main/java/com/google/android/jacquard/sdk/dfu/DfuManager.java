/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.jacquard.sdk.dfu;

import androidx.annotation.Nullable;

import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.Module;
import com.google.android.jacquard.sdk.rx.Signal;

import java.util.List;

/** Interface for device firmware update of connect Jacquard tag. */
public interface DfuManager {

  /** Check for firmware updated of provided components. */
  Signal<List<DFUInfo>> checkFirmware(List<Component> components, boolean forceUpdate);

  /**
   * Check for firmware updated of provided components.
   *
   * <p>If vid/pid is provided, SDK will overwrite tag vid/pid and check updates. After success
   * response, update info will be wrapped with tag vid/pid again to apply tag updates.
   *
   * @param components - Tag component list.
   * @param vid - VendorID, for which the update has to be checked.
   * @param pid - ProductID, for which the update has to be checked.
   * @param forceUpdate - If `true`, api will check for update info from cloud instead of cache.
   */
  Signal<List<DFUInfo>> checkFirmware(
          List<Component> components, @Nullable String vid, @Nullable String pid, boolean forceUpdate);

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
   * @param moduleInfo - Loadable module DFUInfo object.
   */
  Signal<FirmwareUpdateState> applyModuleUpdate(List<DFUInfo> moduleInfo);

  /**
   * Check for loadable module updated of provided module id.
   *
   * @param module - Module object.
   */
  Signal<List<DFUInfo>> checkModuleUpdate(List<Module> module, boolean forceUpdate);

  /**
   * Check for update for all loadable modules present on the ujt.
   *
   */
  Signal<List<DFUInfo>> checkModuleUpdate(boolean forceUpdate);

  /** Stops the dfu process. If the dfu process is going on and also reset the dfu process. */
  void stop();

  /**
   * Returns the current state of {@link FirmwareUpdateStateMachine}.
   */
  Signal<FirmwareUpdateState> getCurrentState();
}
