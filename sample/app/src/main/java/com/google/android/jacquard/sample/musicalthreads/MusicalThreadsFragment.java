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

package com.google.android.jacquard.sample.musicalthreads;

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
import com.google.android.jacquard.sample.ConnectivityManager.Events;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.ViewModelFactory;
import com.google.android.jacquard.sample.utilities.Util;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import java.util.ArrayList;
import java.util.List;

/** Fragment for the musical threads screen. */
public class MusicalThreadsFragment extends Fragment {

  private final List<Subscription> subscriptions = new ArrayList<>();

  private Subscription subscription = new Subscription();
  private MusicalThreadsViewModel viewModel;
  private ViewGroup threadLayout;
  private int threadCount = 0;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_musical_threads, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    viewModel =
        new ViewModelProvider(
                this, new ViewModelFactory(requireActivity().getApplication(), getNavController()))
            .get(MusicalThreadsViewModel.class);

    threadCount = viewModel.getThreadCount();
    threadLayout = view.findViewById(R.id.thread_layout);
    initToolbar(view);
    subscribeEvents();
  }

  @Override
  public void onResume() {
    super.onResume();
    subscription = viewModel.startTouchStream().onNext(this::onLineUpdate);
  }

  @Override
  public void onPause() {
    subscription.unsubscribe();
    super.onPause();
  }

  private void initToolbar(View root) {
    Toolbar toolbar = root.findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> viewModel.backArrowClick());
  }

  private void onLineUpdate(List<Integer> touchLines) {
    for (int i = touchLines.size() - 1, index = 0; i >= 0 && index < threadCount; i--) {
      threadLayout.getChildAt(index++).getBackground().setLevel(touchLines.get(i) * 20 / 255);
    }
  }

  private NavController getNavController() {
    return NavHostFragment.findNavController(this);
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
