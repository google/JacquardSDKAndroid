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

package com.google.android.jacquard.sample.haptics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sample.utilities.Util
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Signal

/**
 * Fragment for Haptics screen.
 */
class HapticsFragment : Fragment(), HapticsAdapter.ItemClickListener {

    companion object {
        private val TAG = HapticsFragment::class.java.simpleName
    }

    private val subscriptions: MutableList<Signal.Subscription?> = mutableListOf()
    private lateinit var adapter: HapticsAdapter
    private val viewModel by lazy { getViewModel<HapticsViewModel>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = HapticsAdapter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_haptics, container,  /* attachToRoot= */false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(view)
        view.findViewById<RecyclerView>(R.id.haptics_recyclerview).adapter = adapter
        viewModel.init()
        subscribeEvents()
    }

    override fun onDestroyView() {
        for (subscription in subscriptions) {
            subscription?.unsubscribe()
        }
        subscriptions.clear()
        super.onDestroyView()
    }

    override fun onItemClick(patternItem: HapticPatternType) {
        subscriptions.add(viewModel.sendHapticRequest(patternItem)
            .tap { PrintLogger.d(TAG, "Haptic sent successfully.") }
            .onError { error: Throwable ->
                error.apply { message?.let { showSnackbar(it) } }
            })
    }

    override fun onPause() {
        viewModel.sendHapticRequest(HapticPatternType.STOP_PATTERN).consume()
        super.onPause()
    }

    private fun getNavController(): NavController {
        return NavHostFragment.findNavController(this)
    }

    private fun initToolbar(root: View) {
        root.findViewById<Toolbar>(R.id.toolbar).setOnClickListener {
            viewModel.backArrowClick()
        }
    }

    private fun showSnackbar(message: String) {
        Util.showSnackBar(view, message)
    }

    private fun subscribeEvents() {
        subscriptions.add(
            viewModel.getConnectivityEvents()
                .onNext { events: ConnectivityManager.Events -> this.onEvents(events) })
    }

    private fun onEvents(events: ConnectivityManager.Events) {
        when (events) {
            ConnectivityManager.Events.TAG_DISCONNECTED -> showSnackbar(getString(R.string.tag_not_connected))
            ConnectivityManager.Events.TAG_DETACHED -> showSnackbar(getString(R.string.gear_detached))
            else -> PrintLogger.d(TAG, "Event received > " + events.name)
        }
    }
}