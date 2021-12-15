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
package com.google.android.jacquard.sample.musicalthreads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.jacquard.sample.ConnectivityManager.Events
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sample.utilities.Util.showSnackBar
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Signal

class MusicalThreadsFragment : Fragment() {

  private val viewModel by lazy { getViewModel<MusicalThreadsViewModel>() }
  private val threadCount by lazy { viewModel.getThreadCount() }
  private val subscriptions: MutableList<Signal.Subscription> = mutableListOf()
  private lateinit var threadLayout: ViewGroup

  companion object {
    private val TAG = MusicalThreadsFragment::class.simpleName
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_musical_threads, container, /* attachToRoot= */false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewModel.init()
    threadLayout = view.findViewById(R.id.thread_layout)
    initToolbar(view)
    subscribeEvents()
  }

  override fun onDestroy() {
    unSubscribeSubscription()
    super.onDestroy()
  }

  private fun initToolbar(root: View) {
    root.findViewById<Toolbar>(R.id.toolbar)
      .setNavigationOnClickListener { viewModel.backArrowClick() }
  }

  private fun subscribeEvents() {
    subscriptions.add(viewModel.getConnectivityEvents().onNext(this::onEvents))
    subscriptions.add(viewModel.startTouchStream().onNext(this::onLineUpdate))
  }

  private fun onEvents(events: Events) {
    when (events) {
      Events.TAG_DISCONNECTED ->
        showSnackbar(getString(R.string.tag_not_connected))
      Events.TAG_DETACHED ->
        showSnackbar(getString(R.string.gear_detached))
      else -> PrintLogger.d(TAG, "Not intended event $events")
    }
  }

  private fun onLineUpdate(touchLines: List<Int>) {
    var linePosition = touchLines.size - 1
    var layoutIndex = 0
    while (linePosition >= 0 && layoutIndex < threadCount) {
      threadLayout.getChildAt(layoutIndex++).background.level = touchLines[linePosition] * 20 / 255
      linePosition--
    }
  }

  private fun showSnackbar(message: String) {
    showSnackBar(view, message)
  }

  private fun unSubscribeSubscription() {
    for (subscription in subscriptions) {
      subscription.unsubscribe()
    }
    subscriptions.clear()
  }
}