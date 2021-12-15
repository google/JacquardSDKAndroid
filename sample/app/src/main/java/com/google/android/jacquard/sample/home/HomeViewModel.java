/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sample.home;

import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.CONNECTED;
import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.DISCONNECTED;
import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.PREPARING_TO_CONNECT;

import android.content.Context;
import android.content.IntentSender;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;
import com.google.android.jacquard.sample.ConnectivityManager;
import com.google.android.jacquard.sample.ConnectivityManager.Events;
import com.google.android.jacquard.sample.Preferences;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.firmwareupdate.FirmwareManager;
import com.google.android.jacquard.sample.firmwareupdate.FirmwareUpdateFragment;
import com.google.android.jacquard.sample.gesture.GestureFragment;
import com.google.android.jacquard.sample.haptics.HapticsFragment;
import com.google.android.jacquard.sample.ledpattern.LedPatternFragment;
import com.google.android.jacquard.sample.musicalthreads.MusicalThreadsFragment;
import com.google.android.jacquard.sample.places.PlacesConfigFragment;
import com.google.android.jacquard.sample.renametag.RenameTagFragment;
import com.google.android.jacquard.sample.tagmanager.TagManagerFragment;
import com.google.android.jacquard.sample.utilities.AbilityConstants;
import com.google.android.jacquard.sample.utilities.Recipe;
import com.google.android.jacquard.sample.utilities.RecipeManager;
import com.google.android.jacquard.sdk.command.BatteryStatus;
import com.google.android.jacquard.sdk.command.BatteryStatusCommand;
import com.google.android.jacquard.sdk.command.BatteryStatusNotificationSubscription;
import com.google.android.jacquard.sdk.connection.ConnectionState;
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.GearState;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.ObservesNext;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * View model for {@link HomeFragment}.
 */
public class HomeViewModel extends ViewModel implements ItemClickListener<HomeTileModel> {

  private static final String TAG = HomeViewModel.class.getSimpleName();

  /**
   * Parameter for detached state {@link State}.
   */
  @AutoValue
  public abstract static class ParamsDetached {

    /**
     * Returns the object for {@link ParamsDetached}.
     */
    public static ParamsDetached of(
        int detachStringResourceId, int detachIndicator, int detachTextColor) {
      return new AutoValue_HomeViewModel_ParamsDetached(
          detachStringResourceId, detachIndicator, detachTextColor);
    }

    /**
     * Returns the subtitle for detached state.
     */
    public abstract int detachStringResourceId();

    /**
     * Returns the indicator for detached state.
     */
    public abstract int detachIndicatorResourceId();

    /**
     * Returns the text color for detached state.
     */
    public abstract int detachColorResourceId();
  }

  /**
   * Parameter for setting Adapter.
   */
  @AutoValue
  public abstract static class ParamsAdapter {

    /**
     * Returns the object for {@link ParamsAdapter}.
     */
    public static ParamsAdapter of(
        ItemClickListener<HomeTileModel> itemClick, List<HomeTileModel> list) {
      return new AutoValue_HomeViewModel_ParamsAdapter(itemClick, list);
    }

    /**
     * Returns the ItemClickListener.
     */
    public abstract ItemClickListener<HomeTileModel> itemClickListener();

    /**
     * Returns the List<HomeTileModel>.
     */
    public abstract List<HomeTileModel> listForAdapter();
  }

  /**
   * State for {@link HomeViewModel}.
   */
  @AutoOneOf(State.Type.class)
  public abstract static class State {

    /**
     * Returns the State for set adapter.
     */
    public static State ofSetAdapter(ParamsAdapter paramsAdapter) {
      return AutoOneOf_HomeViewModel_State.adapter(paramsAdapter);
    }

    /**
     * Returns the Connected state.
     */
    public static State ofConnected(ConnectedJacquardTag tag) {
      return AutoOneOf_HomeViewModel_State.connected(tag);
    }

    /**
     * Returns the Disconnected state.
     */
    public static State ofDisconnected() {
      return AutoOneOf_HomeViewModel_State.disconnected();
    }

    /**
     * Returns the Error state.
     */
    public static State ofError(String error) {
      return AutoOneOf_HomeViewModel_State.error(error);
    }

    /**
     * Returns the Adapter for recyclerview.
     */
    public abstract ParamsAdapter adapter();

    /**
     * Send connected state.
     */
    public abstract ConnectedJacquardTag connected();

    /**
     * Send disconneted state.
     */
    public abstract void disconnected();

