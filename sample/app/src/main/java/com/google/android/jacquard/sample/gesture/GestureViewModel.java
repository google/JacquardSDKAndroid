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

package com.google.android.jacquard.sample.gesture;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;
import com.google.android.jacquard.sample.ConnectivityManager;
import com.google.android.jacquard.sample.ConnectivityManager.Events;
import com.google.android.jacquard.sample.Preferences;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sdk.command.GestureNotificationSubscription;
import com.google.android.jacquard.sdk.connection.ConnectionState;
import com.google.android.jacquard.sdk.model.Gesture;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/** Viewmodel for {@link GestureFragment}. */
public class GestureViewModel extends ViewModel {

  private final ConnectivityManager connectivityManager;
  private final Signal<ConnectedJacquardTag> tag;
  private final NavController navController;
  private final Preferences preferences;
  private final List<GestureViewItem> gestures = new LinkedList<>();

  public GestureViewModel(
      ConnectivityManager connectivityManager,
      NavController navController, Preferences preferences) {
    this.connectivityManager = connectivityManager;
    this.tag = connectivityManager.getConnectedJacquardTag();
    this.navController = navController;
    this.preferences = preferences;
  }

  /** Emits a list of all performed gestures when a gesture is detected. */
  public Signal<List<GestureViewItem>> getGestures() {
    return tag.distinctUntilChanged()
        .switchMap(
            (Fn<ConnectedJacquardTag, Signal<Gesture>>)
                tag -> tag.subscribe(new GestureNotificationSubscription()))
        .scan(
            gestures,
            (list, gesture) -> {
              GestureViewItem item =
                  GestureViewItem.of(System.nanoTime(), gesture, getDrawableResId(gesture));
              gestures.add(0, item);
              return new ArrayList<>(gestures);
            });
  }

  /**
   * Emits connectivity events {@link Events}.
   */
  public Signal<Events> getConnectivityEvents() {
    return connectivityManager.getEventsSignal().distinctUntilChanged();
  }

  private int getDrawableResId(Gesture gesture) {
    switch (gesture.gestureType()) {
      case DOUBLE_TAP:
        return R.drawable.ic_double_tap;
      case BRUSH_OUT:
        return R.drawable.ic_brush_out;
      case SHORT_COVER:
        return R.drawable.ic_cover;
      case BRUSH_IN:
      default:
        return R.drawable.ic_brush_in;
    }
  }

  /** Called when the info button is clicked. */
  public void gestureInfoClick() {
    navController.navigate(R.id.action_gestureFragment_to_gestureInfoFragment);
  }

  /** Called when the up button is clicked. */
  public void upClick() {
    navController.popBackStack();
  }

  /** Return the tooltip string resId for first application run and null on subsequent runs. */
  @Nullable
  @StringRes
  public Integer getInfoButtonToolTip() {
    return preferences.isFirstRun() ? R.string.gesture_info_tooltip : null;
  }

  /** Data class consumed by {@link GestureAdapter}. */
  @AutoValue
  public abstract static class GestureViewItem {

    public static GestureViewItem of(long id, Gesture gesture, int drawableResId) {
      return new AutoValue_GestureViewModel_GestureViewItem(id, gesture, drawableResId);
    }

    /** An id for the data class. */
    public abstract long id();

    /** The gesture performed */
    public abstract Gesture gesture();

    /** An icon describing the gesture/ */
    @DrawableRes
    public abstract int drawableResId();
  }
}
