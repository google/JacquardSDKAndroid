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
package com.google.android.jacquard.sample.ledpattern;

import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;
import com.google.android.jacquard.sample.ConnectivityManager;
import com.google.android.jacquard.sample.ConnectivityManager.Events;
import com.google.android.jacquard.sample.Preferences;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sdk.command.PlayLedPatternCommand;
import com.google.android.jacquard.sdk.command.PlayLedPatternCommand.Color;
import com.google.android.jacquard.sdk.command.PlayLedPatternCommand.Frame;
import com.google.android.jacquard.sdk.command.PlayLedPatternCommand.LedPatternType;
import com.google.android.jacquard.sdk.command.PlayLedPatternCommand.PlayLedPatternCommandBuilder;
import com.google.android.jacquard.sdk.command.PlayLedPatternCommand.PlayType;
import com.google.android.jacquard.sdk.connection.ConnectionState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.GearState;
import com.google.android.jacquard.sdk.model.Product.Capability;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.android.jacquard.sdk.util.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A viewmodel for fragment {@link LedPatternFragment}.
 */
public class LedPatternViewModel extends ViewModel {

  private static final String TAG = LedPatternViewModel.class.getSimpleName();

  private final ConnectivityManager connectivityManager;
  private final Signal<ConnectedJacquardTag> connectedJacquardTag;
  private final NavController navController;
  private final StringUtils stringUtils;
  private final Preferences preferences;

  public LedPatternViewModel(
      ConnectivityManager connectivityManager, NavController navController,
      StringUtils stringUtilsInstance, Preferences preferences) {
    this.connectivityManager = connectivityManager;
    this.connectedJacquardTag = connectivityManager.getConnectedJacquardTag();
    this.navController = navController;
    this.stringUtils = stringUtilsInstance;
    this.preferences = preferences;
  }

  /**
   * Gives notification on gear state change.
   */
  public Signal<GearState> getGearNotification() {
    return connectedJacquardTag
        .distinctUntilChanged()
        .switchMap(ConnectedJacquardTag::getConnectedGearSignal);
  }

  /**
   * Connects to the provided address.
   */
  public Signal<ConnectionState> getConnectionStateSignal() {
    return connectivityManager.getConnectionStateSignal().distinctUntilChanged();
  }


  /**
   * Returns led patterns list to be played on tag/gear.
   */
  public List<LedPatternItem> getLedPatterns() {
    List<LedPatternItem> ledPatternItems = new ArrayList<>();

    ledPatternItems.add(
        LedPatternItem.builder()
            .icon(R.drawable.ic_blue)
            .text("Blue Blink")
            .frames(Collections.singletonList(Frame.of(Color.of(0, 0, 255), 1000)))
            .resumable(true)
            .haltAll(false)
            .playType(PlayType.TOGGLE)
            .ledPatternType(LedPatternType.PATTERN_TYPE_SINGLE_BLINK)
            .build());

    ledPatternItems.add(
        LedPatternItem.builder()
            .icon(R.drawable.ic_green)
            .text("Green Blink")
            .frames(Collections.singletonList(Frame.of(Color.of(0, 255, 0), 1000)))
            .resumable(true)
            .haltAll(false)
            .playType(PlayType.TOGGLE)
            .ledPatternType(LedPatternType.PATTERN_TYPE_SINGLE_BLINK)
            .build());

    ledPatternItems.add(
        LedPatternItem.builder()
            .icon(R.drawable.ic_pink)
            .text("Pink Blink")
            .frames(Collections.singletonList(Frame.of(Color.of(255, 102, 178), 1000)))
            .resumable(true)
            .haltAll(false)
            .playType(PlayType.TOGGLE)
            .ledPatternType(LedPatternType.PATTERN_TYPE_SINGLE_BLINK)
            .build());

    ledPatternItems.add(
        LedPatternItem.builder()
            .icon(R.drawable.ic_blink)
            .text("Blink")
            .frames(Collections.singletonList(Frame.of(Color.of(220, 255, 255), 1000)))
            .resumable(true)
            .haltAll(false)
            .playType(PlayType.TOGGLE)
            .ledPatternType(LedPatternType.PATTERN_TYPE_SINGLE_BLINK)
            .build());

    ledPatternItems.add(
        LedPatternItem.builder()
            .icon(R.drawable.ic_strobe)
            .text("Strobe")
            .frames(
                new ArrayList<Frame>() {
                  {
                    add(Frame.of(Color.of(255, 0, 0), 100));
                    add(Frame.of(Color.of(247, 95, 0), 100));
                    add(Frame.of(Color.of(255, 204, 255), 100));
                    add(Frame.of(Color.of(0, 255, 0), 100));
                    add(Frame.of(Color.of(2, 100, 255), 100));
                    add(Frame.of(Color.of(255, 0, 255), 100));
                    add(Frame.of(Color.of(100, 255, 255), 100));
                    add(Frame.of(Color.of(2, 202, 255), 100));
                    add(Frame.of(Color.of(255, 0, 173), 100));
                    add(Frame.of(Color.of(113, 5, 255), 100));
                    add(Frame.of(Color.of(15, 255, 213), 100));
                  }
                })
            .resumable(true)
            .haltAll(false)
            .playType(PlayType.TOGGLE)
            .ledPatternType(LedPatternType.PATTERN_TYPE_CUSTOM)
            .build());

    ledPatternItems.add(
        LedPatternItem.builder()
            .icon(R.drawable.ic_shine)
            .text("Shine")
            .frames(Collections.singletonList(Frame.of(Color.of(220, 255, 255), 1000)))
            .resumable(true)
            .haltAll(false)
            .playType(PlayType.TOGGLE)
            .ledPatternType(LedPatternType.PATTERN_TYPE_SOLID)
            .build());

    ledPatternItems.add(
        LedPatternItem.builder()
            .icon(R.drawable.clear_icon)
            .text("Stop All")
            .frames(Collections.singletonList(Frame.of(Color.of(0, 0, 0), 1000)))
            .resumable(true)
            .haltAll(true)
            .playType(PlayType.TOGGLE)
            .ledPatternType(LedPatternType.PATTERN_TYPE_SOLID)
            .build());
    return ledPatternItems;
  }

