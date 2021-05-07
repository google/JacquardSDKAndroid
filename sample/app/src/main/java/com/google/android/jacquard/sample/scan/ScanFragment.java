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

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.location.LocationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.jacquard.sample.KnownTag;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.ViewModelFactory;
import com.google.android.jacquard.sample.scan.ScanAdapter.ItemClickListener;
import com.google.android.jacquard.sample.scan.ScanViewModel.State;
import com.google.android.jacquard.sample.utilities.Util;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

/**
 * Fragment for scanning for jacquard tags.
 */
public class ScanFragment extends Fragment implements ItemClickListener {

  private final List<Subscription> subscriptions = new ArrayList<>();
  private ScanAdapter adapter;
  private ScanViewModel viewModel;
  private Subscription scanSubscription;
  private Button scanButton, stopScanButton, pairButton;
  private View tagIndicatorIcon;
  private View progressBarHolder, progressBar, pairedIcon;
  private TextView loadingTitle;
  private TextView description;
  private final ActivityResultLauncher<Intent> enableLocationLauncher = registerForActivityResult(
      new StartActivityForResult(),
      result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
          if (canStartScanning()) {
            startScan();
          }
        }
      });
  private final ActivityResultLauncher<String> requestPermissionLauncher =
      registerForActivityResult(
          new RequestPermission(),
          isGranted -> {
            if (isGranted) {
              if (canStartScanning()) {
                startScan();
              }
            } else {
              onNotAbleToScan();
            }
          });
  private final ActivityResultLauncher<Intent> enableBluetoothLauncher =
      registerForActivityResult(
          new StartActivityForResult(),
          result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
              if (canStartScanning()) {
                startScan();
              }
            }
          });

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel =
        new ViewModelProvider(
            requireActivity(),
            new ViewModelFactory(requireActivity().getApplication(), getNavController()))
            .get(ScanViewModel.class);
    adapter = new ScanAdapter(this);
    subscriptions.add(viewModel.stateSignal.onNext(this::onState));
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_scan, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initView(view);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    unsubscribe();
  }

  @Override
  public void onItemClick(KnownTag tag) {
    pairButton.setVisibility(View.VISIBLE);
    scanButton.setVisibility(View.GONE);
    stopScanButton.setVisibility(View.GONE);
    viewModel.setSelected(tag);
    adapter.notifyDataSetChanged();
  }

  private void initView(View view) {
    RecyclerView recyclerView = view.findViewById(R.id.tag_recyclerview);
    recyclerView.setAdapter(adapter);
    scanButton = view.findViewById(R.id.scan_button);
    tagIndicatorIcon = view.findViewById(R.id.tag_indicator_icon);
    description = view.findViewById(R.id.description);
    scanButton.setOnClickListener(
        v -> {
          if (canStartScanning()) {
            startScan();
          }
        });
    stopScanButton = view.findViewById(R.id.stop_scan_button);
    stopScanButton.setOnClickListener(v -> stopScan());
    pairButton = view.findViewById(R.id.pair_button);
    pairButton.setOnClickListener(v -> viewModel.connect());
    progressBarHolder = view.findViewById(R.id.progress_bar_holder);
    progressBar = view.findViewById(R.id.progress_bar);
    pairedIcon = view.findViewById(R.id.paired_icon);
    loadingTitle = view.findViewById(R.id.loading_title);
  }

  private void startScan() {
    initScanningViews();
    scanSubscription = viewModel
        .startScanning()
        .onNext(tags -> {
          Timber.d("Got tags %d", tags.size());
          adapter.submitList(tags);
        });
    subscriptions.add(scanSubscription);
  }

  private void initScanningViews() {
    scanButton.setText(R.string.scan_page_scanning_button);
    scanButton.setEnabled(false);
    stopScanButton.setVisibility(View.VISIBLE);
    tagIndicatorIcon.setVisibility(View.GONE);
    description.setText(R.string.scan_page_match_last_four_digits_desc);
  }

  private void stopScan() {
    scanButton.setText(R.string.scan_page_scan_button);
    scanButton.setEnabled(true);
    stopScanButton.setVisibility(View.GONE);
    tagIndicatorIcon.setVisibility(View.VISIBLE);
    description.setText(R.string.scan_page_charge_your_tag_desc);
    if (scanSubscription != null) {
      scanSubscription.unsubscribe();
      subscriptions.remove(scanSubscription);
      scanSubscription = null;
    }
  }

  private void onConnectionState() {
    Timber.d("Tag connected");
    progressBar.setVisibility(View.GONE);
    pairedIcon.setVisibility(View.VISIBLE);
    loadingTitle.setText(R.string.scan_page_paired_button);
    viewModel.successfullyConnected(isUserOnboarded());
  }

  private void onState(State state) {
    switch (state.getType()) {
      case CONNECTING:
        progressBarHolder.setVisibility(View.VISIBLE);
        break;
      case CONNECTED:
        onConnectionState();
        break;
      case ERROR:
        Util.showSnackBar(getView(), state.error());
        progressBarHolder.setVisibility(View.GONE);
        break;
    }
  }

  private NavController getNavController() {
    return NavHostFragment.findNavController(this);
  }

  private void unsubscribe() {
    for (Subscription subscription : subscriptions) {
      subscription.unsubscribe();
    }
    subscriptions.clear();
  }

  private boolean canStartScanning() {
    if (!hasBluetoothEnabled()) {
      return false;
    }
    if (!hasPermissions()) {
      return false;
    }
    return hasLocationEnabled();
  }

  private boolean hasPermissions() {
    if (checkSelfPermission(requireActivity(), ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
      return true;
    } else if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
      // User has denied the permission. Its time to show rationale.
      showRationale();
      return false;
    } else {
      requestPermissionLauncher.launch(ACCESS_FINE_LOCATION);
      return false;
    }
  }

  private boolean hasBluetoothEnabled() {
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (bluetoothAdapter.isEnabled()) {
      return true;
    }
    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    enableBluetoothLauncher.launch(enableBtIntent);
    return false;
  }

  private boolean hasLocationEnabled() {
    LocationManager locationManager = (LocationManager) getContext()
        .getSystemService(Context.LOCATION_SERVICE);
    if (LocationManagerCompat.isLocationEnabled(locationManager)) {
      return true;
    }
    Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    enableLocationLauncher.launch(enableLocationIntent);
    return false;
  }

  private void onNotAbleToScan() {
    // TODO Show error..
    Util.showSnackBar(getView(),
        "Please grant location permission for scanning bluetooth devices.");
    Timber.d("Not able to start ble scan.");
  }

  private void showRationale() {
    // TODO Show rationale
    Util.showSnackBar(getView(),
        "Please grant location permission for scanning bluetooth devices.");
    Timber.d("Location permission is denied. Show Primer dialog.");
  }

  private boolean isUserOnboarded() {
    return ScanFragmentArgs.fromBundle(getArguments()).getIsUserOnboarded();
  }
}
