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

package com.google.android.jacquard.sample.touchdata;

import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;
import com.google.android.jacquard.sample.ConnectivityManager;
import com.google.android.jacquard.sample.ConnectivityManager.Events;
import com.google.android.jacquard.sdk.command.ContinuousTouchNotificationSubscription;
import com.google.android.jacquard.sdk.model.GearState;
import com.google.android.jacquard.sdk.model.GearState.Type;
import com.google.android.jacquard.sdk.model.TouchData;
import com.google.android.jacquard.sdk.model.TouchMode;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** View model for {@link TouchYourThreadsFragment}. */
public class TouchDataViewModel extends ViewModel {

  private static final Integer[] EMPTY_THREADS = new Integer[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
  private final ConnectivityManager connectivityManager;
  private final Signal<ConnectedJacquardTag> connectedJacquardTagSignal;
  private final NavController navController;

  public TouchDataViewModel(
      ConnectivityManager connectivityManager, NavController navController) {
    this.connectivityManager = connectivityManager;
    this.connectedJacquardTagSignal = connectivityManager.getConnectedJacquardTag();
    this.navController = navController;
  }

  private static List<Integer> getEmptyLines() {
    return Arrays.asList(EMPTY_THREADS);
  }

  /** Starts the touch stream. */
  public Signal<List<Integer>> startTouchStream() {
    return Signal.create(
        signal -> {
          Subscription subscription = getTouchData().forward(signal);
          return new Subscription() {
            @Override
            protected void onUnsubscribe() {
              subscription.unsubscribe();
              setTouchMode(TouchMode.GESTURE).consume();
              signal.complete();
            }
          };
        });
  }

  /** Emits line data from the gear or a list of 0's when the gear is detached. */
  private Signal<List<Integer>> getTouchData() {
    return Signal.merge(getTouchLinesFromGear(), getTouchLinesForDetachedState());
  }

  /** Returns a List touch data from the gear. */
  private Signal<List<Integer>> getTouchLinesFromGear() {
    return connectedJacquardTagSignal
        .distinctUntilChanged()
        .switchMap(
            (Fn<ConnectedJacquardTag, Signal<List<Integer>>>)
                tag ->
                    tag.setTouchMode(TouchMode.CONTINUOUS)
                        .flatMap(
                            (Fn<Boolean, Signal<List<Integer>>>)
                                modeEnabled -> {
                                  if (!modeEnabled) {
                                    return Signal.empty(
                                        new IllegalStateException(
                                            "Failed to enable continuous touch mode"));
                                  }
                                  return tag.subscribe(
                                      new ContinuousTouchNotificationSubscription())
                                      .map(TouchData::lines);
                                }));
  }

  /** Returns the number of touch threads. */
  public int getThreadCount() {
    return EMPTY_THREADS.length;
  }

  /** Called when the back arrow in the toolbar is clicked. */
  public void backArrowClick() {
    navController.popBackStack();
  }

  /**
   * Emits connectivity events {@link Events}.
   */
  public Signal<Events> getConnectivityEvents() {
    return connectivityManager.getEventsSignal().distinctUntilChanged();
  }

  private Signal<Boolean> setTouchMode(TouchMode mode) {
    // TODO: Revise the below code b/185917177
    AtomicReference<ConnectedJacquardTag> connectedJacquardTag = new AtomicReference<>();
    return connectedJacquardTagSignal
        .flatMap(
            tag -> {
              connectedJacquardTag.set(tag);
              return tag.getConnectedGearSignal();
            }).first().filter(gearState -> gearState.getType() == Type.ATTACHED)
        .flatMap(gearState ->
            connectedJacquardTag.get().setTouchMode(gearState.attached(), mode));
  }

  private Signal<GearState> getGearNotification() {
    return connectedJacquardTagSignal
        .distinctUntilChanged()
        .switchMap(
            (Fn<ConnectedJacquardTag, Signal<GearState>>)
                ConnectedJacquardTag::getConnectedGearSignal);
  }

  private Signal<List<Integer>> getTouchLinesForDetachedState() {
    return getGearNotification()
        .filter(gearState -> gearState.getType() == Type.DETACHED)
        .map(gearState -> getEmptyLines());
  }
}
