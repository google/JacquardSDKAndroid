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

package com.google.android.jacquard.sample.renametag;

import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;
import com.google.android.jacquard.sample.ConnectivityManager;
import com.google.android.jacquard.sample.ConnectivityManager.Events;
import com.google.android.jacquard.sample.KnownTag;
import com.google.android.jacquard.sample.Preferences;
import com.google.android.jacquard.sdk.command.RenameTagCommand;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import java.util.List;

/** View model for the {@link RenameTagFragment}. */
public class RenameTagViewModel extends ViewModel {

  private static final String TAG = RenameTagViewModel.class.getSimpleName();

  private final Preferences preferences;
  private final NavController navController;
  private final ConnectivityManager connectivityManager;
  private KnownTag currentTag;

  public RenameTagViewModel(Preferences preferences, NavController navController,
      ConnectivityManager connectivityManager) {
    this.preferences = preferences;
    this.navController = navController;
    this.connectivityManager = connectivityManager;
  }

  /** Initialises the current tag in the view model. */
  public void initCurrentTag() {
    currentTag = preferences.getCurrentTag();
  }

  /** Handles the back arrow click in the toolbar. */
  public void backArrowClick() {
    navController.popBackStack();
  }

  /** Returns the current tag name. */
  public String getTagName() {
    return currentTag.displayName();
  }

  /** Updates the tag name of current paired tag. */
  public Signal<String> updateTagName(String tagName) {
    return connectivityManager.getConnectedJacquardTag().first()
        .flatMap((Fn<ConnectedJacquardTag, Signal<String>>) tag -> tag
            .enqueue(new RenameTagCommand(tagName)));
  }

  /**
   * Updates the current device data and tag name into the known devices list of given tag
   * identifier.
   */
  public void updateKnownDevices(String updatedTagName) {
    String tagIdentifier = preferences.getCurrentTag().identifier();
    List<KnownTag> knownTags = preferences.getKnownTags();
    KnownTag knownTag = KnownTag
        .of(tagIdentifier, updatedTagName, preferences.getCurrentTag().pairingSerialNumber(), null);
    for (int i = 0; i < knownTags.size(); i++) {
      if (knownTags.get(i).identifier().equals(tagIdentifier)) {
        knownTags.set(i, knownTag);
        break;
      }
    }
    PrintLogger.d(TAG, "Persisting devices: " + knownTags);
    preferences.putKnownDevices(knownTags);
    preferences.putCurrentDevice(knownTag);
    currentTag = knownTag;
  }

  /**
   * Emits connectivity events {@link Events}.
   */
  public Signal<Events> getConnectivityEvents() {
    return connectivityManager.getEventsSignal().distinctUntilChanged();
  }
}
