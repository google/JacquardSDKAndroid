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
package com.google.android.jacquard.sample.scan

import android.Manifest.permission
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.KnownTag
import com.google.android.jacquard.sample.MainActivity
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.dialog.DFUUtil
import com.google.android.jacquard.sample.dialog.DefaultDialog
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sample.scan.ScanViewModel.State
import com.google.android.jacquard.sample.utilities.Util
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Executors
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.rx.Signal.Subscription


/**
 * Fragment for scanning for jacquard tags.
 */
class ScanFragment : Fragment(), ScanAdapter.ItemClickListener {
    companion object {
        private val TAG = ScanFragment::class.java.simpleName
        private const val SCAN_TIMEOUT = 30000
    }

    private val viewModel by lazy { getViewModel<ScanViewModel>() }
    private val subscriptions: MutableList<Subscription?> = mutableListOf()
    private lateinit var adapter: ScanAdapter
    private lateinit var scanButton: Button
    private lateinit var stopScanButton: Button
    private lateinit var pairButton: Button
    private lateinit var tagIndicatorIcon: View
    private lateinit var progressBarHolder: View
    private lateinit var progressBar: View
    private lateinit var pairedIcon: View
    private lateinit var loadingTitle: TextView
    private lateinit var description: TextView
    private var scanSubscription: Subscription? = null
    private var tryAgainSubscription: Subscription? = null
    private var firmwareUpdateDialog: DefaultDialog? = null

