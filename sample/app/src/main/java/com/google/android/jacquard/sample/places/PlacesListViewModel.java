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
 *
 */

package com.google.android.jacquard.sample.places;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.jacquard.sample.ConnectivityManager;
import com.google.android.jacquard.sample.Preferences;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.places.PlacesItem.Type;
import com.google.android.jacquard.sample.places.PlacesListAdapter.ItemClickListener;
import com.google.android.jacquard.sample.places.db.PlaceItem;
import com.google.android.jacquard.sample.storage.PlacesRepository;
import com.google.android.jacquard.sample.utilities.DateUtil;
import com.google.android.jacquard.sdk.command.GestureNotificationSubscription;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Gesture;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.auto.value.AutoOneOf;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fragment for Places list screen.
 */
public class PlacesListViewModel extends ViewModel implements ItemClickListener {

  private static final String TAG = PlacesListViewModel.class.getSimpleName();
  /**
   * The desired interval for location updates. Inexact. Updates may be more or less frequent.
   */
  private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
  /**
   * The fastest rate for active location updates. Updates will never be more frequent than this
   * value.
   */
  private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
      UPDATE_INTERVAL_IN_MILLISECONDS / 2;
  public final Signal<State> stateSignal = Signal.create();
  private final ConnectivityManager connectivityManager;
  private final NavController navController;
  private final Geocoder geocoder;
  private final PlacesRepository repository;
  private final Preferences preferences;
  private final List<Subscription> subscriptions = new ArrayList<>();
  private LiveData<List<PlaceItem>> listLiveData;
  private Observer<List<PlaceItem>> observer;
  private int savedLocations = 0;
  private PlacesItem selectedPlaceItem;

  public PlacesListViewModel(
      ConnectivityManager connectivityManager,
      NavController navController,
      Geocoder geocoder,
      PlacesRepository repository, Preferences preferences) {
    this.connectivityManager = connectivityManager;
    this.navController = navController;
    this.geocoder = geocoder;
    this.repository = repository;
    this.preferences = preferences;
  }


  /**
   * Handles the back arrow click in the toolbar.
   */
  public void backArrowClick() {
    navController.popBackStack();
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    for (Subscription subscription : subscriptions) {
      subscription.unsubscribe();
    }
    subscriptions.clear();
    listLiveData.removeObserver(observer);
  }

  @Override
  public void onMoreOptionClick(PlacesItem placesItem) {
    PrintLogger.d(TAG, "onMoreOptionClick() :: more options clicked.");
    this.selectedPlaceItem = placesItem;
    stateSignal.next(State.ofMoreOptions());
  }

  @Override
  public void onMapClick(PlacesItem placeItem) {
    PrintLogger.d(TAG, "onMapClick() :: map item clicked.");
    navController
        .navigate(PlacesListFragmentDirections.actionPlacesListFragmentToPlacesDetailsFragment(
            placeItem.time()));
  }

  /**
   * Listens to gesture to trigger places ability.
   */
  public Subscription listenToGesture(int gestureAssigned) {
    return connectivityManager.getConnectedJacquardTag(preferences.getCurrentTag().address())
        .distinctUntilChanged()
        .switchMap(
            (Fn<ConnectedJacquardTag, Signal<Gesture>>)
                tag -> tag.subscribe(new GestureNotificationSubscription()))
        .onNext(gesture -> {
              PrintLogger.d(TAG,
                  "listenToGesture() :: gesture triggered : " + gesture.gestureType().getDescription()
                      + " >> " + gestureAssigned);
              if (gestureAssigned == gesture.gestureType().getId()) {
                PrintLogger.d(TAG,
                    "listenToGesture() :: gesture matched : " + gesture.gestureType().getId() + " >> "
                        + gestureAssigned);
                stateSignal.next(State.ofGestureTriggered());
              }
            }
        );
  }

  /**
   * Fetches places data from local database.
   */
  public void getPlacesFromDb(Context context) {
    listLiveData = repository.getPlaces();
    observer = placeItems -> getPlacesList(placeItems, context);
    listLiveData.observeForever(observer);
  }

