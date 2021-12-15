/*
 * Copyright 2021 Google LLC. All Rights Reserved.
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

package com.google.android.jacquard.sample;

import androidx.lifecycle.ViewModel;
import com.google.android.jacquard.sample.firmwareupdate.FirmwareManager;
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState;
import com.google.android.jacquard.sdk.rx.Signal;

/** View model for {@link MainActivity}. */
public class MainActivityViewModel extends ViewModel {

  private static final String TAG = MainActivityViewModel.class.getSimpleName();

  private final Preferences preferences;
  private final FirmwareManager firmwareManager;

  MainActivityViewModel(Preferences preferences, FirmwareManager firmwareManager) {
    this.preferences = preferences;
    this.firmwareManager = firmwareManager;
  }

  @Override
  protected void onCleared() {
    preferences.setFirstRun(false);
    super.onCleared();
  }

  public Signal<FirmwareUpdateState> getState() {
    if (preferences.getCurrentTag() == null) {
      return Signal.empty();
    }
    return firmwareManager.getState(preferences.getCurrentTag().address());
  }

  public boolean isAutoUpdate() {
    return preferences.isAutoUpdate();
  }

  public void removeAutoUpdate() {
    preferences.removeAutoUpdateFlag();
  }

  public void resetAlmostReadyShown() {
    preferences.almostReadyDialogShown(/* isShown= */false);
  }

  public void saveDFUInProgress() {
    preferences.flagDfuInProgress();
  }

  public void removeFlagDfuInProgress() {
    preferences.removeFlagDfuInProgress();
  }
}
