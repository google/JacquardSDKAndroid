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
package com.google.android.jacquard.sdk.imu;

import android.text.TextUtils;
import com.google.android.jacquard.sdk.dfu.DFUInfo;
import com.google.android.jacquard.sdk.dfu.DfuManager;
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.Module;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.common.collect.ImmutableList;
import java.net.URI;
import java.util.List;

/**
 * Fake implementation for {@link DfuManager}.
 */
public class FakeDfuManager implements DfuManager {

  private final String TAG_VID = "11-78-30-c8";
  private final String TAG_PID = "28-3b-e7-a0";

  private static final String LM_VID = "11-78-30-c8";
  private static final String LM_PID = "ef-3e-5b-88";
  private static final String LM_MID = "3d-0b-e7-53";

  private final Signal<FirmwareUpdateState> stateSignal;

  public FakeDfuManager() {
    stateSignal = Signal.<FirmwareUpdateState>create().sticky();
  }

  @Override
  public Signal<List<DFUInfo>> checkFirmware(List<Component> components, boolean forceUpdate) {
    return Signal.just(ImmutableList.of(getTagDfuInfo()));
  }

  @Override
  public Signal<DFUInfo> checkModuleUpdate(Module module) {
    return Signal.just(getModuleDfuInfo());
  }

  @Override
  public void stop() {
  }

  @Override
  public Signal<FirmwareUpdateState> applyUpdates(List<DFUInfo> dfuInfos, boolean autoExecute) {
    stateSignal.next(FirmwareUpdateState.ofCompleted());
    return getCurrentState();
  }

  /**
   * execute the firmware updated.
   */
  @Override
  public void executeUpdates() {
    stateSignal.next(FirmwareUpdateState.ofExecuting());
  }

  @Override
  public Signal<FirmwareUpdateState> applyModuleUpdate(DFUInfo dfuInfo) {
    stateSignal.next(FirmwareUpdateState.ofCompleted());
    return getCurrentState();
  }

  @Override
  public Signal<FirmwareUpdateState> getCurrentState() {
    return stateSignal.distinctUntilChanged();
  }

  private boolean isTag(Component component) {
    return TextUtils.equals(component.vendor().id(), TAG_VID) && TextUtils
        .equals(component.product().id(), TAG_PID);
  }

  private DFUInfo getTagDfuInfo() {
    return DFUInfo
        .create("002005000", "MANDATORY", URI.create("http://test.com"), TAG_VID, TAG_PID, null);
  }

  private DFUInfo getModuleDfuInfo() {
    return DFUInfo
        .create("000017000", "MANDATORY", URI.create("http://test.com"), LM_VID, LM_PID, LM_MID);
  }
}
