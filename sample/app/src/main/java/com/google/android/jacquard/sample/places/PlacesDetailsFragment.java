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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.ViewModelFactory;
import com.google.android.jacquard.sample.places.PlacesDetailsViewModel.State;
import com.google.android.jacquard.sample.places.db.PlaceItem;
import com.google.android.jacquard.sample.utilities.Util;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Fragment for places details screen with map.
 */
public class PlacesDetailsFragment extends Fragment implements OnMapReadyCallback {

  private static final String TAG = PlacesDetailsFragment.class.getSimpleName();
  private final List<Subscription> subscriptions = new ArrayList<>();
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("hh:mm aa",
      Locale.getDefault());
  private PlacesDetailsViewModel viewModel;
  private Toolbar toolbar;
  private GoogleMap map;
  private MapView mapView;
  private long selectedPlaceTimeStamp = 0;
  private List<PlaceItem> placeItems = new ArrayList<>();

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel =
        new ViewModelProvider(
            requireActivity(),
            new ViewModelFactory(requireActivity().getApplication(), getNavController()))
            .get(PlacesDetailsViewModel.class);
    subscriptions.add(viewModel.stateSignal.onNext(this::onState));
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_places_details, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initToolbar(view);
    initView(view);
    selectedPlaceTimeStamp = PlacesDetailsFragmentArgs.fromBundle(getArguments())
        .getSelectedPlace();
    viewModel
        .getPlacesFromDb(selectedPlaceTimeStamp);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  @Override
  public void onMapReady(@NonNull GoogleMap googleMap) {
    map = googleMap;
    addMarkers(placeItems);
  }

  private void addMarkers(List<PlaceItem> placeItems) {
    if (map == null) {
      PrintLogger.d(TAG, "adding markers, map is not ready yet.");
      return;
    }
    for (PlaceItem placeItem : placeItems) {
      LatLng latLng = new LatLng(placeItem.latitude, placeItem.longitude);
      MarkerOptions markerOptions = new MarkerOptions()
          .position(latLng)
          .title(DATE_FORMAT.format(new Date(placeItem.timestamp)));
      if (selectedPlaceTimeStamp == placeItem.timestamp) {
        PrintLogger.d(TAG, "addMarkers() keep default marker.");
        toolbar.setTitle(placeItem.title);
        CameraPosition cameraPosition = new CameraPosition.Builder()
            .target(latLng)
            .zoom(19).build();
        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        Marker marker = map.addMarker(markerOptions);
        Objects.requireNonNull(marker).showInfoWindow();
      } else {
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        map.addMarker(markerOptions);
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  public void onDestroy() {
    unsubscribe();
    mapView.onDestroy();
    super.onDestroy();
  }

  private void initView(View view) {
    mapView = view.findViewById(R.id.map);
  }

  private void initToolbar(View root) {
    toolbar = root.findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> viewModel.backArrowClick());
  }

  private void onState(State state) {
    switch (state.getType()) {
      case PLACES:
        PrintLogger.d(TAG, "onState() :: assign state.");
        this.placeItems = state.places();
        addMarkers(placeItems);
        break;
      case ERROR:
        Util.showSnackBar(getView(), state.error());
        break;
    }
  }

  private void unsubscribe() {
    for (Subscription subscription : subscriptions) {
      subscription.unsubscribe();
    }
    subscriptions.clear();
  }

  private NavController getNavController() {
    return NavHostFragment.findNavController(this);
  }
}
