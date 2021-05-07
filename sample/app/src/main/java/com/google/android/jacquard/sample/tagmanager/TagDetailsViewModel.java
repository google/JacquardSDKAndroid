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
package com.google.android.jacquard.sample.tagmanager;

import android.content.Context;
import android.content.IntentSender;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;
import com.google.android.jacquard.sample.ConnectivityManager;
import com.google.android.jacquard.sample.KnownTag;
import com.google.android.jacquard.sample.Preferences;
import com.google.android.jacquard.sample.home.HomeViewModel.Notification;
import com.google.android.jacquard.sdk.command.BatteryStatus;
import com.google.android.jacquard.sdk.command.BatteryStatusCommand;
import com.google.android.jacquard.sdk.command.BatteryStatusNotificationSubscription;
import com.google.android.jacquard.sdk.connection.ConnectionState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Revision;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A viewmodel for fragment {@link TagDetailsFragment}.
 */
public class TagDetailsViewModel extends ViewModel {

  private static final String TAG = TagDetailsViewModel.class.getSimpleName();

  private final NavController navController;
  private final Preferences preferences;
  private final List<Subscription> subscriptions = new ArrayList<>();
  private final Signal<ConnectedJacquardTag> connectedJacquardTagSignal;
  private final ConnectivityManager connectivityManager;

  public TagDetailsViewModel(
      ConnectivityManager connectivityManager,
      Signal<ConnectedJacquardTag> connectedJacquardTagSignal,
      Preferences preferences,
      NavController navController) {
    this.connectivityManager = connectivityManager;
    this.navController = navController;
    this.preferences = preferences;
    this.connectedJacquardTagSignal = connectedJacquardTagSignal;
  }

  /**
   * Connects to the Notification for the Tag.
   */
  public Signal<Notification> getNotification() {
    return connectedJacquardTagSignal
        .distinctUntilChanged()
        .switchMap(
            (Fn<ConnectedJacquardTag, Signal<Notification>>)
                tag ->
                    Signal.merge(
                        Arrays.asList(getBatteryNotifications(tag), getGearNotifications(tag))));
  }

  /**
   * Returns connection state publisher.
   */
  public Signal<ConnectionState> getConnectionStateSignal() {
    return connectivityManager.getConnectionStateSignal().distinctUntilChanged();
  }

  /**
   * Gets battery status for selected tag.
   */
  public Signal<BatteryStatus> getBatteryStatus() {
    return connectedJacquardTagSignal
        .first()
        .flatMap(
            (Fn<ConnectedJacquardTag, Signal<BatteryStatus>>)
                tag -> tag.enqueue(new BatteryStatusCommand()));
  }

  /**
   * Gets device version info for selected tag.
   */
  public Signal<Revision> getVersion() {
    return connectedJacquardTagSignal.first().map(tag -> tag.tagComponent().version());
  }

  /**
   * Returns true if selected tag is current tag.
   */
  public boolean checkIfCurrentTag(KnownTag knownTag) {
    KnownTag currentTag = preferences.getCurrentTag();
    if (currentTag.identifier().equals(knownTag.identifier())) {
      PrintLogger.d(TAG, "Selected tag is current tag");
      return true;
    }
    return false;
  }

  /**
   * Handles the back arrow click in the toolbar.
   */
  public void backArrowClick() {
    navController.popBackStack();
  }

  /**
   * Sets selected tag as current tag and initiates connect call for the tag.
   */
  public void selectAsCurrentTag(Context context, KnownTag knownTag,
      final @NonNull Fn<IntentSender, Signal<Boolean>> senderHandler) {
    PrintLogger.d(TAG, "Change current tag to :: " + knownTag.displayName());
    connectivityManager.connect(context, knownTag.identifier(), senderHandler);
    preferences.putCurrentDevice(knownTag);
    backArrowClick();
  }

  /**
   * Removes selected tag from known tag list.
   */
  public void forgetTag(Context context, KnownTag knownTag,
      final @NonNull Fn<IntentSender, Signal<Boolean>> senderHandler) {
    connectivityManager.forget(knownTag.identifier());
    List<KnownTag> knownTags = preferences.getKnownTags();
    knownTags.remove(knownTag);
    preferences.putKnownDevices(knownTags);
    if (knownTags.isEmpty()) {
      preferences.removeCurrentDevice();
      initiateScan();
    } else {
      KnownTag currentTagToSet = knownTags.get(0);
      selectAsCurrentTag(context, currentTagToSet, senderHandler);
    }
  }

  /**
   * Clearance operation for ViewModel.
   */
  @Override
  protected void onCleared() {
    for (Subscription subscription : subscriptions) {
      subscription.unsubscribe();
    }
    subscriptions.clear();
    super.onCleared();
  }

  private Signal<Notification> getBatteryNotifications(ConnectedJacquardTag tag) {
    return tag.subscribe(new BatteryStatusNotificationSubscription()).map(Notification::ofBattery);
  }

  private Signal<Notification> getGearNotifications(ConnectedJacquardTag tag) {
    return tag.getConnectedGearSignal().map(Notification::ofGear);
  }

  private void initiateScan() {
    PrintLogger.d(TAG, "Initiate paring flow.");
    navController.navigate(TagDetailsFragmentDirections
        .actionTagDetailsFragmentToScanFragment());
  }
}
