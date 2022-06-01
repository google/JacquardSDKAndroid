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

package com.google.android.jacquard.sample.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.jacquard.sample.BuildConfig
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.rx.Signal.Subscription

/** A fragment for splashing. */
class SplashFragment : Fragment() {

    private val viewModel by lazy { getViewModel<SplashViewModel>() }
    private val subscriptions = mutableListOf<Subscription>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        subscriptions.add(Signal.from(1).delay(2000).onNext { viewModel.navigateToNextScreen() })
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAppVersion()
    }

    override fun onDestroyView() {
        for (subscription in subscriptions) {
            subscription.unsubscribe()
        }
        subscriptions.clear()
        super.onDestroyView()
    }

    private fun setAppVersion() {
        requireView().findViewById<TextView>(R.id.app_version).text = requireContext()
            .getString(R.string.app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }

}