    /**
     * Send error state.
     */
    public abstract String error();

    /**
     * Returns the type of Navigation.
     */
    public abstract Type getType();

    /**
     * Type of {@link State}.
     */
    public enum Type {
      ADAPTER,
      CONNECTED,
      DISCONNECTED,
      ERROR
    }
  }

  /**
   * Notification emitted by this view model.
   */
  @AutoOneOf(Notification.Type.class)
  public abstract static class Notification {

    public static Notification ofBattery(BatteryStatus batteryStatus) {
      return AutoOneOf_HomeViewModel_Notification.battery(batteryStatus);
    }

    public static Notification ofGear(GearState gearState) {
      return AutoOneOf_HomeViewModel_Notification.gear(gearState);
    }

    public abstract Type getType();

    /**
     * Returns the current battery status.
     */
    public abstract BatteryStatus battery();

    /**
     * Returns the state of the gear.
     */
    public abstract GearState gear();

    public enum Type {
      BATTERY,
      GEAR
    }
  }

  private final NavController navController;
  private final Preferences preferences;
  private final List<Subscription> subscriptions = new ArrayList<>();
  private final ConnectivityManager connectivityManager;
  private final RecipeManager recipeManager;
  private Signal<ConnectionState> connectionStateSignal;
  private Signal<ConnectedJacquardTag> connectedJacquardTagSignal;
  private final FirmwareManager firmwareManager;
  private final Resources resources;

  /**
   * Signal State for {@link HomeViewModel}
   */
  public Signal<State> stateSignal = Signal.create();

  public HomeViewModel(
      Preferences preferences,
      NavController navController,
      ConnectivityManager connectivityManager,
      RecipeManager recipeManager,
      FirmwareManager firmwareManager,
      Resources resources) {
    this.navController = navController;
    this.preferences = preferences;
    this.connectivityManager = connectivityManager;
    this.recipeManager = recipeManager;
    this.firmwareManager = firmwareManager;
    this.resources = resources;
  }

