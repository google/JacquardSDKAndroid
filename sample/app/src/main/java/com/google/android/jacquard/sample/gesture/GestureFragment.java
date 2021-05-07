/*
 * Copyright 2021 Google LLC. All Rights Reserved.
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

package com.google.android.jacquard.sample.gesture;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;
import com.google.android.jacquard.sample.ConnectivityManager.Events;
import com.google.android.jacquard.sample.Preferences;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.SampleApplication;
import com.google.android.jacquard.sample.ViewModelFactory;
import com.google.android.jacquard.sample.gesture.GestureViewModel.GestureViewItem;
import com.google.android.jacquard.sample.utilities.Util;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment displaying gestures.
 */
public class GestureFragment extends Fragment {

  private final static String TAG = GestureFragment.class.getSimpleName();
  private final GestureAdapter adapter = new GestureAdapter();
  private ArrayList<Subscription> subscriptions = new ArrayList<>();
  private GestureView gestureView;
  private GestureViewModel viewModel;
  private     Preferences preferences;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    preferences = ((SampleApplication) requireContext().getApplicationContext())
        .getResourceLocator().getPreferences();
    viewModel =
        new ViewModelProvider(
            this, new ViewModelFactory(requireActivity().getApplication(), getNavController()))
            .get(GestureViewModel.class);
    subscriptions.add(viewModel.getGestures().onNext(this::onGestures));
    subscribeEvents();
  }

  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_gesture, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    gestureView = view.findViewById(R.id.gesture_overlay);

    RecyclerView recyclerView = view.findViewById(R.id.gesture_recyclerview);
    recyclerView.setAdapter(adapter);
    // Make sure the recyclerview is showing scroll to the top.
    adapter.registerAdapterDataObserver(
        new AdapterDataObserver() {
          @Override
          public void onItemRangeInserted(int positionStart, int itemCount) {
            recyclerView.scrollToPosition(positionStart);
          }
        });

    Toolbar toolbar = view.findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> viewModel.upClick());
    initInfoButton(view);
  }

  private void initInfoButton(View root) {
    View infoButton = root.findViewById(R.id.gesture_info_button);
    infoButton.setOnClickListener(v -> viewModel.gestureInfoClick());
    Integer toolTopStringResId = viewModel.getInfoButtonToolTip();

    if (preferences.isGestureLoaded()) {
      return;
    }
    preferences.setGestureLoaded(true);

    if (toolTopStringResId == null) {
      return;
    }
    String tooltip = getString(toolTopStringResId);
    TooltipCompat.setTooltipText(infoButton, tooltip);
    // Trigger the tooltip
    int delay = getResources().getInteger(android.R.integer.config_mediumAnimTime);
    infoButton.postDelayed(infoButton::performLongClick, delay);
  }

  private NavController getNavController() {
    return NavHostFragment.findNavController(this);
  }

  private void onGestures(List<GestureViewItem> items) {
    adapter.submitList(items);
    gestureView.show(items.get(0));
  }

  @Override
  public void onDestroy() {
    unSubscribeSubscription();
    super.onDestroy();
  }

  private void unSubscribeSubscription() {
    for (Subscription subscription : subscriptions) {
      subscription.unsubscribe();
    }
    subscriptions.clear();
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
