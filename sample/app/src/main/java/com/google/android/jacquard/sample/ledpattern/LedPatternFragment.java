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

import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.CONNECTED;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.jacquard.sample.ConnectivityManager.Events;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.ViewModelFactory;
import com.google.android.jacquard.sample.ledpattern.LedPatternAdapter.ItemClickListener;
import com.google.android.jacquard.sample.utilities.Util;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.GearState;
import com.google.android.jacquard.sdk.rx.Signal;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for LED patterns screen.
 */
public class LedPatternFragment extends Fragment implements ItemClickListener {

  private static final String TAG = LedPatternFragment.class.getSimpleName();
  private final List<Signal.Subscription> subscriptions = new ArrayList<>();
  private LedPatternViewModel viewModel;
  private SwitchCompat gearSwitch, tagSwitch;
  private ImageView imgTag, imgGarment;
  private TextView txtTag, txtGarment;
  private boolean playLedOnGarment, playLedOnTag;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_led_pattern, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel =
        new ViewModelProvider(
            requireActivity(),
            new ViewModelFactory(requireActivity().getApplication(), getNavController()))
            .get(LedPatternViewModel.class);
    initToolbar(view);
    gearSwitch = view.findViewById(R.id.gearSwitch);
    tagSwitch = view.findViewById(R.id.tagSwitch);
    txtGarment = view.findViewById(R.id.garmentTxt);
    txtTag = view.findViewById(R.id.tagTxt);
    imgGarment = view.findViewById(R.id.garmentImg);
    imgTag = view.findViewById(R.id.tagImg);
    gearSwitch.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          playLedOnGarment = isChecked;
        });
    tagSwitch.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          playLedOnTag = isChecked;
        });
    gearSwitch.setChecked(false);
    subscriptions.add(viewModel.getGearNotification().onNext(this::updateGearState));
    subscriptions.add(viewModel.getConnectionStateSignal()
        .onNext(connectionState -> updateTagLedSwitch(connectionState.isType(CONNECTED))));
    initLedPatternList(view);
    subscribeEvents();
  }

  @Override
  public void onDestroyView() {
    unSubscribeSubscription();
    super.onDestroyView();
  }

  @Override
  public void onItemClick(LedPatternItem patternItem) {

    if (playLedOnGarment) {
      subscriptions.add(viewModel.playLEDCommandOnGear(patternItem)
          .onError(error -> {
            PrintLogger.e(TAG, error.getMessage());
            showSnackbar(error.getMessage());
          }));
    }

    if (playLedOnTag) {
      subscriptions.add(viewModel.playLEDCommandOnUJT(patternItem)
          .onError(error -> {
            PrintLogger.e(TAG, error.getMessage());
            showSnackbar(error.getMessage());
          }));
    }
  }

  private void unSubscribeSubscription() {
    for (Signal.Subscription subscription : subscriptions) {
      subscription.unsubscribe();
    }
    subscriptions.clear();
  }

  private void initToolbar(View root) {
    Toolbar toolbar = root.findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> {
      viewModel.backArrowClick();
    });
  }

  private void updateGearState(GearState state) {
    PrintLogger.d(TAG, String.format("Gear State >> %s", state));
    if (state.getType() == GearState.Type.DETACHED) {
      onDetached();
      return;
    }
    onAttach(state);
  }

  private void onDetached() {
    gearSwitch.setChecked(false);
    showSnackbar(getString(R.string.gear_detached));
  }

  private void onAttach(GearState state) {
    boolean isGearLedSupported = viewModel.checkGearCapability(state);
    updateGarmentLedSwitch(isGearLedSupported);
  }

  private void updateTagLedSwitch(boolean enabled) {
    tagSwitch.setEnabled(enabled);
    imgTag.setEnabled(enabled);
    txtTag.setEnabled(enabled);
    tagSwitch.setChecked(enabled);
  }

  private void updateGarmentLedSwitch(boolean enabled) {
    gearSwitch.setEnabled(enabled);
    imgGarment.setEnabled(enabled);
    txtGarment.setEnabled(enabled);
    gearSwitch.setChecked(enabled);
  }

  private void initLedPatternList(View view) {
    RecyclerView recyclerView = view.findViewById(R.id.recyclerviewPattern);
    recyclerView.setAdapter(new LedPatternAdapter(viewModel.getLedPatterns(), this::onItemClick));
  }

  private NavController getNavController() {
    return NavHostFragment.findNavController(this);
  }

  private void showSnackbar(String message) {
    Util.showSnackBar(getView(), message);
  }

  private void subscribeEvents() {
    subscriptions.add(viewModel.getConnectivityEvents().onNext(this::onEvents));
  }

  private void onEvents(Events events) {
    switch (events) {
      case TAG_DISCONNECTED:
        showSnackbar(getString(R.string.tag_not_connected));
        break;
      case TAG_DETACHED:
        showSnackbar(getString(R.string.gear_detached));
        break;
    }
  }
}
