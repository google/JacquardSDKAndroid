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

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment;
import com.github.mikephil.charting.components.Legend.LegendOrientation;
import com.github.mikephil.charting.components.Legend.LegendVerticalAlignment;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.ViewModelFactory;
import com.google.android.jacquard.sample.dialog.DefaultDialog;
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogBuilder;
import com.google.android.jacquard.sample.utilities.Util;
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState;
import com.google.android.jacquard.sdk.imu.InitState;
import com.google.android.jacquard.sdk.imu.InitState.Type;
import com.google.android.jacquard.sdk.imu.exception.DCException;
import com.google.android.jacquard.sdk.imu.model.ImuStream;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Consumer;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionMode;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatus;
import com.google.atap.jacquard.protocol.JacquardProtocol.ImuSample;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Fragment to show real time Imu Samples streaming.
 */
public class ImuStreamingFragment extends Fragment implements OnClickListener {

  private static final String TAG = ImuStreamingFragment.class.getSimpleName();
  private static final LegendEntry[] ACCL_LEGENDS = new LegendEntry[]{
      legendEntry("AX", Color.RED),
      legendEntry("AY", Color.GREEN),
      legendEntry("AZ", Color.BLUE)};

  private static final LegendEntry[] GYRO_LEGENDS = new LegendEntry[]{
      legendEntry("GX", Color.RED),
      legendEntry("GY", Color.GREEN),
      legendEntry("GZ", Color.BLUE)};
  private final List<Subscription> subscriptions = new ArrayList<>();
  private Button startStream, stopStream;
  private LinearLayout progressLayout;
  private ImuStreamingViewModel imuViewModel;
  private Subscription imuStreamingSubscription;
  private TextView progressMessage;
  private LineChart acclChart;
  private LineChart gyroChart;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.imu_fragment_rtstream, container, false);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    imuViewModel =
        new ViewModelProvider(
            requireActivity(),
            new ViewModelFactory(requireActivity().getApplication(), getNavController()))
            .get(ImuStreamingViewModel.class);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initToolbar();
    progressLayout = view.findViewById(R.id.progress);
    progressMessage = view.findViewById(R.id.progress_message);
    acclChart = view.findViewById(R.id.accel_linechart);
    gyroChart = view.findViewById(R.id.gyro_linechart);
    setChartProperties();
    setUpButtons(view);
    showProgress();
    initImuStreaming();
  }

  @Override
  public void onDestroyView() {
    hideProgress();
    for (Subscription subscription : subscriptions) {
      if (subscription != null) {
        subscription.unsubscribe();
      }
    }
    subscriptions.clear();
    imuViewModel.destroy();
    super.onDestroyView();
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.start_imu_streaming: {
        PrintLogger.d(TAG, "Start Streaming Clicked #");
        startStreaming();
        break;
      }
      case R.id.stop_imu_streaming: {
        PrintLogger.d(TAG, "Stop Streaming Clicked #");
        stopStreaming();
        break;
      }
    }
  }

  private static LegendEntry legendEntry(String label, int color) {
    return new LegendEntry(label, LegendForm.LINE, /* formSize= */10f, /* formLineWidth= */
        2f, /* formLineDashEffect= */null, color);
  }

  private void initImuStreaming() {
    subscriptions.add(imuViewModel.init()
        .tap(state -> updateProgressMessage(getInitStateMessage(state)))
        .filter(state -> state.isType(Type.INITIALIZED))
        .tap(state -> PrintLogger.d(TAG,
            "Module init done in fragment #"))
        .flatMap(ignore -> imuViewModel.isImuStreamingInProgress())
        .first()
        .observe(streamingInProgress -> {
          PrintLogger.d(TAG,
              "IMU streaming is in progress ? " + streamingInProgress);
          if (streamingInProgress) {
            hideProgress();
            imuStreamingSubscription = imuViewModel.startStreaming().observe(observeImuStream(),
                handleErrorInImuStream());
            subscriptions.add(imuStreamingSubscription);
          } else {
            PrintLogger.d(TAG, "Checking if DC is in Store mode #");
            subscriptions.add(checkIfOtherDCInProgress());
          }
        }, error -> {
          if (error != null) {
            onFatalError(error instanceof TimeoutException ? getString(R.string.tag_disconnected)
                : error.getMessage());
          }
        }));
  }

  private Subscription checkIfOtherDCInProgress() {
    return imuViewModel.getDataCollectionStatus().flatMap(status -> {
      PrintLogger.d(TAG, "DC Status # " + status);
      if (status.equals(DataCollectionStatus.DATA_COLLECTION_LOGGING)) {
        return imuViewModel.getCurrentDCMode();
      }
      return null; // Ignore
    }).observe(mode -> {
      PrintLogger.d(TAG, "Current DC Mode # " + mode);
      if (!DataCollectionMode.DATA_COLLECTION_MODE_STREAMING.equals(mode)) {
        onFatalError("Ujt is collecting IMU Samples in " + mode);
      }
      hideProgress();
    }, error -> {
      if (error != null) {
        hideProgress();
      }
    });
  }

  private LineDataSet createSet(int color) {
    LineDataSet set = new LineDataSet(null, /* label= */ "IMU"); // Label not for UI
    set.setAxisDependency(YAxis.AxisDependency.LEFT);
    set.setLineWidth(1.5f);
    set.setColor(color);
    set.setHighlightEnabled(false);
    set.setDrawValues(false);
    set.setDrawCircles(false);
    set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
    set.setCubicIntensity(0.2f);
    return set;
  }

  private void addToChart(ImuStream stream) {
    ImuSample sample = stream.imuSample();
    PrintLogger.d(TAG, "Imu Sample Received # " + sample);
    LineData acclData = acclChart.getData();
    if (acclData != null) {
      int count = acclData.getDataSetByIndex(0).getEntryCount();
      acclData.addEntry(new Entry(count, sample.getAccX()), 0);
      acclData.addEntry(new Entry(count, sample.getAccY()), 1);
      acclData.addEntry(new Entry(count, sample.getAccZ()), 2);
      acclData.notifyDataChanged();
      acclChart.notifyDataSetChanged();
      acclChart.setVisibleXRangeMaximum(50);
      acclChart.moveViewToX(acclData.getEntryCount());
    }

    LineData gyroData = gyroChart.getData();
    if (gyroData != null) {
      int count = gyroData.getDataSetByIndex(0).getEntryCount();
      gyroData.addEntry(new Entry(count, sample.getGyroRoll()), 0);
      gyroData.addEntry(new Entry(count, sample.getGyroPitch()), 1);
      gyroData.addEntry(new Entry(count, sample.getGyroYaw()), 2);
      gyroData.notifyDataChanged();
      gyroChart.notifyDataSetChanged();
      gyroChart.setVisibleXRangeMaximum(50);
      gyroChart.moveViewToX(gyroData.getEntryCount());
    }
  }

  private void setChartProperties() {
    populateEmptyDataSet(acclChart);
    setChartLegends(acclChart, ACCL_LEGENDS);
    setAxisAndBorders(acclChart);
    populateEmptyDataSet(gyroChart);
    setChartLegends(gyroChart, GYRO_LEGENDS);
    setAxisAndBorders(gyroChart);
  }

  private void populateEmptyDataSet(LineChart chart) {
    LineData data = new LineData();
    // add empty data
    chart.setData(data);
    ILineDataSet setX = data.getDataSetByIndex(0);
    if (setX == null) {
      setX = createSet(ACCL_LEGENDS[0].formColor);
      data.addDataSet(setX);
    }
    ILineDataSet setY = data.getDataSetByIndex(1);
    if (setY == null) {
      setY = createSet(ACCL_LEGENDS[1].formColor);
      data.addDataSet(setY);
    }
    ILineDataSet setZ = data.getDataSetByIndex(2);
    if (setZ == null) {
      setZ = createSet(ACCL_LEGENDS[2].formColor);
      data.addDataSet(setZ);
    }
  }

  private void setChartLegends(LineChart chart, LegendEntry[] legends) {
    Legend l = chart.getLegend();
    l.setTextColor(Color.BLACK);
    l.setOrientation(LegendOrientation.HORIZONTAL);
    l.setHorizontalAlignment(LegendHorizontalAlignment.LEFT);
    l.setVerticalAlignment(LegendVerticalAlignment.BOTTOM);
    l.setCustom(Arrays.asList(legends));
  }

  private void setAxisAndBorders(LineChart chart) {
    // enable description text
    chart.getDescription().setEnabled(false);
    // enable touch gestures
    chart.setTouchEnabled(true);
    // enable scaling and dragging
    chart.setDragEnabled(true);
    chart.setScaleEnabled(true);
    chart.setDrawGridBackground(false);
    // if disabled, scaling can be done on x- and y-axis separately
    chart.setPinchZoom(true);
    // set an alternative background color
    XAxis xl = chart.getXAxis();
    xl.setTextColor(Color.WHITE);
    xl.setDrawGridLines(false);
    xl.setAvoidFirstLastClipping(true);
    xl.setEnabled(false);
    xl.setDrawLabels(false);

    YAxis leftAxis = chart.getAxisLeft();
    leftAxis.setTextColor(Color.WHITE);
    leftAxis.setDrawGridLines(false);
    leftAxis.setDrawLabels(false);
    YAxis rightAxis = chart.getAxisRight();
    rightAxis.setEnabled(false);

    chart.getAxisLeft().setAxisMinimum(Short.MIN_VALUE);
    chart.getAxisLeft().setAxisMaximum(Short.MAX_VALUE);
    chart.getAxisLeft().setDrawGridLines(false);
    chart.getXAxis().setDrawGridLines(false);
    chart.setDrawBorders(false);
  }

  private String getInitStateMessage(InitState state) {
    switch (state.getType()) {
      case INIT:
        return getString(R.string.imu_init_state_message);
      case CHECK_FOR_UPDATES:
        return getString(R.string.imu_check_updates_message);
      case ACTIVATE:
        return getString(R.string.imu_activating_message);
      case MODULE_DFU:
        return getDfuStatusMessage(state.moduleDfu());
      case TAG_DFU:
        return getDfuStatusMessage(state.tagDfu());
      case INITIALIZED:
        return getString(R.string.imu_initialize_complete_message);
    }
    return ""; // should never come here
  }

  private String getDfuStatusMessage(FirmwareUpdateState dfuState) {
    switch (dfuState.getType()) {
      case IDLE:
        return getString(R.string.imu_dfu_idle_message);
      case PREPARING_TO_TRANSFER:
        return getString(R.string.imu_dfu_ready_to_transfer_message);
      case TRANSFER_PROGRESS:
        return getString(R.string.imu_dfu_progress_message, dfuState.transferProgress());
      case TRANSFERRED:
        return getString(R.string.imu_dfu_transfer_complete_message);
      case EXECUTING:
        return getString(R.string.imu_dfu_applying_updates_message);
      case COMPLETED:
        return getString(R.string.imu_dfu_updates_done_message);
      case ERROR:
        return getString(R.string.imu_dfu_error_message);
    }
    return "";
  }

  private void stopStreaming() {
    showProgress();
    imuViewModel.stopStreaming().observe(stopped -> {
      PrintLogger.d(TAG, "Stop stream successful ? " + stopped);
      if (stopped) {
        hideProgress();
        imuStreamingSubscription.unsubscribe();
        stopStream.setVisibility(View.GONE);
        startStream.setVisibility(View.VISIBLE);
      }
    }, this::handleError);
  }

  private void startStreaming() {
    showProgress();
    imuStreamingSubscription = imuViewModel.startStreaming().observe(observeImuStream(),
        handleErrorInImuStream());
    subscriptions.add(imuStreamingSubscription);
  }

  private Consumer<Throwable> handleErrorInImuStream() {
    return error -> {
      hideProgress();
      if (error instanceof DCException) {
        DataCollectionStatus status = ((DCException) error).getDataCollectionStatus();
        if (status.equals(DataCollectionStatus.DATA_COLLECTION_LOW_BATTERY) || status
            .equals(DataCollectionStatus.DATA_COLLECTION_LOW_STORAGE)) {
          int message = getErrorMessage(status);
          int title = getErrorTitle(status);
          getErrorDialog(title, message).show(getParentFragmentManager(), /* tag= */ null);
          return;
        }
      }
      handleError(error);
    };
  }

  private Consumer<ImuStream> observeImuStream() {
    return stream -> {
      hideProgress();
      updateSessionButtons(DataCollectionStatus.DATA_COLLECTION_LOGGING);
      addToChart(stream);
    };
  }

  private DefaultDialog getErrorDialog(int title, int message) {
    return new DefaultDialogBuilder()
        .setCancellable(false)
        .setShowPositiveButton(true)
        .setPositiveButtonTitleId(R.string.got_it)
        .setShowSubtitle(true)
        .setTitle(title)
        .setSubtitle(message).build();
  }

  private int getErrorTitle(DataCollectionStatus status) {
    switch (status) {
      case DATA_COLLECTION_LOW_BATTERY:
        return R.string.imu_low_battery_title;
      case DATA_COLLECTION_LOW_STORAGE:
        return R.string.imu_low_storage_title;
      default:
        return 0;
    }
  }

  private void updateSessionButtons(DataCollectionStatus dcstatus) {
    if (dcstatus.equals(DataCollectionStatus.DATA_COLLECTION_LOGGING)) {
      startStream.setVisibility(View.GONE);
      stopStream.setVisibility(View.VISIBLE);
    } else {
      stopStream.setVisibility(View.GONE);
      startStream.setVisibility(View.VISIBLE);
    }
  }

  private void setUpButtons(@NonNull View view) {
    startStream = view.findViewById(R.id.start_imu_streaming);
    stopStream = view.findViewById(R.id.stop_imu_streaming);
    startStream.setOnClickListener(this);
    stopStream.setOnClickListener(this);
  }

  private void onFatalError(String message) {
    Util.showSnackBar(getView(), message);
    subscriptions.add(Signal.from(1).delay(1000).onNext(ignore -> imuViewModel.backKeyPressed()));
  }

  private void handleError(Throwable error) {
    if (error != null) {
      hideProgress();
      error.printStackTrace();
      String message;
      if (error instanceof DCException) {
        message = getString(getErrorMessage(((DCException) error).getDataCollectionStatus()));
      } else {
        message = error.getMessage();
      }
      Util.showSnackBar(requireView(), message);
    }
  }

  private int getErrorMessage(DataCollectionStatus status) {
    switch (status) {
      case DATA_COLLECTION_LOW_BATTERY:
        return R.string.imu_low_battery_message;
      case DATA_COLLECTION_LOW_STORAGE:
        return R.string.imu_low_storage_message;
      default:
        return R.string.imu_start_session_generic_error;
    }
  }

  private void initToolbar() {
    Toolbar toolbar = getView().findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> imuViewModel.backKeyPressed());
  }

  private void showProgress() {
    if (isAdded()) {
      updateProgressMessage(getString(R.string.please_wait));
      changeStatusBarColor(R.color.progress_overlay);
      progressLayout.setVisibility(View.VISIBLE);
    }
  }

  private void updateProgressMessage(String message) {
    progressMessage.setText(message);
  }

  private void hideProgress() {
    PrintLogger.d(TAG, "Hide Progress # ");
    if (isAdded()) {
      changeStatusBarColor(R.color.white);
      progressLayout.setVisibility(View.GONE);
    }
  }

  private void changeStatusBarColor(@ColorRes int color) {
    requireActivity().getWindow().setStatusBarColor(
        ContextCompat.getColor(requireContext(), color));
  }

  private NavController getNavController() {
    return NavHostFragment.findNavController(this);
  }
}