  /**
   * Save this location place item to database.
   */
  public void savePlace(Context context, Location location) {
    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
    try {
      List<Address> addresses;
      addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude,
          1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
      String address = addresses.get(0).getAddressLine(
          0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
      String city = addresses.get(0).getLocality();
      // Saving to local database
      PlaceItem placeItem = new PlaceItem(location.getLatitude(), location.getLongitude(), city,
          address, new Date().getTime());
      repository.insert(placeItem);
      stateSignal.next(State.ofPlacesItem());
    } catch (IOException e) {
      PrintLogger.e(TAG, e.getMessage());
      stateSignal.next(State.ofError(context.getString(R.string.location_details_error)));
    }
  }

  /**
   * Returns number of locations saved.
   */
  public int getSavedLocations() {
    return savedLocations;
  }

  /**
   * Deletes selected record from database.
   */
  public void deleteItem() {
    repository.delete(selectedPlaceItem.id());
    stateSignal.next(State.ofDeleted());
  }

  /**
   * Deletes all places record from database.
   */
  public void deleteAllRecords() {
    repository.deleteAll();
    savedLocations = 0;
    stateSignal.next(State.ofDeleted());
  }

  /**
   * Returns location request.
   */
  public LocationRequest getLocationRequest(){
    return LocationRequest.create()
        .setInterval(UPDATE_INTERVAL_IN_MILLISECONDS)
        .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
  }

  private void getPlacesList(List<PlaceItem> placeItemsList, Context context) {
    savedLocations = placeItemsList.size();
    List<PlacesItem> placesItems = new ArrayList<>();
    Set<String> dateHeaders = new HashSet<>();
    for (PlaceItem placeItem : placeItemsList) {
      String header = DateUtil.getUserReadableString(new Date(placeItem.timestamp), context);
      PrintLogger.d(TAG, "Header >> " + header);
      if (!dateHeaders.contains(header)) {
        dateHeaders.add(header);
        PlacesItem placesItem = PlacesItem.builder().id(0).latLng(new LatLng(0, 0)).title(header)
            .subTitle("").time(0).type(Type.SECTION).build();
        placesItems.add(placesItem);
      }
      PlacesItem placesItem = PlacesItem.builder().id(placeItem.uid)
          .latLng(new LatLng(placeItem.latitude, placeItem.longitude)).title(placeItem.title)
          .subTitle(placeItem.subtitle).time(placeItem.timestamp).type(Type.PLACES_ITEM).build();
      placesItems.add(placesItem);
    }
    selectedPlaceItem = null;
    stateSignal.next(State.ofPlacesItems(placesItems));
  }

  @AutoOneOf(PlacesListViewModel.State.Type.class)
  public abstract static class State {

    public static PlacesListViewModel.State ofMoreOptions() {
      return AutoOneOf_PlacesListViewModel_State.moreOptions();
    }

    public static PlacesListViewModel.State ofError(String errorMsg) {
      return AutoOneOf_PlacesListViewModel_State.error(errorMsg);
    }

    public static PlacesListViewModel.State ofPlacesItem() {
      return AutoOneOf_PlacesListViewModel_State.placesItem();
    }

    public static PlacesListViewModel.State ofPlacesItems(List<PlacesItem> placesItems) {
      return AutoOneOf_PlacesListViewModel_State.placesItems(placesItems);
    }

    public static PlacesListViewModel.State ofGestureTriggered() {
      return AutoOneOf_PlacesListViewModel_State.gestureTriggered();
    }

    public static PlacesListViewModel.State ofDeleted() {
      return AutoOneOf_PlacesListViewModel_State.deleted();
    }

    public abstract Type getType();

    abstract void moreOptions();

    abstract void placesItem();

    abstract List<PlacesItem> placesItems();

    abstract void gestureTriggered();

    abstract String error();

    abstract void deleted();

    public enum Type {
      MORE_OPTIONS,
      ERROR,
      PLACES_ITEM,
      GESTURE_TRIGGERED,
      PLACES_ITEMS,
      DELETED
    }
  }
}
