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

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
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
  private static final int DEFAULT_LED_DURATION = 5 * 1000; // 5 sec
  private static final int MIN_DURATION = 1;
  private static final int MAX_DURATION = Integer.MAX_VALUE / 1000; // in seconds
  private final List<Signal.Subscription> subscriptions = new ArrayList<>();
  private LedPatternViewModel viewModel;
  private SwitchCompat gearSwitch, tagSwitch;
  private ImageView imgTag, imgGarment;
  private TextView txtTag, txtGarment;
  private EditText editDuration;
  private boolean playLedOnGarment, playLedOnTag;
  private boolean isTagLedClickedWithoutUser, isGearLedClickedWithoutUser;

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
    editDuration = view.findViewById(R.id.led_duration);
    editDuration.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        String text = v.getText().toString();
        if (!TextUtils.isEmpty(text)) {
          int duration = Integer.parseInt(text);
          if (duration < MIN_DURATION) {
            showSnackbar(getString(R.string.led_duration_min_error));
            setDefault();
          } else if (duration > MAX_DURATION) {
            showSnackbar(getString(R.string.led_duration_max_error, MAX_DURATION));
            setDefault();
          }
        }
      }
      return false;
    });
    imgGarment = view.findViewById(R.id.garmentImg);
    imgTag = view.findViewById(R.id.tagImg);
    gearSwitch.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          playLedOnGarment = isChecked;
          if (isGearLedClickedWithoutUser) {
            isGearLedClickedWithoutUser = false;
            return;
          }
          viewModel.persistGearLedState(isChecked);
        });
    tagSwitch.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          playLedOnTag = isChecked;
          if (isTagLedClickedWithoutUser) {
            isTagLedClickedWithoutUser = false;
            return;
          }
          viewModel.persistTagLedState(isChecked);
        });
    gearSwitch.setChecked(viewModel.isGearLedActive());
    updateSwitchState();
    subscriptions.add(viewModel.getGearNotification().distinctUntilChanged().onNext(this::updateGearState));
    subscriptions.add(viewModel.getConnectivityEvents()
        .onNext(this::updateTagLedSwitch));
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
    int ledDuration = DEFAULT_LED_DURATION;
    String durationText =
        editDuration.getText().toString().trim();
    if (!TextUtils.isEmpty(durationText)) {
      int duration = Integer.parseInt(
          editDuration.getText().toString());
      if (duration >= MIN_DURATION && duration <= MAX_DURATION) {
        ledDuration = duration * 1000;
      } else {
        setDefault();
      }
    } else {
      PrintLogger.e(TAG, "Using default led duration.");
    }
    if (playLedOnGarment) {
      subscriptions.add(viewModel.playLEDCommandOnGear(patternItem, ledDuration)
          .onError(error -> {
            PrintLogger.e(TAG, error.getMessage());
            showSnackbar(error.getMessage());
          }));
    }

    if (playLedOnTag) {
      subscriptions.add(viewModel.playLEDCommandOnUJT(patternItem, ledDuration)
          .onError(error -> {
            PrintLogger.e(TAG, error.getMessage());
            showSnackbar(error.getMessage());
          }));
    }
  }

  @Override
  public void onPause() {
    Util.hideSoftKeyboard(requireActivity());
    super.onPause();
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
    if (enabled) {
      tagSwitch.setChecked(viewModel.isTagLedActive());
    } else {
      isTagLedClickedWithoutUser = true;
      tagSwitch.setChecked(false);
    }
    tagSwitch.setEnabled(enabled);
    imgTag.setEnabled(enabled);
    txtTag.setEnabled(enabled);
  }

  private void updateTagLedSwitch(Events events) {
    switch (events) {
      case TAG_CONNECTED:
        updateTagLedSwitch(true);
        break;
      case TAG_DISCONNECTED:
        updateTagLedSwitch(false);
        break;
    }
  }

  private void updateGarmentLedSwitch(boolean enabled) {
    if (enabled) {
      gearSwitch.setChecked(viewModel.isGearLedActive());
    } else {
      isGearLedClickedWithoutUser = true;
      gearSwitch.setChecked(false);
    }
    imgGarment.setEnabled(enabled);
    txtGarment.setEnabled(enabled);
    gearSwitch.setEnabled(enabled);
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

  private void updateSwitchState() {
    playLedOnTag = viewModel.isTagLedActive();
    tagSwitch.setChecked(playLedOnTag);
  }

  private void setDefault() {
    editDuration.setText("5");
  }
}
