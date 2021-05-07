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

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.location.LocationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.ViewModelFactory;
import com.google.android.jacquard.sample.dialog.BottomSheetDialogFragment.BottomSheetMenuListener;
import com.google.android.jacquard.sample.dialog.BottomSheetItem;
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogButtonClickListener;
import com.google.android.jacquard.sample.dialog.DialogUtils;
import com.google.android.jacquard.sample.places.PlacesListViewModel.State;
import com.google.android.jacquard.sample.utilities.Util;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Gesture.GestureType;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for showing places list.
 */
public class PlacesListFragment extends Fragment {

  private static final String TAG = PlacesListFragment.class.getSimpleName();
  private static final int CANCEL = 0, DELETE = 1;
  private static final int DELETE_ALL = 2, CHANGE_GESTURE = 3;
  private static final long PLACES_TIME_OUT = 10 * 1000;
  private static final Handler handler = new Handler();
  private final List<Subscription> subscriptions = new ArrayList<>();
  private final List<PlacesItem> placesItems = new ArrayList<>();
  private TextView locationsTxt;
  private LinearLayout progressBar;
  private PlacesListViewModel viewModel;
  private PlacesListAdapter placesListAdapter;
  private FusedLocationProviderClient fusedLocationClient;
  private boolean isGestureTriggered;
  private final LocationCallback locationCallback = new LocationCallback() {
    @Override
    public void onLocationResult(@NonNull LocationResult locationResult) {
      super.onLocationResult(locationResult);
      PrintLogger.d(TAG, "onLocationResult() : got latest location.");
      progressBar.setVisibility(View.GONE);
      if (isGestureTriggered) {
        PrintLogger.d(TAG, "onLocationResult() : saving location.");
        viewModel.savePlace(requireContext(), locationResult.getLastLocation());
        isGestureTriggered = false;
        handler.removeCallbacks(locationTimeOut);
      }
    }
  };

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel =
        new ViewModelProvider(
            requireActivity(),
            new ViewModelFactory(requireActivity().getApplication(), getNavController()))
            .get(PlacesListViewModel.class);
    subscriptions.add(viewModel
        .listenToGesture(PlacesListFragmentArgs.fromBundle(getArguments()).getGestureAssigned()));
    subscriptions.add(viewModel.stateSignal.onNext(this::onState));
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_places_list, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initToolbar(view);
    initView(view);
    viewModel.getPlacesFromDb(requireContext());
  }

  @Override
  public void onDestroy() {
    unsubscribe();
    handler.removeCallbacks(locationTimeOut);
    super.onDestroy();
  }

  @Override
  public void onResume() {
    super.onResume();
    startLocationUpdates();
    requireContext().registerReceiver(locationReceiver,
        new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
  }

  @Override
  public void onPause() {
    super.onPause();
    stopLocationUpdates();
    requireContext().unregisterReceiver(locationReceiver);
  }

  BroadcastReceiver locationReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {
        startLocationUpdates();
      }
    }
  };

  private void initView(View view) {
    locationsTxt = view.findViewById(R.id.subTitleTxt);
    progressBar = view.findViewById(R.id.progress);
    RecyclerView recyclerView = view.findViewById(R.id.recyclerview);
    placesListAdapter = new PlacesListAdapter(placesItems, viewModel);
    recyclerView.addItemDecoration(
        new DividerItemDecoration(requireContext(), LinearLayout.VERTICAL));
    recyclerView.setAdapter(placesListAdapter);
    recyclerView.setRecyclerListener(recyclerListener);
  }

  private void initToolbar(View root) {
    Toolbar toolbar = root.findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> viewModel.backArrowClick());
    ImageView moreOptions = root.findViewById(R.id.more_options);
    moreOptions.setOnClickListener(v -> showOptionsMenuDialog());
  }

  private void showOptionsMenuDialog() {
    DialogUtils.showBottomOptions(new ArrayList<BottomSheetItem>() {
      {
        add(new BottomSheetItem(CHANGE_GESTURE, getString(R.string.change_gesture),
            optionsMenuBottomSheetListener));
        if (viewModel.getSavedLocations() > 0) {
          add(new BottomSheetItem(DELETE_ALL, getString(R.string.delete_all),
              optionsMenuBottomSheetListener));
        }
        add(new BottomSheetItem(CANCEL, getString(R.string.cancel),
            optionsMenuBottomSheetListener));
      }
    }, requireActivity());
  }

  private void updateLocationsText(int numberOfLocations) {
    if (numberOfLocations > 0) {
      locationsTxt.setText(getString(R.string.locations, numberOfLocations));
    } else {
      GestureType assignedGesture = GestureType
          .from(PlacesListFragmentArgs.fromBundle(getArguments()).getGestureAssigned());
      locationsTxt
          .setText(getString(R.string.use_gesture, assignedGesture.getDescription().toLowerCase()));
    }
  }

  private void onState(State state) {
    switch (state.getType()) {
      case MORE_OPTIONS:
        PrintLogger.d(TAG, "onState() :: more options for item.");
        DialogUtils.showBottomOptions(new ArrayList<BottomSheetItem>() {
          {
            add(new BottomSheetItem(DELETE, getString(R.string.delete),
                listItemBottomSheetListener));
            add(new BottomSheetItem(CANCEL, getString(R.string.cancel),
                listItemBottomSheetListener));
          }
        }, requireActivity());
        break;
      case PLACES_ITEM:
        PrintLogger.d(TAG, "onState() :: places item saved state.");
        break;
      case PLACES_ITEMS:
        PrintLogger.d(TAG, "onState() :: places list state.");
        placesItems.clear();
        placesItems.addAll(state.placesItems());
        placesListAdapter.notifyDataSetChanged();
        updateLocationsText(viewModel.getSavedLocations());
        break;
      case GESTURE_TRIGGERED:
        PrintLogger.d(TAG, "onState() :: gesture triggered state.");
        if (checkPermissions()) {
          progressBar.setVisibility(View.VISIBLE);
          isGestureTriggered = true;
          handler.removeCallbacks(locationTimeOut);
          handler.postDelayed(locationTimeOut, PLACES_TIME_OUT);
        }
        break;
      case DELETED:
        PrintLogger.d(TAG, "onState() :: deleted state.");
        break;
      case ERROR:
        Util.showSnackBar(getView(), state.error());
        break;
    }
  }

  Runnable locationTimeOut = new Runnable() {
    @Override
    public void run() {
      PrintLogger.d(TAG, "locationTimeOut() : location not received yet.");
      isGestureTriggered = false;
      progressBar.setVisibility(View.GONE);
      Util.showSnackBar(getView(), getString(R.string.places_time_out));
    }
  };

  BottomSheetMenuListener optionsMenuBottomSheetListener = id -> {
    switch (id) {
      case DELETE_ALL:
        DialogUtils.showDialog(R.string.delete_all_location, R.string.delete_all_location_desc,
            R.string.delete, R.string.cancel, new DefaultDialogButtonClickListener() {
              @Override
              public void buttonClick() {
                viewModel.deleteAllRecords();
              }
            }, getChildFragmentManager());
        break;
      case CHANGE_GESTURE:
        PrintLogger.d(TAG, "Change assigned gesture clicked.");
        getNavController().navigate(
            PlacesListFragmentDirections.actionPlacesListFragmentToPlacesConfigFragment());
        break;
      case CANCEL:
        break;
    }
  };

  BottomSheetMenuListener listItemBottomSheetListener = id -> {
    switch (id) {
      case DELETE:
        DialogUtils.showDialog(R.string.delete_location, R.string.delete_location_desc,
            R.string.delete, R.string.cancel, new DefaultDialogButtonClickListener() {
              @Override
              public void buttonClick() {
                viewModel.deleteItem();
              }
            }, getChildFragmentManager());
        break;
      case CANCEL:
        break;
    }
  };

  /**
   * RecycleListener that completely clears the {@link com.google.android.gms.maps.GoogleMap}
   * attached to a row in the RecyclerView. Sets the map type to {@link
   * com.google.android.gms.maps.GoogleMap#MAP_TYPE_NONE} and clears the map.
   */
  private final RecyclerView.RecyclerListener recyclerListener = holder -> {
    PlacesListAdapter.AbstractViewHolder mapHolder = (PlacesListAdapter.AbstractViewHolder) holder;
    if (mapHolder.map != null) {
      // Clear the map and free up resources by changing the map type to none.
      // Also reset the map when it gets reattached to layout, so the previous map would
      // not be displayed.
      mapHolder.map.clear();
      mapHolder.map.setMapType(GoogleMap.MAP_TYPE_NONE);
    }
  };

  @SuppressLint("MissingPermission")
  private void startLocationUpdates() {
    if (!checkPermissions()) {
      PrintLogger.d(TAG, "startLocationUpdates but no permission.");
      return;
    }
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
    fusedLocationClient
        .requestLocationUpdates(viewModel.getLocationRequest(), locationCallback,
            Looper.getMainLooper());
  }

  private void stopLocationUpdates() {
    if (fusedLocationClient != null) {
      fusedLocationClient.removeLocationUpdates(locationCallback);
    }
  }

  private void unsubscribe() {
    for (Subscription subscription : subscriptions) {
      subscription.unsubscribe();
    }
    subscriptions.clear();
  }

  private boolean checkPermissions() {
    if (!hasPermissions()) {
      return false;
    }
    return hasLocationEnabled();
  }

  private boolean hasPermissions() {
    if (checkSelfPermission(requireActivity(), ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
      return true;
    } else if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
      requireAccess();
      return false;
    } else {
      requireAccess();
      return false;
    }
  }

  private boolean hasLocationEnabled() {
    LocationManager locationManager = (LocationManager) requireContext()
        .getSystemService(Context.LOCATION_SERVICE);
    if (LocationManagerCompat.isLocationEnabled(locationManager)) {
      return true;
    }
    Util.showSnackBar(getView(), getString(R.string.location_disabled));
    return false;
  }

  private void requireAccess() {
    Util.showSnackBar(getView(), getString(R.string.requires_location_access));
  }

  private NavController getNavController() {
    return NavHostFragment.findNavController(this);
  }
}
