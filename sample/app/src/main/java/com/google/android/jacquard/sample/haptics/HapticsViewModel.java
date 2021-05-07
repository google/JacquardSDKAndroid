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
package com.google.android.jacquard.sample.haptics;

import android.content.res.Resources;
import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;
import com.google.android.jacquard.sample.ConnectivityManager;
import com.google.android.jacquard.sample.ConnectivityManager.Events;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sdk.command.HapticCommand;
import com.google.android.jacquard.sdk.command.HapticCommand.Frame;
import com.google.android.jacquard.sdk.connection.ConnectionState;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.GearState;
import com.google.android.jacquard.sdk.model.GearState.Type;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;

/**
 * A viewmodel for fragment {@link HapticsFragment}.
 */
public class HapticsViewModel extends ViewModel {

  private static final String TAG = HapticsViewModel.class.getSimpleName();

  private final ConnectivityManager connectivityManager;
  private final Signal<ConnectedJacquardTag> connectedJacquardTag;
  private final NavController navController;
  private final Signal<ConnectionState> connectionStateSignal;
  private final Resources resources;

  public HapticsViewModel(
      ConnectivityManager connectivityManager,
      NavController navController,
      Resources resources) {
    this.connectivityManager = connectivityManager;
    this.connectionStateSignal = connectivityManager.getConnectionStateSignal();
    this.connectedJacquardTag = connectivityManager.getConnectedJacquardTag();
    this.navController = navController;
    this.resources = resources;
  }

  /**
   * Handles back arrow in toolbar.
   */
  public void backArrowClick() {
    navController.popBackStack();
  }

  public Signal<Boolean> sendHapticRequest(HapticPatternType type) {
    Frame hapticFrame = type.getFrame();
    return getAttachedComponent().first().flatMap(attachedComponent ->
        getConnectedJacquardTag().first().switchMap(connectedJacquardTag -> connectedJacquardTag
            .enqueue(new HapticCommand(hapticFrame, attachedComponent)))
    );
  }

  /**
   * Emits connectivity events {@link Events}.
   */
  public Signal<Events> getConnectivityEvents() {
    return connectivityManager.getEventsSignal().distinctUntilChanged();
  }

  private Signal<GearState> getGearNotification() {
    return connectedJacquardTag
        .distinctUntilChanged()
        .switchMap(
            (Fn<ConnectedJacquardTag, Signal<GearState>>)
                ConnectedJacquardTag::getConnectedGearSignal);
  }

  private Signal<ConnectionState> getConnectionStateSignal() {
    return connectionStateSignal.distinctUntilChanged();
  }

  private Signal<ConnectedJacquardTag> getConnectedJacquardTag() {
    return connectedJacquardTag.distinctUntilChanged();
  }

  private Signal<Component> getAttachedComponent() {
    return getConnectionStateSignal().first().flatMap(connectionState -> {
      if (!connectionState.isType(ConnectionState.Type.CONNECTED)) {
        throw new IllegalStateException(resources.getString(R.string.tag_not_connected));
      }
      return getGearNotification().first();
    }).map(gearState -> {
      if (gearState.getType() != Type.ATTACHED) {
        throw new IllegalStateException(resources.getString(R.string.gear_detached));
      }
      return gearState.attached();
    });

  }
}
