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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sample.utilities.Util
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Executors
import com.google.android.jacquard.sdk.rx.Signal
import kotlinx.coroutines.launch
import java.io.File

/** Fragment to view IMU Samples. */
class ImuSamplesListFragment: Fragment() {

  companion object {
    private val TAG = ImuSamplesListFragment::class.java.simpleName
  }

  private val viewModel by lazy { getViewModel<ImuSamplesListViewModel>() }
  private val imuSamplesList: RecyclerView by lazy { requireView().findViewById(R.id.imu_samples_list) }
  private val title: TextView by lazy { requireView().findViewById(R.id.page_title) }
  private val progressLayout: LinearLayout by lazy { requireView().findViewById(R.id.progress) }
  private val imuDataListAdapter by lazy { ImuDataListAdapter(mutableListOf(), resources) }
  private val subscriptions: MutableList<Signal.Subscription> = mutableListOf()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.imu_samples_list, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initToolbar(view)
    initRecyclerView()
    showProgress()
    parseArguments()
  }

  override fun onDestroyView() {
    for (subscription in subscriptions) {
      subscription.unsubscribe()
    }
    subscriptions.clear()
    super.onDestroyView()
  }

  private fun initToolbar(view: View) {
    view.findViewById<Toolbar>(R.id.toolbar)
      .setNavigationOnClickListener { viewModel.backKeyPressed() }
  }

  private fun initRecyclerView() {
    context?.let { context ->
      ContextCompat.getDrawable(context, R.drawable.list_view_divider)?.let { drawable ->

        val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        divider.setDrawable(drawable)
        imuSamplesList.layoutManager = LinearLayoutManager(context)
        imuSamplesList.adapter = imuDataListAdapter
        imuSamplesList.addItemDecoration(divider)
      }
    }
  }

  private fun hideProgress() {
    if (isAdded) {
      changeStatusBarColor(R.color.white)
      progressLayout.visibility = View.GONE
    }
  }

  private fun showProgress() {
    if (isAdded) {
      changeStatusBarColor(R.color.progress_overlay)
      progressLayout.visibility = View.VISIBLE
    }
  }

  private fun changeStatusBarColor(@ColorRes color: Int) {
    requireActivity()
      .window.statusBarColor = ContextCompat.getColor(requireContext(), color)
  }

  private fun navigateBack(message: Int) {
    view?.let {
      Util.showSnackBar(it, getString(message), duration = 1000)
    }
    subscriptions.add(Signal.from(1).delay(1000).onNext { viewModel.backKeyPressed() })
  }

  private fun setTitle(path: String) {
    File(path).name.let { sessionName ->
      title.text = sessionName.subSequence(0, sessionName.lastIndexOf('.'))
      PrintLogger.d(TAG, "Parsing # $sessionName")
    }
  }

  private fun noPathError() {
    hideProgress()
    navigateBack(R.string.enable_to_parse_path_error)
  }

  private fun parseArguments() {
    arguments?.let { args ->
      val path = ImuSamplesListFragmentArgs.fromBundle(args).sessionDataFilePath
      if (path.isEmpty()) noPathError()
      else {
        startParsing(path)
        setTitle(path)
      }
    } ?: kotlin.run {
      // If Arguments does not exists.
      noPathError()
    }
  }

  private fun startParsing(path: String) {

    // Lifecycle aware Coroutine scope.
    lifecycleScope.launch {
      viewModel.parse(path)
        .observeOn(Executors.mainThreadExecutor())
        .observe({ imuTrialData ->
                   hideProgress()
                   for (sample in imuTrialData.imuSampleCollections) {
                     imuDataListAdapter.add(sample.imuSamples)
                   }
                   PrintLogger.d(
                     TAG,
                     "Total Imu Samples found # ${imuDataListAdapter.itemCount}"
                   )
                   if (imuDataListAdapter.itemCount == 0) {
                     navigateBack(R.string.no_imu_samples_after_parsing)
                   }

                 }, { error ->
                   hideProgress()
                   error?.let {
                     navigateBack(R.string.enable_to_parse)
                   }
                 })
    }
  }
}