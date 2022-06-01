/*
 * Copyright 2021 Google LLC. All Rights Reserved.
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
package com.google.android.jacquard.sample.gesture

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.google.android.jacquard.sample.*
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sample.gesture.GestureViewModel.GestureViewItem
import com.google.android.jacquard.sample.utilities.Util.showSnackBar
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Signal
import java.util.*

/**
 * Fragment displaying gestures.
 */
class GestureFragment : Fragment() {
    companion object {
        private val TAG by lazy { MainActivity::class.java.simpleName }
    }
    private val adapter by lazy { GestureAdapter() }
    private val subscriptions = ArrayList<Signal.Subscription>()
    private lateinit var gestureView: GestureView
    private lateinit var preferences: Preferences
    private val viewModel by lazy { getViewModel<GestureViewModel>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = (requireContext().applicationContext as SampleApplication)
            .resourceLocator.preferences
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gesture, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gestureView = view.findViewById(R.id.gesture_overlay)
        val recyclerView: RecyclerView = view.findViewById(R.id.gesture_recyclerview)
        recyclerView.adapter = adapter
        // Make sure the recyclerview is showing scroll to the top.
        adapter.registerAdapterDataObserver(
            object : AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    recyclerView.scrollToPosition(positionStart)
                }
            })
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { viewModel.upClick() }
        viewModel.init()
        subscriptions.add(viewModel.getGestures().onNext { items -> onGestures(items) })
        subscribeEvents()
        initInfoButton(view)
    }

    private fun initInfoButton(root: View) {
        val infoButton = root.findViewById<View>(R.id.gesture_info_button)
        infoButton.setOnClickListener { viewModel.gestureInfoClick() }
        val toolTopStringResId = viewModel.infoButtonToolTip
        if (preferences.isGestureLoaded) {
            return
        }
        preferences.isGestureLoaded = true
        if (toolTopStringResId == null) {
            return
        }
        TooltipCompat.setTooltipText(infoButton, getString(toolTopStringResId))
        // Trigger the tooltip
        val delay = resources.getInteger(android.R.integer.config_mediumAnimTime)
        infoButton.postDelayed({ infoButton.performLongClick() }, delay.toLong())
    }

    private fun onGestures(items: List<GestureViewItem>) {
        adapter.submitList(items)
        gestureView.show(items[0])
    }

    override fun onDestroyView() {
        unSubscribeSubscription()
        super.onDestroyView()
    }

    private fun unSubscribeSubscription() {
        for (subscription in subscriptions) {
            subscription.unsubscribe()
        }
        subscriptions.clear()
    }

    private fun showSnackbar(message: String) {
        showSnackBar(view, message)
    }

    private fun subscribeEvents() {
        subscriptions.add(viewModel.connectivityEvents.onNext { events ->
            onEvents(
                events
            )
        })
    }

    private fun onEvents(events: ConnectivityManager.Events) {
        when (events) {
            ConnectivityManager.Events.TAG_DISCONNECTED -> showSnackbar(getString(R.string.tag_not_connected))
            ConnectivityManager.Events.TAG_DETACHED -> showSnackbar(getString(R.string.gear_detached))
            else -> {
                PrintLogger.d(TAG, "No action needed for other events");
            }
        }
    }
}