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
package com.google.android.jacquard.sample.scan;

import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.CONNECTED;

import android.content.Context;
import android.content.IntentSender;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;
import com.google.android.jacquard.sample.ConnectivityManager;
import com.google.android.jacquard.sample.KnownTag;
import com.google.android.jacquard.sample.Preferences;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.splash.SplashFragmentDirections;
import com.google.android.jacquard.sdk.connection.ConnectionState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.ObservesNext;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.AdvertisedJacquardTag;
import com.google.auto.value.AutoOneOf;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/** View model for {@link ScanFragment}. */
public class ScanViewModel extends ViewModel {

  private static final int PAIRING_TIMEOUT = 30 * 1000;
  private static final String TAG = ScanViewModel.class.getSimpleName();

  public final Signal<State> stateSignal = Signal.create();
  private final Preferences preferences;
  private final ConnectivityManager connectivityManager;
  private final NavController navController;
  private final List<Subscription> subscriptions = new ArrayList<>();
  private KnownTag tag;

  public ScanViewModel(
      ConnectivityManager connectivityManager,
      Preferences preferences,
      NavController navController) {
    this.connectivityManager = connectivityManager;
    this.preferences = preferences;
    this.navController = navController;
  }

  public Signal<List<AdapterItem>> startScanning() {
    List<AdvertisedJacquardTag> tags = new ArrayList<>();
    Signal<List<AdapterItem>> scanningSignal =
        connectivityManager
            .startScanning()
            .distinct()
            .scan(
                tags,
                (advertisedJacquardTags, tag) -> {
                  advertisedJacquardTags.add(tag);
                  return advertisedJacquardTags;
                })
            .map(this::getAdvertisingTagSection)
            .map(
                advertisingSection -> {
                  List<AdapterItem> items = new ArrayList<>(advertisingSection);
                  items.addAll(getKnownTagsSection());
                  return items;
                });
    Signal<List<AdapterItem>> knownTags = Signal.just(getKnownTagsSection());
    return Signal.merge(scanningSignal, knownTags);
  }

  private List<AdapterItem> getKnownTagsSection() {
    List<AdapterItem> knownTagSection = new ArrayList<>();
    List<KnownTag> knownTags = preferences.getKnownTags();
    if (!knownTags.isEmpty()) {
      knownTagSection
          .add(AdapterItem.ofSectionHeader(R.string.scan_adapter_section_title_previously_connected_tags));
    }
    for (KnownTag knownTag : knownTags) {
      knownTagSection.add(AdapterItem.ofTag(knownTag));
    }
    return knownTagSection;
  }

  private List<AdapterItem> getAdvertisingTagSection(
      List<AdvertisedJacquardTag> advertisedJacquardTags) {
    List<AdapterItem> tags = new ArrayList<>();
    tags.add(AdapterItem.ofSectionHeader(R.string.scan_page_nearby_tags_header));
    for (AdvertisedJacquardTag advertisedJacquardTag : advertisedJacquardTags) {
      tags.add(AdapterItem.ofTag(KnownTag.of(advertisedJacquardTag)));
    }
    return tags;
  }

  private void persistKnownDevices(KnownTag tag) {
    Set<KnownTag> knownTags = new HashSet<>(preferences.getKnownTags());
    knownTags.add(tag);
    PrintLogger.d(TAG, "Persisting devices: " + knownTags);
    preferences.putKnownDevices(knownTags);
    preferences.putCurrentDevice(tag);
  }

  /** Sets the selected known tag. */
  public void setSelected(KnownTag tag) {
    this.tag = tag;
  }

  /** Connects to the selected tag. */
  public void connect(Context context,
      final @NonNull Fn<IntentSender, Signal<Boolean>> senderHandler) {
    stateSignal.next(State.ofConnecting());
    subscriptions.add(connectivityManager.connect(context, tag.identifier(), senderHandler)
        .filter(connectionState -> connectionState.isType(CONNECTED)).first()
        .timeout(PAIRING_TIMEOUT)
        .observe(new ObservesNext<ConnectionState>() {
                   @Override
                   public void onNext(@NonNull ConnectionState connectionState) {
                     stateSignal.next(State.ofConnected());
                   }

                   @Override
                   public void onError(@NonNull Throwable t) {
                     PrintLogger.e(TAG, "Failed to connect: " + t);
                     String errorMsg;
                     if (t instanceof TimeoutException) {
                       errorMsg = "Getting issue to tag pairing, Please retry.";
                     } else {
                       errorMsg = t.getMessage();
                     }
                     stateSignal.next(State.ofError(errorMsg));
                   }
                 }
        ));
  }

  /**
   * Saves the connected tag details and notify the tag is connected.
   */
  public void successfullyConnected(boolean isUserAlreadyOnboarded) {
    persistKnownDevices(tag);
    if (isUserAlreadyOnboarded) {
      navController.popBackStack();
      return;
    }
    navController.navigate(SplashFragmentDirections.actionToHomeFragment());
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    for (Subscription subscription : subscriptions) {
      subscription.unsubscribe();
    }
    subscriptions.clear();
  }

  @AutoOneOf(State.Type.class)
  public abstract static class State {

    public static State ofConnecting() {
      return AutoOneOf_ScanViewModel_State.connecting();
    }

    public static State ofConnected() {
      return AutoOneOf_ScanViewModel_State.connected();
    }

    public static State ofError(String errorMsg) {
      return AutoOneOf_ScanViewModel_State.error(errorMsg);
    }

    public abstract Type getType();

    abstract void connecting();

    abstract void connected();

    public abstract String error();

    public enum Type {
      CONNECTING,
      CONNECTED,
      ERROR
    }
  }
}
