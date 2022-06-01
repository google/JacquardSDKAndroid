/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sample.imu

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment
import com.github.mikephil.charting.components.Legend.LegendOrientation
import com.github.mikephil.charting.components.Legend.LegendVerticalAlignment
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.dialog.DefaultDialog
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogBuilder
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sample.fragment.extensions.showSnackBar
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState
import com.google.android.jacquard.sdk.lm.InitState
import com.google.android.jacquard.sdk.imu.exception.DCException
import com.google.android.jacquard.sdk.imu.model.ImuStream
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Consumer
import com.google.android.jacquard.sdk.rx.Signal
import com.google.atap.jacquard.protocol.JacquardProtocol
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatus
import java.util.concurrent.TimeoutException

/** Fragment to show real time Imu Samples streaming.*/
class ImuStreamingFragment : Fragment(), View.OnClickListener {

  companion object {
    private val TAG = ImuStreamingFragment::class.java.simpleName
  }

  private val ACCL_LEGENDS = listOf(
    legendEntry("AX", Color.RED),
    legendEntry("AY", Color.GREEN),
    legendEntry("AZ", Color.BLUE)
  )

  private val GYRO_LEGENDS = listOf(
    legendEntry("GX", Color.RED),
    legendEntry("GY", Color.GREEN),
    legendEntry("GZ", Color.BLUE)
  )

