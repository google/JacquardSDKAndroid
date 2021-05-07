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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.location.LocationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.jacquard.sample.BuildConfig;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.ViewModelFactory;
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogBuilder;
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogButtonClickListener;
import com.google.android.jacquard.sample.places.PlacesConfigViewModel.State;
import com.google.android.jacquard.sample.utilities.Util;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for places ability configuration.
 */
public class PlacesConfigFragment extends Fragment {

  private static final String TAG = PlacesConfigFragment.class.getSimpleName();
  private PlacesConfigViewModel viewModel;
  private final List<Subscription> subscriptions = new ArrayList<>();
  private final ActivityResultLauncher<Intent> enableLocationLauncher = registerForActivityResult(
      new StartActivityForResult(),
      result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
          if (canAssignGesture()) {
            viewModel.assignAbility();
          }
        }
      });
  private final ActivityResultLauncher<String> requestPermissionLauncher =
      registerForActivityResult(
          new RequestPermission(),
          isGranted -> {
            if (isGranted) {
              if (canAssignGesture()) {
                viewModel.assignAbility();
              }
            } else {
              shouldShowRequestPermissionRationale();
            }
          });

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel =
        new ViewModelProvider(
            requireActivity(),
            new ViewModelFactory(requireActivity().getApplication(), getNavController()))
            .get(PlacesConfigViewModel.class);
    subscriptions.add(viewModel.stateSignal.onNext(this::onState));
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_places_config, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initToolbar(view);
    initView(view);
  }

  @Override
  public void onDestroy() {
    unsubscribe();
    super.onDestroy();
  }

  private void initView(View view) {
    RecyclerView recyclerView = view.findViewById(R.id.recyclerview);
    Button assignButton = view.findViewById(R.id.assignButton);
    assignButton.setOnClickListener(viewModel);
    GestureAdapter gestureAdapter = new GestureAdapter(viewModel.getGestures(), viewModel);
    recyclerView.setAdapter(gestureAdapter);
  }

  private void initToolbar(View root) {
    Toolbar toolbar = root.findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> viewModel.backArrowClick());
  }

  private void onState(State state) {
    switch (state.getType()) {
      case ASSIGNED:
        PrintLogger.d(TAG, "onState() :: assign state.");
        if(TextUtils.isEmpty(BuildConfig.MAPS_API_KEY)){
          showApiKeyMissingDialog();
          return;
        }
        if (canAssignGesture()) {
          PrintLogger.d(TAG, "onState() :: Gesture is assigned.");
          viewModel.assignAbility();
        }
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

  private void showApiKeyMissingDialog() {
    new DefaultDialogBuilder()
        .setTitle(R.string.key_required)
        .setSubtitle(R.string.key_required_desc)
        .setPositiveButtonTitleId(R.string.ok)
        .setShowNegativeButton(false)
        .setShowPositiveButton(true)
        .setCancellable(false)
        .setShowSubtitle(true)
        .setShowProgress(false)
        .build().show(getParentFragmentManager(), /* tag= */null);
  }

  private void showMandatoryDialog() {
    new DefaultDialogBuilder()
        .setTitle(R.string.places_permission_dialog_title)
        .setSubtitle(R.string.places_permission_dialog_subtitle)
        .setPositiveButtonTitleId(R.string.places_permission_dialog_positive_btn)
        .setNegativeButtonTitle(R.string.places_permission_dialog_negative_btn)
        .setShowNegativeButton(true)
        .setShowPositiveButton(true)
        .setCancellable(false)
        .setShowSubtitle(true)
        .setShowProgress(false)
        .setPositiveButtonClick(new DefaultDialogButtonClickListener() {
          @Override
          public void buttonClick() {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
          }
        }).build().show(getParentFragmentManager(), /* tag= */null);
  }

  private boolean canAssignGesture() {
    if (!hasPermissions()) {
      return false;
    }
    return hasLocationEnabled();
  }

  private boolean hasPermissions() {
    if (checkSelfPermission(requireActivity(), ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
      return true;
    } else {
      requestPermissionLauncher.launch(ACCESS_FINE_LOCATION);
      return false;
    }
  }

  private void shouldShowRequestPermissionRationale() {
    if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
      return;
    }
    PrintLogger.d(TAG, "need to show permission rational custom dialog.");
    showMandatoryDialog();
  }

  private boolean hasLocationEnabled() {
    LocationManager locationManager = (LocationManager) requireContext()
        .getSystemService(Context.LOCATION_SERVICE);
    if (LocationManagerCompat.isLocationEnabled(locationManager)) {
      return true;
    }
    Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    enableLocationLauncher.launch(enableLocationIntent);
    return false;
  }

  private NavController getNavController() {
    return NavHostFragment.findNavController(this);
  }
}
