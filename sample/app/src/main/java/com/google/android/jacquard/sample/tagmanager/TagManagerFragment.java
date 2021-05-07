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
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.jacquard.sample.MainActivity;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.ViewModelFactory;
import com.google.android.jacquard.sample.tagmanager.TagManagerViewModel.State;
import com.google.android.jacquard.sample.utilities.Util;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;

/**
 * A fragment for tag manager screen.
 */
public class TagManagerFragment extends Fragment {

  private TagManagerViewModel viewModel;
  private TagManagerAdapter adapter;
  private Subscription subscription;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel =
        new ViewModelProvider(
            requireActivity(),
            new ViewModelFactory(requireActivity().getApplication(), getNavController()))
            .get(TagManagerViewModel.class);
    adapter = new TagManagerAdapter(intentSender -> ((MainActivity) requireActivity())
        .startForResult(intentSender, COMPANION_DEVICE_REQUEST)
        .map(result -> result.resultCode() == Activity.RESULT_OK), viewModel);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_tag_manager, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initView(view);
    initToolbar();
    initScanningFlow();
    subscription = viewModel.stateSignal.onNext(this::onState);
  }

  @Override
  public void onDestroyView() {
    subscription.unsubscribe();
    super.onDestroyView();
  }

  private NavController getNavController() {
    return NavHostFragment.findNavController(this);
  }

  private void initView(View view) {
    RecyclerView recyclerView = view.findViewById(R.id.tag_manager_recyclerview);
    recyclerView.setAdapter(adapter);
    adapter.submitList(viewModel.getKnownTagsSection());
  }

  private void initToolbar() {
    Toolbar toolbar = getView().findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> {
      viewModel.backArrowClick();
    });
  }

  private void initScanningFlow() {
    ImageView scanView = getView().findViewById(R.id.initiate_scan_iv);
    scanView.setOnClickListener(v -> viewModel.initiateScan());
  }

  private void onState(State navigation) {
    switch (navigation.getType()) {
      case ACTIVE:
        Util.showSnackBar(getView(),
            getString(R.string.selected_as_current_tag, navigation.active()));
    }
  }
}
