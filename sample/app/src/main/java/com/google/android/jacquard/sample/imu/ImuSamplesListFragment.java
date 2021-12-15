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
 */

package com.google.android.jacquard.sample.imu;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.ViewModelFactory;
import com.google.android.jacquard.sample.utilities.Util;
import com.google.android.jacquard.sdk.imu.parser.ImuSessionData.ImuSampleCollection;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Executors;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.atap.jacquard.protocol.JacquardProtocol.ImuSample;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment to view IMU Samples.
 */
public class ImuSamplesListFragment extends Fragment {

  private static final String TAG = ImuSamplesListFragment.class.getSimpleName();
  private String path;
  private ImuSamplesListViewModel viewModel;
  private RecyclerView imuSamplesList;
  private ImuDataListAdapter imuDataListAdapter;
  private TextView title;
  private LinearLayout progressLayout;
  private List<Subscription> subscriptions = new ArrayList<>();

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initToolbar();
    progressLayout = view.findViewById(R.id.progress);
    initRecyclerView(view);

    title = view.findViewById(R.id.page_title);
    String sessionName = new File(path).getName();
    title.setText(sessionName.substring(0, sessionName.lastIndexOf('.')));

    PrintLogger.d(TAG, "Parsing # " + sessionName);
    viewModel =
        new ViewModelProvider(
            requireActivity(),
            new ViewModelFactory(requireActivity().getApplication(), getNavController()))
            .get(ImuSamplesListViewModel.class);
    showProgress();
    startParsing();
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.imu_samples_list, container, false);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    path = ImuSamplesListFragmentArgs.fromBundle(getArguments()).getSessionDataFilePath();
  }

  @Override
  public void onDestroyView() {
    for (Subscription subscription : subscriptions) {
      if (subscription != null) {
        subscription.unsubscribe();
      }
    }
    subscriptions.clear();
    super.onDestroyView();
  }

  private void initRecyclerView(@NonNull View view) {
    DividerItemDecoration divider =
        new DividerItemDecoration(getContext(),
            DividerItemDecoration.VERTICAL);
    divider.setDrawable(ContextCompat.getDrawable(getContext(),
        R.drawable.list_view_divider));
    imuSamplesList = view.findViewById(R.id.imu_samples_list);
    imuSamplesList.setLayoutManager(new LinearLayoutManager(getContext()));
    imuDataListAdapter = new ImuDataListAdapter(new ArrayList<>());
    imuSamplesList.setAdapter(imuDataListAdapter);
    imuSamplesList.addItemDecoration(divider);
  }

  private void startParsing() {
    new Thread(() -> viewModel.parse(path)
        .observeOn(Executors.mainThreadExecutor())
        .observe(imuTrialData -> {
          hideProgress();
          List<ImuSampleCollection> samples = imuTrialData.getImuSampleCollections();
          int count = 0;
          for (ImuSampleCollection g : samples) {
            List<ImuSample> imuSamples = g.getImuSamples();
            if (!imuSamples.isEmpty()) {
              imuDataListAdapter.add(imuSamples);
              count += imuSamples.size();
            }
          }
          PrintLogger.d(TAG, "Total Imu Samples found # " + count);
          if (count == 0) {
            navigateBack(R.string.no_imu_samples_after_parsing);
          }
        }, error -> {
          hideProgress();
          if (error != null) {
            navigateBack(R.string.enable_to_parse);
          }
        })).start();
  }

  private void showProgress() {
    if (isAdded()) {
      changeStatusBarColor(R.color.progress_overlay);
      progressLayout.setVisibility(View.VISIBLE);
    }
  }

  private void hideProgress() {
    if (isAdded()) {
      changeStatusBarColor(R.color.white);
      progressLayout.setVisibility(View.GONE);
    }
  }

  private void changeStatusBarColor(@ColorRes int color) {
    requireActivity().getWindow().setStatusBarColor(
        ContextCompat.getColor(requireContext(), color));
  }

  private void navigateBack(int message) {
    Util.showSnackBar(getView(), getString(message), 1000);
    subscriptions.add(Signal.from(1).delay(1000).onNext(ignore -> viewModel.backKeyPressed()));
  }

  private void initToolbar() {
    Toolbar toolbar = getView().findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> viewModel.backKeyPressed());
  }

  private NavController getNavController() {
    return NavHostFragment.findNavController(this);
  }
}