  /**
   * Handles the back arrow click in the toolbar.
   */
  public void backArrowClick() {
    navController.popBackStack();
  }

  /**
   * Returns true if gear supports led otherwise false.
   */
  public boolean checkGearCapability(GearState state) {
    boolean ledSupportedByGear = false;
    for (Capability capability : state.attached().gearCapabilities()) {
      if (capability.ordinal() == Capability.LED.ordinal()) {
        ledSupportedByGear = true;
        break;
      }
    }
    return ledSupportedByGear;
  }

  /**
   * Plays LED Pattern on Gear.
   * @param patternItem pattern to be played.
   * @return Signal<Boolean> defines if pattern played successfully.
   */
  public Signal<Boolean> playLEDCommandOnGear(LedPatternItem patternItem, int durationMs) {
    PlayLedPatternCommandBuilder ledPatternCommandBuilder = getPlayLEDBuilder(patternItem, durationMs);
    return getGearNotification()
        .first()
        .filter(gearState -> gearState.getType() == GearState.Type.ATTACHED)
        .map(GearState::attached)
        .tap(c -> PrintLogger.d(TAG, "LED pattern Component # " + c))
        .flatMap(component ->
            connectedJacquardTag
                .first()
                .flatMap(tag ->
                    tag.enqueue(
                        ledPatternCommandBuilder.setComponent(component).build())));
  }

  /**
   * Plays LED Pattern on UJT.
   * @param patternItem pattern to be played.
   * @return Signal<Boolean> defines if pattern played successfully.
   */
  public Signal<Boolean> playLEDCommandOnUJT(LedPatternItem patternItem, int durationMs) {
    PlayLedPatternCommandBuilder ledPatternCommandBuilder = getPlayLEDBuilder(patternItem, durationMs);
    return connectedJacquardTag
        .first()
        .flatMap(
            tag ->
                tag.enqueue(
                    ledPatternCommandBuilder.setComponent(tag.tagComponent()).build()));
  }

  /**
   * Emits connectivity events {@link Events}.
   */
  public Signal<Events> getConnectivityEvents() {
    return connectivityManager.getEventsSignal().distinctUntilChanged();
  }

  private PlayLedPatternCommandBuilder getPlayLEDBuilder(LedPatternItem patternItem, int durationMs) {
    return  PlayLedPatternCommand.newBuilder()
            .setFrames(patternItem.frames())
            .setResumable(patternItem.resumable())
            .setPlayType(patternItem.playType())
            .setLedPatternType(patternItem.ledPatternType())
            .setHaltAll(patternItem.haltAll())
            .setIntensityLevel(100)
            .setDurationInMs(durationMs)
            .setStringUtils(stringUtils);
  }

  /**
   * Persists tag led state.
   *
   * @param isChecked true if tag led control is active
   */
  public void persistTagLedState(boolean isChecked) {
    preferences.persistTagLedState(isChecked);
  }

  /**
   * Returns true if tag led is active.
   */
  public boolean isTagLedActive() {
    return preferences.isTagLedActive();
  }

  /**
   * Persists gear led state.
   *
   * @param isActive true if gear led control is active
   */
  public void persistGearLedState(boolean isActive) {
    preferences.persistGearLedState(isActive);
  }

  /**
   * Returns true if gear led is active.
   */
  public boolean isGearLedActive() {
    return preferences.isGearLedActive();
  }
}
