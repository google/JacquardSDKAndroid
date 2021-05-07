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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;
import com.google.android.jacquard.sample.places.db.PlaceItem;
import com.google.android.jacquard.sample.storage.PlacesRepository;
import com.google.android.jacquard.sample.utilities.DateUtil;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.auto.value.AutoOneOf;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * View model for places details screen.
 */
public class PlacesDetailsViewModel extends ViewModel {

  private static final String TAG = PlacesDetailsViewModel.class.getSimpleName();
  public final Signal<State> stateSignal = Signal.create();
  private final NavController navController;
  private final PlacesRepository repository;
  private final List<Subscription> subscriptions = new ArrayList<>();
  private LiveData<List<PlaceItem>> listLiveData;
  private Observer<List<PlaceItem>> observer;

  public PlacesDetailsViewModel(NavController navController, PlacesRepository repository) {
    this.navController = navController;
    this.repository = repository;
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

  /**
   * Fetches places data from local database.
   */
  public void getPlacesFromDb(long selectedPlace) {
    Date selectedDate = DateUtil.getZeroDateTime(new Date(selectedPlace));
    Date dayAfter = DateUtil.incrementDateByOne(selectedDate);
    listLiveData = repository.getAllRecordsForDate(selectedDate.getTime(), dayAfter.getTime());
    observer = this::getPlacesList;
    listLiveData.observeForever(observer);
  }

  private void getPlacesList(List<PlaceItem> placeItems) {
    PrintLogger.d(TAG,"getPlacesList() :: number of records "+placeItems.size());
    stateSignal.next(State.ofPlaces(placeItems));
  }

  @AutoOneOf(PlacesDetailsViewModel.State.Type.class)
  public abstract static class State {

    public static PlacesDetailsViewModel.State ofPlaces(List<PlaceItem> placeItems) {
      return AutoOneOf_PlacesDetailsViewModel_State.places(placeItems);
    }

    public static PlacesDetailsViewModel.State ofError(String errorMsg) {
      return AutoOneOf_PlacesDetailsViewModel_State.error(errorMsg);
    }

    public abstract Type getType();

    abstract List<PlaceItem> places();

    abstract String error();

    public enum Type {
      PLACES,
      ERROR
    }
  }
}
