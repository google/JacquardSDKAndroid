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

package com.google.android.jacquard.sample.touchdata;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.LinearLayout.LayoutParams;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.jacquard.sample.ConnectivityManager.Events;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.ViewModelFactory;
import com.google.android.jacquard.sample.utilities.Util;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal;
import java.util.ArrayList;
import java.util.List;

/** Fragment for the touch your threads screen. */
public class TouchYourThreadsFragment extends Fragment {

  public static final String TAG = TouchYourThreadsFragment.class.getSimpleName();
  private final List<Signal.Subscription> subscriptions = new ArrayList<>();
  private TouchDataViewModel viewModel;
  private Signal.Subscription touchDataSubscription;
  private BarChart chart;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_touch_data, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    chart = view.findViewById(R.id.chart);
    viewModel =
        new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(requireActivity().getApplication(), getNavController()))
            .get(TouchDataViewModel.class);
    view.getViewTreeObserver()
        .addOnGlobalLayoutListener(
            new OnGlobalLayoutListener() {
              @Override
              public void onGlobalLayout() {
                setChartBounds(view);
                view.getViewTreeObserver().removeOnGlobalLayoutListener(/* victim= */ this);
              }
            });
    setUpChart();
    initToolbar(view);
    subscribeEvents();
  }

  @Override
  public void onResume() {
    super.onResume();
    touchDataSubscription =
        viewModel
            .startTouchStream()
            .observe(touchData -> {
              updateTouchLines(touchData);
            }, error -> {
              if (error == null) {
                return;
              }
              PrintLogger.e(TAG, error.getMessage());
              showSnackbar(error.getMessage());
            });
  }

  @Override
  public void onPause() {
    if (touchDataSubscription != null) {
      touchDataSubscription.unsubscribe();
    }
    super.onPause();
  }

  @Override
  public void onDestroy() {
    for (Signal.Subscription subscription : subscriptions) {
      subscription.unsubscribe();
    }
    subscriptions.clear();
    super.onDestroy();
  }

  private void initToolbar(View root) {
    Toolbar toolbar = root.findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> viewModel.backArrowClick());
  }

  private NavController getNavController() {
    return NavHostFragment.findNavController(this);
  }

  private void updateTouchLines(@NonNull List<Integer> touchLines) {
    BarData data = chart.getData();
    BarDataSet barDataSet = (BarDataSet) data.getDataSetByIndex(0);
    if (barDataSet == null) {
      return;
    }
    barDataSet.resetColors();
    barDataSet.clear();
    chart.notifyDataSetChanged();
    chart.invalidate();
    int[] colors = new int[touchLines.size()];
    String[] labels = new String[touchLines.size()];
    for (int i = 0; i < touchLines.size(); i++) {
      barDataSet.addEntry(new BarEntry(i, touchLines.get(i)));
      colors[i] = Color.argb(touchLines.get(i), 66, 66, 255);
      labels[i] = Integer.toString(touchLines.get(i));
    }
    barDataSet.setColors(colors);
    barDataSet.setDrawValues(false);
    chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
    chart.getXAxis().setLabelCount(labels.length);
    chart.notifyDataSetChanged();
    chart.invalidate();
  }

  private void setUpChart() {
    List<BarEntry> threads = new ArrayList<>();
    List<String> labelsList = new ArrayList<>();
    // Initial Entry
    for (int i = 0; i < viewModel.getThreadCount(); i++) {
      threads.add(new BarEntry(i, 0));
      labelsList.add("0");
    }
    setUpChartLabels(labelsList);
    setChartProperties();
    chart.setData(getBarData(threads));
  }

  private void setChartBounds(View rootView) {
    int height = rootView.getMeasuredHeight();
    int width = rootView.getMeasuredWidth();
    int chartDimension = width < height ? width : height;
    chart.setLayoutParams(new LayoutParams(chartDimension, chartDimension));
  }

  private void setUpChartLabels(List<String> labelsList) {
    chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labelsList));
    chart.getXAxis().setLabelCount(labelsList.size());
  }

  private BarData getBarData(List<BarEntry> threads) {
    BarDataSet bardataset =
        new BarDataSet(threads, getText(R.string.thread_visualizer_title).toString());
    BarData data = new BarData(bardataset);
    data.setBarWidth(0.9f);
    bardataset.setBarShadowColor(getResources().getColor(R.color.bar_shadow_background, null));
    bardataset.setColor(getResources().getColor(R.color.bar_background, null));
    bardataset.setDrawValues(false);
    return data;
  }

  private void setChartProperties() {
    chart.animateY(/* durationMillis= */ 500);
    chart.getAxisRight().setDrawGridLines(false);
    chart.getAxisLeft().setDrawGridLines(false);
    chart.getAxisLeft().setDrawAxisLine(false);
    chart.getAxisLeft().setAxisMinimum(0f);
    chart.getAxisLeft().setAxisMaximum(255f);
    chart.getAxisRight().setDrawAxisLine(false);
    chart.getAxisRight().setAxisMinimum(0f);
    chart.getXAxis().setDrawGridLines(false);
    chart.getXAxis().setDrawAxisLine(false);
    chart.setDrawBarShadow(true);
    chart.getLegend().setEnabled(false);
    chart.setDrawGridBackground(false);
    chart.setDrawBorders(false);
    chart.setTouchEnabled(false);
    chart.setDoubleTapToZoomEnabled(false);
    chart.setPinchZoom(false);
    chart.getDescription().setEnabled(false);
    chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
    chart.getAxisLeft().setEnabled(false);
    chart.getAxisRight().setEnabled(false);
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