  /**
   * Initialize view model, It should be called from onViewCreated.
   */
  public void init() {
    onCleared();
    connectionStateSignal = connectivityManager
        .getConnectionStateSignal(preferences.getCurrentTag().address());
    connectedJacquardTagSignal = connectivityManager
        .getConnectedJacquardTag(preferences.getCurrentTag().address());
    onTagDisconnected();
    preferences.setHomeLoaded(true);
    subscribeConnectionState();
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
   * Click of the Item from Recyclerview Tile.
   */
  @Override
  public void itemClick(HomeTileModel model) {
    onTileClick(model.title());
  }

  /**
   * Triggers state signal with data for Connected Tag.
   */
  public void onTagConnected() {
    stateSignal.next(
        State.ofSetAdapter(
            ParamsAdapter.of(
                /* itemClick= */ this,
                getHomeTilesList(/* isTagConnected= */ true, /* isAttached= */ false))));
  }

  /**
   * Triggers state signal with data for Attached Gear.
   */
  public void onTagAttached() {
    stateSignal.next(
        State.ofSetAdapter(
            ParamsAdapter.of(
                /* itemClick= */ this,
                getHomeTilesList(/* isTagConnected= */ true, /* isAttached= */ true))));
  }

  /**
   * Triggers state signal with data for disconnected Tag.
   */
  public void onTagDisconnected() {
    stateSignal.next(
        State.ofSetAdapter(
            ParamsAdapter.of(
                /* itemClick= */ this,
                getHomeTilesList(/* isTagConnected= */ false, /* isAttached= */ false))));
  }

  /** Returns the current tag name. */
  public Signal<String> getTagName() {
    return connectedJacquardTagSignal.distinctUntilChanged().switchMap(tag -> tag.getCustomAdvName());
  }

  /**
   * Connects to the selected tag, It should be called once, when fragment is created.
   */
  public void connect(Context context,
      final @NonNull Fn<IntentSender, Signal<Boolean>> senderHandler) {
    PrintLogger.d(TAG, "Home connect: " + preferences.getCurrentTag().address());
    connectivityManager.connect(context, preferences.getCurrentTag().address(), senderHandler)
        .delay(
            1000) // Added delay to accommodate some time for fragment to subscribe to stateSignal.
        .onError(t -> {
          onErrorState(t.getMessage());
        });
  }

  /**
   * Returns the BatteryStatus signal of current selected tag.
   */
  public Signal<BatteryStatus> getBatteryStatus() {
    return connectedJacquardTagSignal.distinctUntilChanged()
        .flatMap(tag -> tag.enqueue(new BatteryStatusCommand()));
  }

  /**
   * Emits connectivity events {@link Events}.
   */
  public Signal<Events> getConnectivityEvents() {
    return connectivityManager.getEventsSignal(preferences.getCurrentTag().address())
        .distinctUntilChanged();
  }

  public Signal<FirmwareUpdateState> getState() {
    if (preferences.getCurrentTag() == null) {
      return Signal.empty();
    }
    return firmwareManager.getState(preferences.getCurrentTag().address());
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

  /**
   * Navigates to {@link LedPatternFragment}.
   */
  private void ledPatternClick() {
    navController.navigate(R.id.action_homeFragment_to_ledPatternFragment);
  }

  /**
   * Navigates to {@link MusicalThreadsFragment}.
   */
  private void musicalThreadsButtonClick() {
    navController.navigate(R.id.action_homeFragment_to_musicalThreadsFragment);
  }

  /**
   * Navigates to {@link GestureFragment}.
   */
  private void gestureButtonClick() {
    navController.navigate(R.id.action_homeFragment_to_gestureFragment);
  }

  /**
   * Navigates to {@link HapticsFragment}.
   */
  private void hapticsClick() {
    navController.navigate(R.id.action_homeFragment_to_hapticsFragment);
  }

  /**
   * Navigates to {@link TagManagerFragment}.
   */
  private void tagManagerClick() {
    if(preferences.isDfuInProgress()) {
      onErrorState(resources.getString(R.string.feature_will_not_work));
      return;
    }
    navController.navigate(R.id.action_homeFragment_to_tagManagerFragment);
  }

  /**
   * Navigates to {@link TagManagerFragment}.
   */
  private void capVisualiserClick() {
    navController.navigate(R.id.action_homeFragment_to_touchYourThreadsFragment);
  }

  /**
   * Navigates to {@link RenameTagFragment}.
   */
  private void renameClick() {
    if(preferences.isDfuInProgress()) {
      onErrorState(resources.getString(R.string.feature_will_not_work));
      return;
    }
    navController.navigate(R.id.action_homeFragment_to_renameTagFragment);
  }

  /**
   * Navigates to {@link FirmwareUpdateFragment}.
   */
  private void firmwareUpdateClick() {
    navController.navigate(R.id.action_homeFragment_to_firmwareUpdateFragment);
  }

  /**
   * Navigates to {@link PlacesConfigFragment}.
   */
  private void placesConfigClick() {
    Recipe recipe = recipeManager.getRecipeForAbility(AbilityConstants.PLACES_ABILITY);
    if (recipe != null) {
      PrintLogger.d(TAG, "Places ability is already assigned.");
      navController.navigate(
          HomeFragmentDirections.actionHomeFragmentToPlacesListFragment(recipe.gestureId()));
    } else {
      navController.navigate(R.id.action_homeFragment_to_placesConfigFragment);
    }
  }

  private void onTileClick(int tileTitleResourceId) {
    switch (tileTitleResourceId) {
      case R.string.tile_leds_title: {
        ledPatternClick();
        break;
      }
      case R.string.tile_musical_threads_title: {
        musicalThreadsButtonClick();
        break;
      }
      case R.string.tile_gesture_title: {
        gestureButtonClick();
        break;
      }
      case R.string.tile_haptics_title: {
        hapticsClick();
        break;
      }
      case R.string.tile_tagmanager_title: {
        tagManagerClick();
        break;
      }
      case R.string.tile_cap_visualizer_title: {
        capVisualiserClick();
        break;
      }
      case R.string.tile_rename_tag_title: {
        renameClick();
        break;
      }
      case R.string.tile_firmware_updates_title: {
        firmwareUpdateClick();
        break;
      }
      case R.string.places_title: {
        placesConfigClick();
        break;
      }
      case R.string.imu_tile_title: {
        if (preferences.isDfuInProgress()) {
          onErrorState(resources.getString(R.string.feature_will_not_work));
          return;
        }
        navController.navigate(R.id.action_homeFragment_to_imuFragment);
        break;
      }
      case R.string.imu_rtstream_title: {
        if (preferences.isDfuInProgress()) {
          onErrorState(resources.getString(R.string.feature_will_not_work));
          return;
        }
        navController.navigate(R.id.action_homeFragment_to_imuStreamingFragment);
        break;
      }
    }
  }

  private Signal<Notification> getBatteryNotifications(ConnectedJacquardTag tag) {
    return tag.subscribe(new BatteryStatusNotificationSubscription()).map(Notification::ofBattery);
  }

  private Signal<Notification> getGearNotifications(ConnectedJacquardTag tag) {
    return tag.getConnectedGearSignal().map(gearState -> {
      PrintLogger.d(TAG, "## GearNotifications GearState: " + gearState);
      return Notification.ofGear(gearState);
    }).tapError(error -> PrintLogger.d(TAG, "## GearNotifications error: " + error.getMessage()));
  }

  private List<HomeTileModel> getHomeTilesList(boolean isTagConnected, boolean isAttached) {
    return Arrays.asList(
        HomeTileModel.of(
            R.string.section_api,
            /* subTitle= */ 0,
            HomeTileModel.Type.SECTION,
            /* enabled= */ true),
        HomeTileModel.of(
            R.string.tile_gesture_title,
            R.string.tile_gesture_subtitle,
            HomeTileModel.Type.TILE_API,
            /* enabled= */ isAttached),
        HomeTileModel.of(
            R.string.tile_haptics_title,
            R.string.tile_haptics_subtitle,
            HomeTileModel.Type.TILE_API,
            /* enabled= */ isAttached),
        HomeTileModel.of(
            R.string.tile_leds_title,
            R.string.tile_leds_subtitle,
            HomeTileModel.Type.TILE_API,
            /* enabled= */ isTagConnected),
        HomeTileModel.of(
            R.string.tile_cap_visualizer_title,
            R.string.tile_cap_visualizer_subtitle,
            HomeTileModel.Type.TILE_API,
            /* enabled= */ isAttached),
        HomeTileModel.of(
            R.string.tile_rename_tag_title,
            R.string.tile_rename_tag_subtitle,
            HomeTileModel.Type.TILE_API,
            /* enabled= */ isTagConnected),
        HomeTileModel.of(
            R.string.tile_tagmanager_title,
            R.string.tile_tagmanager_subtitle,
            HomeTileModel.Type.TILE_API,
            /* enabled= */ true),
        HomeTileModel.of(
            R.string.tile_firmware_updates_title,
            R.string.tile_firmware_updates_subtitle,
            HomeTileModel.Type.TILE_API,
                /* enabled= */ isTagConnected),
        HomeTileModel.of(
            R.string.imu_tile_title,
            /* subTitle= */ R.string.imu_title_subtitle,
            HomeTileModel.Type.TILE_API,
            /* enabled= */ isTagConnected),
        HomeTileModel.of(
            R.string.imu_rtstream_title,
            /* subTitle= */ R.string.imu_rtstream_title,
            HomeTileModel.Type.TILE_API,
            /* enabled= */ isTagConnected),
        HomeTileModel.of(
            R.string.section_sample_use_cases,
            /* subTitle= */ 0,
            HomeTileModel.Type.SECTION,
            /* enabled= */ true),
        HomeTileModel.of(
            R.string.tile_musical_threads_title,
            /* subTitle= */ 0,
            HomeTileModel.Type.TILE_SAMPLE_USE_CASE,
            /* enabled= */ isAttached),
        HomeTileModel.of(
            R.string.places_title,
            /* subTitle= */ 0,
            HomeTileModel.Type.TILE_SAMPLE_USE_CASE,
            /* enabled= */ isAttached));
  }

  private void subscribeConnectionState() {
    subscriptions.add(
        connectionStateSignal
            .tap(state -> PrintLogger
                .d(TAG, "HomeViewModel # subscribeConnectionState # state -> " + state))
            .distinctUntilChanged()
            .observe(
                new ObservesNext<ConnectionState>() {
                  @Override
                  public void onNext(@NonNull ConnectionState connectionState) {
                    onConnectionState(connectionState);
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    onErrorState(t.getMessage());
                  }
                }));
  }

  private void onConnectionState(ConnectionState connectionState) {
    PrintLogger.d(TAG, "onConnectionState: " + connectionState);
    if (connectionState.isType(CONNECTED)) {
      stateSignal.next(State.ofConnected(connectionState.connected()));
    } else if (connectionState.isType(PREPARING_TO_CONNECT)
        || connectionState.isType(DISCONNECTED)) {
      stateSignal.next(State.ofDisconnected());
    }
  }

  private void onErrorState(String message) {
    PrintLogger.d(TAG, /* message= */"Failed to connect : " + message);
    stateSignal.next(State.ofError(message));
  }
}
