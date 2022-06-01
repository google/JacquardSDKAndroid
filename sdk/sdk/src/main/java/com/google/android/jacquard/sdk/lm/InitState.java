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
 */

package com.google.android.jacquard.sdk.lm;

import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState;
import com.google.android.jacquard.sdk.imu.ImuModule;
import com.google.auto.value.AutoOneOf;

/**
 * States included in {@link ImuModule} init process.
 */
@AutoOneOf(InitState.Type.class)
public abstract class InitState {

  /**
   * Init started.
   */
  public abstract void init();

  /**
   * Tag dfu is in progress.
   *
   * @return Exact dfu progress.
   */
  public abstract FirmwareUpdateState tagDfu();

  /**
   * Module dfu is in progress.
   *
   * @return Exact dfu progress.
   */
  public abstract FirmwareUpdateState moduleDfu();

  /**
   * Checking if updates available for tag or module.
   */
  public abstract void checkForUpdates();

  /**
   * {@link ImuModule} is initialized.
   */
  public abstract void initialized();

  /**
   * Activating module.
   */
  public abstract void activate();

  public abstract Type getType();

  public boolean isType(Type type) {
    return getType() == type;
  }

  static InitState ofInit() {
    return AutoOneOf_InitState.init();
  }

  static InitState ofTagDfu(FirmwareUpdateState state) {
    return AutoOneOf_InitState.tagDfu(state);
  }

  static InitState ofModuleDfu(FirmwareUpdateState state) {
    return AutoOneOf_InitState.moduleDfu(state);
  }

  static InitState ofInitialized() {
    return AutoOneOf_InitState.initialized();
  }

  static InitState ofCheckForUpdates() {
    return AutoOneOf_InitState.checkForUpdates();
  }

  static InitState ofActivate() {
    return AutoOneOf_InitState.activate();
  }

  /**
   * List of states.
   */
  public enum Type {
    INIT, CHECK_FOR_UPDATES, TAG_DFU, MODULE_DFU, ACTIVATE, INITIALIZED
  }
}
