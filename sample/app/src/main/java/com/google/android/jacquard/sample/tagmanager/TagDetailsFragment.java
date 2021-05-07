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
package com.google.android.jacquard.sample.tagmanager;

import static com.google.android.jacquard.sample.MainActivity.COMPANION_DEVICE_REQUEST;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.jacquard.sample.KnownTag;
import com.google.android.jacquard.sample.MainActivity;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.ViewModelFactory;
import com.google.android.jacquard.sample.utilities.Util;
import com.google.android.jacquard.sdk.command.BatteryStatus;
import com.google.android.jacquard.sdk.command.BatteryStatus.ChargingState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.GearState;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for showing tag details.
 */
public class TagDetailsFragment extends Fragment {

  private static final String TAG = TagDetailsFragment.class.getSimpleName();
  private final List<Subscription> subscriptions = new ArrayList<>();

  private KnownTag knownTag;
  private TagDetailsViewModel tagDetailsViewModel;
  private TextView txtSerialNumber, txtVersion, txtBattery, txtAttached, txtTagStatus,
      txtSelectAsCurrentTag, txtForgetTag;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    knownTag = TagDetailsFragmentArgs.fromBundle(getArguments()).getKnownTag();
    tagDetailsViewModel =
        new ViewModelProvider(
            requireActivity(),
            new ViewModelFactory(requireActivity().getApplication(), getNavController()))
            .get(TagDetailsViewModel.class);
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_tag_details, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initToolbar();
    initUI(view);
    getTagDetails();
    showMessageWhenTagIsNotCurrent();
    txtSerialNumber.setText(knownTag.pairingSerialNumber());
    txtSelectAsCurrentTag.setOnClickListener(v -> {
      tagDetailsViewModel
          .selectAsCurrentTag(requireActivity(), knownTag,
              intentSender -> ((MainActivity) requireActivity())
                  .startForResult(intentSender, COMPANION_DEVICE_REQUEST)
                  .map(result -> result.resultCode() == Activity.RESULT_OK));
      showSnackBar(getString(R.string.tag_details_tag_selected, knownTag.displayName()));
    });
    txtForgetTag.setOnClickListener(v -> {
      tagDetailsViewModel
          .forgetTag(requireActivity(), knownTag, intentSender -> ((MainActivity) requireActivity())
              .startForResult(intentSender, COMPANION_DEVICE_REQUEST)
              .map(result -> result.resultCode() == Activity.RESULT_OK));
      showSnackBar(getString(R.string.tag_details_tag_removed, knownTag.displayName()));
    });
  }

  @Override
  public void onDestroyView() {
    for (Subscription subscription : subscriptions) {
      subscription.unsubscribe();
    }
    subscriptions.clear();
    super.onDestroyView();
  }

  private void initUI(View view) {
    txtSerialNumber = view.findViewById(R.id.serialNumberValueTxt);
    txtVersion = view.findViewById(R.id.versionNumberValueTxt);
    txtBattery = view.findViewById(R.id.batteryValueTxt);
    txtAttached = view.findViewById(R.id.attachStatusTxt);
    txtTagStatus = view.findViewById(R.id.tagStatusTxt);
    txtSelectAsCurrentTag = view.findViewById(R.id.currentTagTxt);
    txtForgetTag = view.findViewById(R.id.forgetTagTxt);
  }

  private void getFirmwareVersion() {
    subscriptions.add(
        tagDetailsViewModel.getVersion().observe(version -> {
          PrintLogger.d(TAG, "Received device info: " + version.toString());
          txtVersion.setText(version.toString());
        }, error -> {
          if (error == null) {
            return;
          }
          PrintLogger.e(TAG, "getFirmwareVersion: " + error.getMessage());
          showSnackBar(error.getMessage());
        }));
  }

  private void getBatteryStatus() {
    subscriptions.add(
        tagDetailsViewModel.getBatteryStatus()
            .observe(notification -> {
              PrintLogger.d(TAG, "Received battery status info: " + notification);
              onBatteryStatus(notification);
            }, error -> {
              if (error == null) {
                return;
              }
              PrintLogger.e(TAG, "getBatteryStatus: " + error.getMessage());
              showSnackBar(error.getMessage());
            }));
  }

  private void subscribeToNotifications() {
    subscriptions.add(
        tagDetailsViewModel
            .getNotification()
            .observe(notification -> {
              PrintLogger.d(TAG, "Received notification: " + notification);
              switch (notification.getType()) {
                case BATTERY:
                  onBatteryStatus(notification.battery());
                  break;
                case GEAR:
                  onGearState(notification.gear());
                  break;
              }
            }, error -> {
              if (error == null) {
                return;
              }
              PrintLogger.e(TAG, "subscribeToNotifications: " + error.getMessage());
              showSnackBar(error.getMessage());
            }));
  }

  private void onGearState(GearState gearState) {
    if (gearState.getType() == GearState.Type.ATTACHED) {
      txtAttached.setText(requireContext()
          .getString(R.string.tag_details_tag_attached, gearState.attached().product().name()));
    } else {
      txtAttached.setText("No");
    }
  }

  private void getTagDetails() {
    if (tagDetailsViewModel.checkIfCurrentTag(knownTag)) {
      txtAttached.setText("No");
      txtTagStatus.setVisibility(View.VISIBLE);
      txtSelectAsCurrentTag.setVisibility(View.GONE);
      getFirmwareVersion();
      getBatteryStatus();
      subscribeToNotifications();
    } else {
      txtSelectAsCurrentTag.setVisibility(View.VISIBLE);
      showSnackBar(getString(R.string.tag_details_tag_not_current));
    }
  }

  private void onBatteryStatus(BatteryStatus batteryStatus) {
    String chargingStateText =
        requireContext()
            .getString(
                batteryStatus.chargingState() == ChargingState.CHARGING
                    ? R.string.home_page_battery_charge_state_charging
                    : R.string.home_page_battery_charge_state_not_charging);
    txtBattery.setText(
        requireContext()
            .getString(
                R.string.tag_details_battery_info,
                String.valueOf(batteryStatus.batteryLevel()),
                chargingStateText));
    txtTagStatus.setText("Current Tag");
    txtTagStatus.setEnabled(true);
  }

  private void initToolbar() {
    Toolbar toolbar = getView().findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> {
      tagDetailsViewModel.backArrowClick();
    });
    ((TextView)getView().findViewById(R.id.toolbar_title)).setText(knownTag.displayName());
  }

  private void showSnackBar(String text) {
    Util.showSnackBar(getView(), text);
  }

  private NavController getNavController() {
    return NavHostFragment.findNavController(this);
  }

  private void showMessageWhenTagIsNotCurrent() {
    if (tagDetailsViewModel.checkIfCurrentTag(knownTag)) {
      PrintLogger.d(TAG, knownTag.identifier() + " is current tag");
      return;
    }
    Util.showSnackBar(getView(), getString(R.string.tag_details_tag_not_current));
  }
}
