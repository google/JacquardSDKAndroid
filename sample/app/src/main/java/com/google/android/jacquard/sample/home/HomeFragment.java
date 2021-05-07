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

import static com.google.android.jacquard.sample.MainActivity.COMPANION_DEVICE_REQUEST;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.jacquard.sample.ConnectivityManager.Events;
import com.google.android.jacquard.sample.MainActivity;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.ViewModelFactory;
import com.google.android.jacquard.sample.dialog.DefaultDialog;
import com.google.android.jacquard.sample.home.HomeViewModel.Notification;
import com.google.android.jacquard.sample.home.HomeViewModel.State;
import com.google.android.jacquard.sample.utilities.JacquardRelativeLayout;
import com.google.android.jacquard.sample.utilities.Util;
import com.google.android.jacquard.sdk.BuildConfig;
import com.google.android.jacquard.sdk.command.BatteryStatus;
import com.google.android.jacquard.sdk.command.BatteryStatus.ChargingState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.GearState;
import com.google.android.jacquard.sdk.rx.Signal.ObservesNext;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/** Fragment for Dashboard UI. */
public class HomeFragment extends Fragment implements JacquardRelativeLayout.OnDoubleListener {

  private static final String TAG = HomeFragment.class.getSimpleName();

  private final List<Subscription> subscriptions = new ArrayList<>();
  private final String blurProductImageName = "product_blur_image_hear";
  private RecyclerView recyclerView;
  private final GridLayoutManager.SpanSizeLookup spanSizeLookup =
      new SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
          if (HomeTileModel.Type.values()[
                  Objects.requireNonNull(recyclerView.getAdapter()).getItemViewType(position)]
              == HomeTileModel.Type.SECTION) {
            return 2;
          }
          return 1;
        }
      };
  private HomeViewModel homeViewModel;
  private TextView txtGearState;
  private TextView txtTagName;
  private TextView txtBattery;
  private TextView txtRssi;
  private ImageView imgProduct;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    homeViewModel =
        new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication(), getNavController()))
            .get(HomeViewModel.class);
    homeViewModel.connect(requireActivity(), intentSender -> ((MainActivity) requireActivity())
        .startForResult(intentSender, COMPANION_DEVICE_REQUEST)
        .map(result -> result.resultCode() == Activity.RESULT_OK));
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    subscriptions.add(homeViewModel.stateSignal.onNext(this::onNavigation));
    JacquardRelativeLayout view =
        (JacquardRelativeLayout) inflater.inflate(R.layout.fragment_home, container, false);
    view.setDoubleTouchListener(this);
    return view;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    imgProduct = view.findViewById(R.id.imgProduct);
    txtGearState = view.findViewById(R.id.txtTagState);
    txtTagName = view.findViewById(R.id.txtTagName);
    txtBattery = view.findViewById(R.id.txtBattery);
    txtRssi = view.findViewById(R.id.txtRssi);
    recyclerView = view.findViewById(R.id.recyclerGearOptions);
    GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), /* spanCount= */ 2);
    layoutManager.setSpanSizeLookup(spanSizeLookup);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.addItemDecoration(new ItemOffsetDecoration(requireContext(), R.dimen.item_offset));
    homeViewModel.init();

    populateView();
    subscribeToNotifications();
    subscriptions.add(homeViewModel.getBatteryStatus().onNext(this::onBatteryStatus));
    subscribeEvents();
  }

  @Override
  public void onTwoFingerDoubleTap() {
    try {
      showAppVersionDialog();
    } catch (ParseException e) {
      PrintLogger.e(TAG, "Parsing issue for build date.");
    }
  }

  private void showAppVersionDialog() throws ParseException {
    DefaultDialog defaultDialog = new DefaultDialog.DefaultDialogBuilder()
        .setTitle(R.string.app_version_dialog_title)
        .setSubtitle(getString(R.string.app_version_dialog_desc,
            BuildConfig.SDK_VERSION,
            com.google.android.jacquard.sample.BuildConfig.GIT_HEAD,
            getBuildDate()))
        .setPositiveButtonTitleId(R.string.ok_caps)
        .setShowNegativeButton(false)
        .setShowPositiveButton(true)
        .setCancellable(true)
        .setShowSubtitle(true)
        .setShowProgress(false)
        .build();

    defaultDialog.show(getParentFragmentManager(), /* tag= */null);
  }

  private String getBuildDate() throws ParseException {
    String versionStr = String.valueOf(com.google.android.jacquard.sample.BuildConfig.VERSION_CODE);
    SimpleDateFormat original = new SimpleDateFormat("yyyyMMddHH");
    SimpleDateFormat updated = new SimpleDateFormat("yyyy-MM-dd hh aa");
    Date parsedDate = original.parse(versionStr);
    return parsedDate != null ? updated.format(parsedDate) : "Not Available";
  }

  @Override
  public void onDestroyView() {
    for (Subscription subscription : subscriptions) {
      subscription.unsubscribe();
    }
    subscriptions.clear();
    super.onDestroyView();
  }

  private void onNavigation(State state) {
    PrintLogger.d(TAG, "onNavigation: " + state);
    switch (state.getType()) {
      case ADAPTER:
        onSetAdapter(state.adapter().listForAdapter(), state.adapter().itemClickListener());
        return;
      case CONNECTED:
        onConnected(state.connected());
        return;
      case DISCONNECTED:
        onDisconnected();
        return;
      case ERROR:
        showSnackbar(state.error());
    }
  }

  private void onSetAdapter(
      List<HomeTileModel> list, ItemClickListener<HomeTileModel> itemClickListener) {
    if (recyclerView.getAdapter() == null) {
      recyclerView.setAdapter(new HomeTilesListAdapter(requireContext(), itemClickListener));
    }
    ((ListAdapter) recyclerView.getAdapter()).submitList(list);
  }

  private NavController getNavController() {
    return NavHostFragment.findNavController(this);
  }

  private void populateView() {
    homeViewModel.getTagName().onNext(txtTagName::setText);
  }

  private void subscribeToNotifications() {
    subscriptions.add(
        homeViewModel
            .getNotification()
            .observe(
                new ObservesNext<Notification>() {
                  @Override
                  public void onNext(@NonNull Notification notification) {
                    PrintLogger.d(TAG, "Received notification: " + notification);
                    switch (notification.getType()) {
                      case BATTERY:
                        onBatteryStatus(notification.battery());
                        break;
                      case GEAR:
                        onGearState(notification.gear());
                        break;
                    }
                  }

                  @Override
                  public void onError(@NonNull Throwable t) {
                    PrintLogger.e(TAG, "Failed Notification: " + t);
                  }
                }));
  }

  private void onRSSIChanged(int rssiValue) {
    if (isAdded()) {
      txtRssi.setText(getString(R.string.home_page_rssi_info, String.valueOf(rssiValue)));
      txtRssi.setTextColor(getContext().getColor(Util.getRSSIColor(rssiValue)));
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
                R.string.home_page_battery_info,
                String.valueOf(batteryStatus.batteryLevel()),
                chargingStateText));
  }

  private void onGearState(GearState gearState) {
    PrintLogger.d(TAG, "onGearState: " + gearState);
    if (gearState.getType() == GearState.Type. ATTACHED) {
      updateGearImage(gearState.attached().product().image());
      homeViewModel.onTagAttached(gearState.attached());
      txtGearState.setText(gearState.attached().product().name());
      txtGearState.setTextColor(requireContext().getColor(R.color.grey_700));
      txtTagName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.green_indicator, 0);
    } else {
      updateGearImage(blurProductImageName);
      homeViewModel.onTagConnected();
      txtGearState.setText(requireContext().getString(R.string.state_not_attached));
      txtGearState.setTextColor(requireContext().getColor(R.color.home_tile_grey_shade_2));
      txtTagName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.red_indicator, 0);
    }
  }

  private void updateGearImage(String productImage) {
    int imageResourceId = requireContext().getResources().getIdentifier(productImage, /* defType = */"drawable", getContext().getPackageName());
    imgProduct.setImageResource(imageResourceId);
  }

  private void onConnected(ConnectedJacquardTag tag) {
    subscriptions.add(tag.rssiSignal().onNext(this::onRSSIChanged));
    onGearState(GearState.ofDetached());
  }

  private void onDisconnected() {
    homeViewModel.onTagDisconnected();
    txtBattery.setText("");
    txtRssi.setText("");
    txtTagName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.red_indicator, 0);
    txtGearState.setText(requireContext().getString(R.string.state_disconnected));
    txtGearState.setTextColor(requireContext().getColor(R.color.home_tile_grey_shade_2));
  }

  private void showSnackbar(String message) {
    Util.showSnackBar(getView(), message);
  }

  private void showDisconnectSnackbar() {
    Util.showSnackBar(getView(),
        getString(R.string.tag_not_connected),
        getString(R.string.disconnect_help),
        view -> showDisconnectDialog());
  }

  private void showDisconnectDialog() {
    new DefaultDialog.DefaultDialogBuilder()
        .setTitle(R.string.disconnect_help_dialog_title)
        .setSubtitle(R.string.disconnect_help_dialog_subtitle)
        .setPositiveButtonTitleId(R.string.disconnect_help_dialog_positive_btn)
        .setShowNegativeButton(false)
        .setShowPositiveButton(true)
        .setCancellable(true)
        .setShowSubtitle(true)
        .setShowProgress(false)
        .build()
        .show(getParentFragmentManager(), /* tag= */null);
  }

  private void subscribeEvents() {
    subscriptions.add(homeViewModel.getConnectivityEvents().onNext(this::onEvents));
  }

  private void onEvents(Events events) {
    switch (events) {
      case TAG_DISCONNECTED:
        showDisconnectSnackbar();
        break;
      case TAG_DETACHED:
        showSnackbar(getString(R.string.gear_detached));
        break;
    }
  }
}
