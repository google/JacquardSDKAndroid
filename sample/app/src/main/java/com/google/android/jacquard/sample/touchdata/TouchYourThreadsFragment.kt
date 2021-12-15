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

package com.google.android.jacquard.sample.touchdata

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.jacquard.sample.ConnectivityManager.Events
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sample.utilities.Util
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Signal

/** Fragment for the touch your threads screen. */
class TouchYourThreadsFragment() : Fragment() {

    companion object {
        private val TAG: String = TouchYourThreadsFragment::class.java.simpleName
    }

    private val viewModel by lazy { getViewModel<TouchDataViewModel>() }
    private val subscriptions = mutableListOf<Signal.Subscription>()
    private lateinit var touchDataSubscription: Signal.Subscription
    private lateinit var chart: BarChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_touch_data, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chart = view.findViewById(R.id.chart)
        viewModel.init()
        view.viewTreeObserver
            .addOnGlobalLayoutListener(
                object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        setChartBounds(view)
                        view.viewTreeObserver.removeOnGlobalLayoutListener( /* victim= */this)
                    }
                })
        setUpChart()
        initToolbar(view)
        subscribeEvents()
    }

    override fun onResume() {
        super.onResume()
        touchDataSubscription = viewModel
            .startTouchStream()
            .observe(
                { touchData ->
                    updateTouchLines(
                        touchData,
                    )
                }
            ) { error: Throwable ->
                PrintLogger.e(TAG, error.message)
                error.message?.let { showSnackbar(it) }
            }
    }

    override fun onPause() {
        touchDataSubscription.unsubscribe()
        super.onPause()
    }

    override fun onDestroy() {
        for (subscription in subscriptions) {
            subscription.unsubscribe()
        }
        subscriptions.clear()
        super.onDestroy()
    }

    private fun updateTouchLines(touchLines: List<Int>) {
        val data = chart.data
        val barDataSet = data.getDataSetByIndex(0) as BarDataSet
        barDataSet.resetColors()
        barDataSet.clear()
        chart.notifyDataSetChanged()
        chart.invalidate()
        val colors = IntArray(touchLines.size)
        val labels = arrayOfNulls<String>(touchLines.size)
        for ((index, value) in touchLines.withIndex()) {
            barDataSet.addEntry(BarEntry(index.toFloat(), value.toFloat()))
            colors[index] = Color.argb(value, 66, 66, 255)
            labels[index] = Integer.toString(value)
        }
        barDataSet.setColors(*colors)
        barDataSet.setDrawValues(false)
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.xAxis.labelCount = labels.size
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    private fun subscribeEvents() {
        subscriptions.add(
            viewModel.getConnectivityEvents()
                .onNext { events ->
                    onEvents(
                        events
                    )
                }
        )
    }

    private fun showSnackbar(message: String) {
        Util.showSnackBar(view, message)
    }

    private fun onEvents(events: Events) {
        when (events) {
            Events.TAG_DISCONNECTED -> showSnackbar(getString(R.string.tag_not_connected))
            Events.TAG_DETACHED -> showSnackbar(getString(R.string.gear_detached))
        }
    }

    private fun initToolbar(root: View) =
        root.findViewById<Toolbar>(R.id.toolbar)
            .setNavigationOnClickListener { viewModel.backArrowClick() }

    private fun setChartBounds(rootView: View) {
        val height = rootView.measuredHeight
        val width = rootView.measuredWidth
        val chartDimension = if (width < height) width else height
        chart.layoutParams = LinearLayout.LayoutParams(chartDimension, chartDimension)
    }

    private fun setUpChart() {
        val threads: MutableList<BarEntry> = ArrayList()
        val labelsList: MutableList<String> = ArrayList()
        // Initial Entry
        repeat(viewModel.getThreadCount()) { index ->
            threads.add(BarEntry(index.toFloat(), 0F))
            labelsList.add("0")
        }
        setUpChartLabels(labelsList)
        setChartProperties()
        chart.data = getBarData(threads)
    }

    private fun setUpChartLabels(labelsList: List<String>) {
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labelsList)
        chart.xAxis.labelCount = labelsList.size
    }

    private fun setChartProperties() {
        chart.animateY( /* durationMillis= */500)
        chart.axisRight.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.setDrawAxisLine(false)
        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.axisMaximum = 255f
        chart.axisRight.setDrawAxisLine(false)
        chart.axisRight.axisMinimum = 0f
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(false)
        chart.setDrawBarShadow(true)
        chart.legend.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)
        chart.setTouchEnabled(false)
        chart.isDoubleTapToZoomEnabled = false
        chart.setPinchZoom(false)
        chart.description.isEnabled = false
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.axisLeft.isEnabled = false
        chart.axisRight.isEnabled = false
    }

    private fun getBarData(threads: List<BarEntry>): BarData {
        val bardataset = BarDataSet(threads, getText(R.string.thread_visualizer_title).toString())
        val data = BarData(bardataset)
        data.barWidth = 0.9f
        bardataset.barShadowColor = resources.getColor(R.color.bar_shadow_background, null)
        bardataset.color = resources.getColor(R.color.bar_background, null)
        bardataset.setDrawValues(false)
        return data
    }

}