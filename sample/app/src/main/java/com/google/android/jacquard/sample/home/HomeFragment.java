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
import com.google.android.jacquard.sample.BuildConfig;
import com.google.android.jacquard.sample.ConnectivityManager.Events;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.ViewModelFactory;
import com.google.android.jacquard.sample.home.HomeViewModel.Notification;
import com.google.android.jacquard.sample.home.HomeViewModel.State;
import com.google.android.jacquard.sample.utilities.Util;
import com.google.android.jacquard.sdk.command.BatteryStatus;
import com.google.android.jacquard.sdk.command.BatteryStatus.ChargingState;
import com.google.android.jacquard.sdk.model.GearState;
import com.google.android.jacquard.sdk.rx.Signal.ObservesNext;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import timber.log.Timber;

/** Fragment for Dashboard UI. */
public class HomeFragment extends Fragment {

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
  private ImageView imgProduct;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    homeViewModel =
        new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication(), getNavController()))
            .get(HomeViewModel.class);
    homeViewModel.connect();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    subscriptions.add(homeViewModel.stateSignal.onNext(this::onNavigation));
    return inflater.inflate(R.layout.fragment_home, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    imgProduct = view.findViewById(R.id.imgProduct);
    txtGearState = view.findViewById(R.id.txtTagState);
    txtTagName = view.findViewById(R.id.txtTagName);
    txtBattery = view.findViewById(R.id.txtBattery);
    recyclerView = view.findViewById(R.id.recyclerGearOptions);
    GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), /* spanCount= */ 2);
    layoutManager.setSpanSizeLookup(spanSizeLookup);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.addItemDecoration(new ItemOffsetDecoration(requireContext(), R.dimen.item_offset));
    homeViewModel.init();

    populateView();
    subscribeToNotifications();
    setAppVersion();
    subscriptions.add(homeViewModel.getBatteryStatus().onNext(this::onBatteryStatus));
    subscribeEvents();
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
    switch (state.getType()) {
      case ADAPTER:
        onSetAdapter(state.adapter().listForAdapter(), state.adapter().itemClickListener());
        return;
      case CONNECTED:
        onConnected();
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
                    Timber.d("Received notification %s", notification);
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
                    Timber.e(t, "Failed Notification");
                  }
                }));
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
    if (gearState.getType() == GearState.Type. ATTACHED) {
      updateGearImage(gearState.attached().product().image());
      homeViewModel.onTagAttached(gearState.attached());
      txtGearState.setText(gearState.attached().product().name());
      txtGearState.setTextColor(requireContext().getColor(R.color.black));
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

  private void onConnected() {
    onGearState(GearState.ofDetached());
  }

  private void onDisconnected() {
    homeViewModel.onTagDisconnected();
    txtBattery.setText("");
    txtTagName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.red_indicator, 0);
    txtGearState.setText(requireContext().getString(R.string.state_disconnected));
    txtGearState.setTextColor(requireContext().getColor(R.color.home_tile_grey_shade_2));
  }

  private void setAppVersion() {
    TextView appVersion = getView().findViewById(R.id.app_version);
    appVersion.setText(getString(R.string.app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
  }

  private void showSnackbar(String message) {
    Util.showSnackBar(getView(), message);
  }

  private void subscribeEvents() {
    subscriptions.add(homeViewModel.getConnectivityEvents().onNext(this::onEvents));
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