  private val UNKNOWN_ERROR = "Unknown Error"
  private val subscriptions = mutableListOf<Signal.Subscription>()
  private val imuViewModel by lazy { getViewModel<ImuStreamingViewModel>() }
  private val progressLayout: LinearLayout by lazy { requireView().findViewById(R.id.progress) }
  private val progressMessage: TextView by lazy { requireView().findViewById(R.id.progress_message) }
  private val acclChart: LineChart by lazy { requireView().findViewById(R.id.accel_linechart) }
  private val gyroChart: LineChart by lazy { requireView().findViewById(R.id.gyro_linechart) }
  private lateinit var startStream: Button
  private lateinit var stopStream: Button
  private var imuStreamingSubscription: Signal.Subscription? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.imu_fragment_rtstream, container, /* attachToRoot= */false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initToolbar(view)
    setChartProperties()
    showProgress()
    initImuStreaming()
    setUpButtons()
  }

  override fun onDestroyView() {
    hideProgress()
    for (subscription in subscriptions) {
      subscription.unsubscribe()
    }
    subscriptions.clear()
    imuViewModel.destroy()
    super.onDestroyView()
  }

  override fun onClick(v: View) {
    when(v.id) {
      R.id.start_imu_streaming -> {
        PrintLogger.d(TAG, "Start Streaming Clicked #")
        startStreaming()
      }
      R.id.stop_imu_streaming -> {
        PrintLogger.d(TAG, "Stop Streaming Clicked #")
        stopStreaming()
      }
    }
  }

  private fun initToolbar(view: View) {
    view.findViewById<Toolbar>(R.id.toolbar)
      .setNavigationOnClickListener { imuViewModel.backKeyPressed() }
  }

  private fun createSet(color: Int): LineDataSet {
    return LineDataSet(null,  /* label= */"IMU") // Label not for UI
      .apply {
        axisDependency = YAxis.AxisDependency.LEFT
        lineWidth = 1.5f
        this.color = color
        isHighlightEnabled = false
        setDrawValues(false)
        setDrawCircles(false)
        mode = LineDataSet.Mode.CUBIC_BEZIER
        cubicIntensity = 0.2f
      }
  }

  private fun populateEmptyDataSet(chart: LineChart) {
    val data = LineData()
    // add empty data
    chart.data = data
    var setX = data.getDataSetByIndex(0)
    if (setX == null) {
      setX = createSet(ACCL_LEGENDS[0].formColor)
      data.addDataSet(setX)
    }
    var setY = data.getDataSetByIndex(1)
    if (setY == null) {
      setY = createSet(ACCL_LEGENDS[1].formColor)
      data.addDataSet(setY)
    }
    var setZ = data.getDataSetByIndex(2)
    if (setZ == null) {
      setZ = createSet(ACCL_LEGENDS[2].formColor)
      data.addDataSet(setZ)
    }
  }

  private fun setChartLegends(chart: LineChart, legends: List<LegendEntry>) {
    chart.legend.apply {
      textColor = Color.BLACK
      orientation = LegendOrientation.HORIZONTAL
      horizontalAlignment = LegendHorizontalAlignment.LEFT
      verticalAlignment = LegendVerticalAlignment.BOTTOM
      setCustom(legends)
    }
  }

  private fun setChartProperties() {
    populateEmptyDataSet(acclChart)
    setChartLegends(acclChart, ACCL_LEGENDS)
    setAxisAndBorders(acclChart)
    populateEmptyDataSet(gyroChart)
    setChartLegends(gyroChart, GYRO_LEGENDS)
    setAxisAndBorders(gyroChart)
  }

  private fun setAxisAndBorders(chart: LineChart) {
    // enable description text
    chart.description.isEnabled = false
    // enable touch gestures
    chart.setTouchEnabled(true)
    // enable scaling and dragging
    chart.isDragEnabled = true
    chart.setScaleEnabled(true)
    chart.setDrawGridBackground(false)
    // if disabled, scaling can be done on x- and y-axis separately
    chart.setPinchZoom(true)
    // set an alternative background color
    val xl = chart.xAxis
    xl.textColor = Color.WHITE
    xl.setDrawGridLines(false)
    xl.setAvoidFirstLastClipping(true)
    xl.isEnabled = false
    xl.setDrawLabels(false)

    val leftAxis = chart.axisLeft
    leftAxis.textColor = Color.WHITE
    leftAxis.setDrawGridLines(false)
    leftAxis.setDrawLabels(false)
    val rightAxis = chart.axisRight
    rightAxis.isEnabled = false

    chart.axisLeft.axisMinimum = Short.MIN_VALUE.toFloat()
    chart.axisLeft.axisMaximum = Short.MAX_VALUE.toFloat()
    chart.axisLeft.setDrawGridLines(false)
    chart.xAxis.setDrawGridLines(false)
    chart.setDrawBorders(false)
  }

  private fun showProgress() {
    if (isAdded) {
      updateProgressMessage(getString(R.string.please_wait))
      changeStatusBarColor(R.color.progress_overlay)
      progressLayout.visibility = View.VISIBLE
    }
  }

  private fun updateProgressMessage(message: String) {
    progressMessage.text = message
  }

  private fun changeStatusBarColor(@ColorRes color: Int) {
    requireActivity()
      .window.statusBarColor = ContextCompat.getColor(requireContext(), color)
  }

  private fun initImuStreaming() {
    subscriptions.add(imuViewModel
                        .init()
                        .tap { state -> updateProgressMessage(getInitStateMessage(state)) }
                        .filter { state -> state.isType(InitState.Type.INITIALIZED) }
                        .tap { PrintLogger.d(TAG, "Module init done in fragment #") }
                        .flatMap { imuViewModel.isImuStreamingInProgress() }
                        .first()
                        .observe({ streamingInProgress ->
                                   PrintLogger.d(
                                     TAG,
                                     "IMU streaming is in progress ? $streamingInProgress"
                                   )

                                   if (streamingInProgress) {
                                     hideProgress()
                                     imuStreamingSubscription = imuViewModel.startStreaming()
                                       .observe(observeImuStream(), handleErrorInImuStream())
                                     subscriptions.add(imuStreamingSubscription!!)
                                   } else {
                                     PrintLogger.d(TAG, "Checking if DC is in Store mode #")
                                     subscriptions.add(checkIfOtherDCInProgress())
                                   }
                                 },
                                 { error ->
                                   error?.apply {
                                     onFatalError(
                                       if (this is TimeoutException) getString(R.string.tag_disconnected) else message
                                         ?: UNKNOWN_ERROR
                                     )
                                   }
                                 })
    )
  }

  private fun checkIfOtherDCInProgress(): Signal.Subscription {
    return imuViewModel.getDataCollectionStatus().flatMap { status ->
      PrintLogger.d(
        TAG,
        "DC Status # $status"
      )
      if (status == DataCollectionStatus.DATA_COLLECTION_LOGGING) {
        return@flatMap imuViewModel.getCurrentDCMode()
      }
      return@flatMap null; // Ignore
    }.observe({ mode ->
                PrintLogger.d(TAG, "Current DC Mode # $mode")
                hideProgress()
                if (JacquardProtocol.DataCollectionMode.DATA_COLLECTION_MODE_STREAMING != mode) {
                  onFatalError("Ujt is collecting IMU Samples in $mode")
                }
              }, { error -> error?.apply { hideProgress() } })
  }

  private fun onFatalError(message: String) {
    showSnackBar(message)
    subscriptions.add(Signal.from(1).delay(1000).onNext { imuViewModel.backKeyPressed() })
  }

  private fun getInitStateMessage(state: InitState): String {
    return when (state.type) {
      InitState.Type.INIT -> getString(R.string.imu_init_state_message)
      InitState.Type.CHECK_FOR_UPDATES -> getString(R.string.imu_check_updates_message)
      InitState.Type.ACTIVATE -> getString(R.string.imu_activating_message)
      InitState.Type.MODULE_DFU -> getDfuStatusMessage(state.moduleDfu())
      InitState.Type.TAG_DFU -> getDfuStatusMessage(state.tagDfu())
      InitState.Type.INITIALIZED -> getString(R.string.imu_initialize_complete_message)
    }
  }

  private fun getDfuStatusMessage(dfuState: FirmwareUpdateState): String {
    return when (dfuState.type) {
      FirmwareUpdateState.Type.IDLE -> getString(R.string.imu_dfu_idle_message)
      FirmwareUpdateState.Type.PREPARING_TO_TRANSFER -> getString(R.string.imu_dfu_ready_to_transfer_message)
      FirmwareUpdateState.Type.TRANSFER_PROGRESS -> getString(
        R.string.imu_dfu_progress_message,
        dfuState.transferProgress()
      )
      FirmwareUpdateState.Type.TRANSFERRED -> getString(R.string.imu_dfu_transfer_complete_message)
      FirmwareUpdateState.Type.EXECUTING -> getString(R.string.imu_dfu_applying_updates_message)
      FirmwareUpdateState.Type.COMPLETED -> getString(R.string.imu_dfu_updates_done_message)
      FirmwareUpdateState.Type.STOPPED -> getString(R.string.imu_dfu_updates_cancelled_message)
      FirmwareUpdateState.Type.ERROR -> getString(R.string.imu_dfu_error_message)
    }
  }

  private fun hideProgress() {
    PrintLogger.d(TAG, "Hide Progress # ")
    if(isAdded) {
      changeStatusBarColor(R.color.white)
      progressLayout.visibility = View.GONE
    }
  }

  private fun observeImuStream(): Consumer<ImuStream> {
    return Consumer<ImuStream> { stream ->
      hideProgress()
      updateSessionButtons(DataCollectionStatus.DATA_COLLECTION_LOGGING)
      addToChart(stream)
    }
  }

  private fun handleErrorInImuStream(): Consumer<Throwable> {
    return Consumer<Throwable> { error ->
      hideProgress()
      stopStream.visibility = View.GONE
      startStream.visibility = View.VISIBLE
      handleError(error)
      if (error is DCException) {
        val status = error.dataCollectionStatus
        if (status.equals(DataCollectionStatus.DATA_COLLECTION_LOW_BATTERY)
          || status.equals(DataCollectionStatus.DATA_COLLECTION_LOW_STORAGE)
        ) {
          val message = getErrorMessage(status)
          val title = getErrorTitle(status)
          getErrorDialog(title, message).show(parentFragmentManager, /* tag= */ null)
        }
      }
    }
  }

  private fun handleError(error: Throwable) {
    hideProgress()
    error.printStackTrace()
    val message = if (error is DCException) {
      getString(getErrorMessage(error.dataCollectionStatus))
    } else {
      error.message ?: UNKNOWN_ERROR
    }

    showSnackBar(message)
  }

  private fun getErrorMessage(status: DataCollectionStatus): Int {
    return when (status) {
      DataCollectionStatus.DATA_COLLECTION_LOW_BATTERY -> R.string.imu_low_battery_message
      DataCollectionStatus.DATA_COLLECTION_LOW_STORAGE -> R.string.imu_low_storage_message
      else -> R.string.imu_start_session_generic_error
    }
  }

  private fun getErrorTitle(status: DataCollectionStatus): Int {
    return when (status) {
      DataCollectionStatus.DATA_COLLECTION_LOW_BATTERY -> R.string.imu_low_battery_title
      DataCollectionStatus.DATA_COLLECTION_LOW_STORAGE -> R.string.imu_low_storage_title
      else -> 0
    }
  }

  private fun getErrorDialog(title: Int, message: Int): DefaultDialog {
    return DefaultDialogBuilder()
      .setCancellable(false)
      .setShowPositiveButton(true)
      .setPositiveButtonTitleId(R.string.got_it)
      .setShowSubtitle(true)
      .setTitle(title)
      .setSubtitle(message)
      .build()
  }

  private fun updateSessionButtons(dcstatus: JacquardProtocol.DataCollectionStatus) {
    if (dcstatus == DataCollectionStatus.DATA_COLLECTION_LOGGING) {
      startStream.visibility = View.GONE
      stopStream.visibility = View.VISIBLE
    } else {
      stopStream.visibility = View.GONE
      startStream.visibility = View.VISIBLE
    }
  }

  private fun addToChart(stream: ImuStream) {
    val sample = stream.imuSample()
    PrintLogger.d(TAG, "Imu Sample Received # $sample")
    acclChart.data?.apply {
      val count = getDataSetByIndex(0).entryCount
      addEntry(Entry(count.toFloat(), sample.accX.toFloat()), 0)
      addEntry(Entry(count.toFloat(), sample.accY.toFloat()), 1)
      addEntry(Entry(count.toFloat(), sample.accZ.toFloat()), 2)
      notifyDataChanged()
      acclChart.notifyDataSetChanged()
      acclChart.setVisibleXRangeMaximum(50f)
      acclChart.moveViewToX(entryCount.toFloat())
    }

    gyroChart.data?.apply {
      val count = getDataSetByIndex(0).entryCount
      addEntry(Entry(count.toFloat(), sample.gyroRoll.toFloat()), 0)
      addEntry(Entry(count.toFloat(), sample.gyroPitch.toFloat()), 1)
      addEntry(Entry(count.toFloat(), sample.gyroYaw.toFloat()), 2)
      notifyDataChanged()
      gyroChart.notifyDataSetChanged()
      gyroChart.setVisibleXRangeMaximum(50f)
      gyroChart.moveViewToX(entryCount.toFloat())
    }

  }

  private fun startStreaming() {
    showProgress()
    imuStreamingSubscription =
      imuViewModel.startStreaming().observe(observeImuStream(), handleErrorInImuStream())
    subscriptions.add(imuStreamingSubscription!!)
  }

  private fun stopStreaming() {
    showProgress()
    subscriptions.add(imuViewModel
                        .stopStreaming().observe({ stopped ->
                                                   PrintLogger.d(
                                                     TAG,
                                                     "Stop stream successful ? $stopped"
                                                   )
                                                   if (stopped) {
                                                     hideProgress()
                                                     imuStreamingSubscription?.unsubscribe()
                                                     stopStream.visibility = View.GONE
                                                     startStream.visibility = View.VISIBLE
                                                   }
                                                 }, { error -> error?.apply { handleError(this) } })
    )
  }

  private fun legendEntry(label: String, color: Int): LegendEntry {
    return LegendEntry(
      label,
      Legend.LegendForm.LINE,
      /* formSize= */ 10f,
      /* formLineWidth= */ 2f,
      /* formLineDashEffect= */ null,
      color
    )
  }

  private fun setUpButtons() {
    startStream = assignButton(R.id.start_imu_streaming, this)
    stopStream = assignButton(R.id.stop_imu_streaming, this)
  }

  private fun assignButton(id: Int, onClick: View.OnClickListener): Button {
    val v = requireView().findViewById<Button>(id)
    v.setOnClickListener(this)
    return v
  }
}