    private val enableLocationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> if (result.resultCode == RESULT_OK && canStartScanning()) startScan() }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (canStartScanning()) startScan()
        } else shouldShowRequestPermissionRationale()
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> if (result.resultCode == RESULT_OK && canStartScanning()) startScan() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = ScanAdapter(this)
        subscriptions.add(
            viewModel.stateSignal.observeOn(Executors.mainThreadExecutor()).onNext { state ->
                onState(state)
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun onDestroy() {
        super.onDestroy()
        unsubscribe()
    }

    override fun onItemClick(tag: KnownTag) {
        pairButton.visibility = View.VISIBLE
        scanButton.visibility = View.GONE
        stopScanButton.visibility = View.GONE
        viewModel.setSelected(tag)
        adapter.notifyDataSetChanged()
    }

    private fun initView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.tag_recyclerview)
        scanButton = view.findViewById(R.id.scan_button)
        tagIndicatorIcon = view.findViewById(R.id.tag_indicator_icon)
        description = view.findViewById(R.id.description)
        stopScanButton = view.findViewById(R.id.stop_scan_button)
        pairButton = view.findViewById(R.id.pair_button)
        progressBarHolder = view.findViewById(R.id.progress_bar_holder)
        progressBar = view.findViewById(R.id.progress_bar)
        pairedIcon = view.findViewById(R.id.paired_icon)
        loadingTitle = view.findViewById(R.id.loading_title)

        recyclerView.adapter = adapter
        scanButton.setOnClickListener { if (canStartScanning()) startScan() }
        stopScanButton.setOnClickListener { stopScan() }
        pairButton.setOnClickListener {
            changeStatusBarColor(R.color.progress_overlay)
            viewModel.connect(requireActivity()) { intentSender ->
                (requireActivity() as MainActivity)
                    .startForResult(intentSender, MainActivity.COMPANION_DEVICE_REQUEST)
                    .map { result -> result.resultCode == RESULT_OK }
            }
        }
    }

    private fun startScan() {
        initScanningViews()
        startTimer()
        scanSubscription?.unsubscribe()
        subscriptions.remove(scanSubscription)
        scanSubscription = viewModel.startScanning().onNext { tags ->
            PrintLogger.d(TAG, "Got tags: ${tags.size}")
            adapter.submitList(tags)
        }
        subscriptions.add(scanSubscription)
    }

    private fun initScanView() {
        scanButton.text = getString(R.string.scan_page_scan_button)
        scanButton.isEnabled = true
        stopScanButton.visibility = View.GONE
        tagIndicatorIcon.visibility = View.VISIBLE
        description.text = getString(R.string.scan_page_charge_your_tag_desc)
    }

    private fun initScanningViews() {
        scanButton.text = getString(R.string.scan_page_scanning_button)
        scanButton.isEnabled = false
        stopScanButton.visibility = View.VISIBLE
        tagIndicatorIcon.visibility = View.GONE
        description.text = getString(R.string.scan_page_match_last_four_digits_desc)
    }

    private fun initTryAgainView() {
        scanButton.text = getString(R.string.scan_page_try_again_button)
        scanButton.isEnabled = true
        stopScanButton.visibility = View.GONE
        tagIndicatorIcon.visibility = View.VISIBLE
        description.text = getString(R.string.scan_page_charge_your_tag_desc)
    }

    private fun startTimer() {
        tryAgainSubscription = Signal.from(1)
            .delay(SCAN_TIMEOUT.toLong())
            .onNext { initTryAgainView() }
        subscriptions.add(tryAgainSubscription)
    }

    private fun stopTimer() {
        tryAgainSubscription?.unsubscribe()
        subscriptions.remove(tryAgainSubscription)
    }

    private fun stopScan() {
        stopTimer()
        initScanView()
        scanSubscription?.unsubscribe()
        subscriptions.remove(scanSubscription)
    }

    private fun onConnectedState() {
        PrintLogger.d(TAG, "Tag connected")
        lifecycleScope.launchWhenResumed {
            progressBar.visibility = View.GONE
            pairedIcon.visibility = View.VISIBLE
            loadingTitle.text = getString(R.string.scan_page_paired_button)
            subscriptions.add(
                Signal.from<Boolean>(isUserOnboarded()).delay( /* delayInMillis= */2000)
                    .onNext { isOnBoarded ->
                        viewModel.successfullyConnected(isOnBoarded)
                        changeStatusBarColor(R.color.white)
                    })
        }
    }

    private fun onFirmwareUpdateInitiated() {
        PrintLogger.d(TAG, "Tag onFirmwareUpdateInitiated")
        progressBarHolder.visibility = View.GONE
        createFirmwareUpdateDialog()
    }

    private fun createFirmwareUpdateDialog() {
        if (isResumed) {
            firmwareUpdateDialog = DFUUtil.showApplyFirmwareProgress(parentFragmentManager)
        }
    }

    private fun onFirmwareTransferring(progress: Int) {
        PrintLogger.d(TAG, "Tag onFirmwareTransferring $progress")
        if (firmwareUpdateDialog == null) {
            createFirmwareUpdateDialog()
        }
        firmwareUpdateDialog?.updateProgress(progress)
    }

    private fun onFirmwareTransferComplete() {
        PrintLogger.d(TAG, "Tag onFirmwareTransferComplete")
        dismissFirmwareUpdate()
    }

    private fun onFirmwareExecuting() {
        PrintLogger.d(TAG, "Tag onFirmwareExecuting")
        showProgressLoader(R.string.execute_updates_progress)
    }

    private fun onState(state: State) {
        when (state.getType()) {
            State.Type.CONNECTING -> progressBarHolder.visibility = View.VISIBLE
            State.Type.CONNECTED -> onConnectedState()
            State.Type.FIRMWARE_UPDATE_INITIATED -> onFirmwareUpdateInitiated()
            State.Type.FIRMWARE_TRANSFERRING -> onFirmwareTransferring(state.firmwareTransferring())
            State.Type.FIRMWARE_TRANSFER_COMPLETE -> onFirmwareTransferComplete()
            State.Type.FIRMWARE_EXECUTING -> onFirmwareExecuting()
            State.Type.ERROR -> {
                dismissFirmwareUpdate()
                progressBarHolder.visibility = View.GONE
                changeStatusBarColor(R.color.white)
                Signal.just(1).delay(500).onNext { Util.showSnackBar(view, state.error()) }
            }
        }
    }

    private fun unsubscribe() {
        for (subscription in subscriptions) {
            subscription?.unsubscribe()
        }
        subscriptions.clear()
    }

    private fun canStartScanning(): Boolean {
        return if (!hasBluetoothEnabled() || !hasPermissions()) false else hasLocationEnabled()
    }

    private fun hasPermissions(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                requireActivity(),
                permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) true else {
            requestPermissionLauncher.launch(permission.ACCESS_FINE_LOCATION)
            false
        }
    }

    private fun shouldShowRequestPermissionRationale() {
        if (!shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
            PrintLogger.d(TAG, "need to show permission rational.")
            Util.showSnackBar(view, getString(R.string.scan_page_location_permission_msg))
        }
    }

    private fun hasBluetoothEnabled(): Boolean {
        return if (BluetoothAdapter.getDefaultAdapter().isEnabled)
            true
        else {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            false
        }
    }

    private fun hasLocationEnabled(): Boolean {
        return if (LocationManagerCompat.isLocationEnabled(
                requireContext()
                    .getSystemService(Context.LOCATION_SERVICE) as LocationManager
            )
        ) true
        else {
            enableLocationLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            false
        }
    }

    private fun isUserOnboarded(): Boolean {
        return ScanFragmentArgs.fromBundle(requireArguments()).isUserOnboarded
    }

    private fun changeStatusBarColor(@ColorRes color: Int) {
        ContextCompat.getColor(requireContext(), color)
            .also { requireActivity().window.statusBarColor = it }
    }

    private fun showProgressLoader(loaderMessageId: Int) {
        PrintLogger.d(TAG, "showProgressLoader")
        progressBarHolder.visibility = View.VISIBLE
        loadingTitle.setText(loaderMessageId)
    }

    private fun dismissFirmwareUpdate() {
        PrintLogger.d(TAG, "dismissFirmwareUpdate")
        lifecycleScope.launchWhenResumed {
            firmwareUpdateDialog?.dismiss()
            firmwareUpdateDialog = null
        }
    }
}