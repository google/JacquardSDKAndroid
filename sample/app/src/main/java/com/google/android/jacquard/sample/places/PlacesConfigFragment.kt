/*
 *
 *
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

package com.google.android.jacquard.sample.places

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.BuildConfig
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogBuilder
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogButtonClickListener
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sample.utilities.Util.showSnackBar
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Signal


class PlacesConfigFragment : Fragment() {
    companion object {
        private val TAG = PlacesConfigFragment::class.java.simpleName
    }

    private val viewModel by lazy { getViewModel<PlacesConfigViewModel>() }
    private val subscriptions: MutableList<Signal.Subscription> = mutableListOf()

    private val enableLocationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && canAssignGesture()) {
            viewModel.assignAbility()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (canAssignGesture()) {
                viewModel.assignAbility()
            }
        } else {
            shouldShowRequestPermissionRationale()
        }
    }

    private fun shouldShowRequestPermissionRationale() {
        if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
            return
        }
        PrintLogger.d(TAG, "need to show permission rational custom dialog.")
        showMandatoryDialog()
    }

    private fun showMandatoryDialog() {
        DefaultDialogBuilder()
            .setTitle(R.string.places_permission_dialog_title)
            .setSubtitle(R.string.places_permission_dialog_subtitle)
            .setPositiveButtonTitleId(R.string.places_permission_dialog_positive_btn)
            .setNegativeButtonTitle(R.string.places_permission_dialog_negative_btn)
            .setShowNegativeButton(true)
            .setShowPositiveButton(true)
            .setCancellable(false)
            .setShowSubtitle(true)
            .setShowProgress(false)
            .setPositiveButtonClick(object : DefaultDialogButtonClickListener() {
                override fun buttonClick() {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", requireContext().packageName, null)
                    startActivity(intent)
                }
            }).build().show(parentFragmentManager,  /* tag= */null)
    }

    private fun canAssignGesture(): Boolean {
        return if (!hasPermissions()) {
            false
        } else hasLocationEnabled()
    }

    private fun hasPermissions(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                requireActivity(),
                permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            requestPermissionLauncher.launch(permission.ACCESS_FINE_LOCATION)
            false
        }
    }

    private fun hasLocationEnabled(): Boolean {
        if (LocationManagerCompat.isLocationEnabled(
                requireContext()
                    .getSystemService(Context.LOCATION_SERVICE) as LocationManager
            )
        ) {
            return true
        }
        enableLocationLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        subscriptions.add(viewModel.stateSignal.onNext(this::onState));
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_places_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(view)
        initView(view)
    }

    override fun onDestroy() {
        unsubscribe()
        super.onDestroy()
    }

    private fun initToolbar(root: View) {
        root.findViewById<Toolbar>(R.id.toolbar)
            .setNavigationOnClickListener { viewModel.backArrowClick() }
    }

    private fun initView(view: View) {
        view.findViewById<Button>(R.id.assignButton).setOnClickListener(viewModel)
        view.findViewById<RecyclerView>(R.id.recyclerview).adapter = GestureAdapter(
            viewModel.getGestures(), viewModel
        )
    }

    private fun unsubscribe() {
        for (subscription in subscriptions) {
            subscription.unsubscribe()
        }
        subscriptions.clear()
    }

    private fun onState(state: PlacesConfigViewModel.State) {
        when (state.type) {
            PlacesConfigViewModel.State.Type.ASSIGNED -> {
                PrintLogger.d(TAG, "onState() :: assign state.")
                if (TextUtils.isEmpty(BuildConfig.MAPS_API_KEY)) {
                    showApiKeyMissingDialog()
                    return
                }
                if (canAssignGesture()) {
                    PrintLogger.d(TAG, "onState() :: Gesture is assigned.")
                    viewModel.assignAbility()
                }
            }
            PlacesConfigViewModel.State.Type.ERROR -> showSnackBar(
                view, state.error()
            )
        }
    }

    private fun showApiKeyMissingDialog() {
        DefaultDialogBuilder()
            .setTitle(R.string.key_required)
            .setSubtitle(R.string.key_required_desc)
            .setPositiveButtonTitleId(R.string.ok)
            .setShowNegativeButton(false)
            .setShowPositiveButton(true)
            .setCancellable(false)
            .setShowSubtitle(true)
            .setShowProgress(false)
            .build().show(parentFragmentManager,  /* tag= */null)
    }
}