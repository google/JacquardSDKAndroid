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
 *
 */

package com.google.android.jacquard.sample.firmwareupdate

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.dialog.DFUUtil
import com.google.android.jacquard.sample.dialog.DefaultDialog
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogButtonClickListener
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sample.utilities.CustomBottomProgress
import com.google.android.jacquard.sdk.dfu.DFUInfo
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState
import com.google.android.jacquard.sdk.dfu.execption.InsufficientBatteryException
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.model.GearState
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag

/** A fragment for tag and gear firmware updated. */
class FirmwareUpdateFragment : Fragment() {
    companion object {
        private val TAG = FirmwareUpdateFragment::class.java.simpleName
    }

    private val dfuSubscriptionList = mutableListOf<Signal.Subscription>()
    private val viewModel by lazy { getViewModel<FirmwareUpdateViewModel>() }
    private var stateSubscription: Signal.Subscription? = null
    private var gearNotificationSub: Signal.Subscription? = null
    private var defaultDialog: DefaultDialog? = null
    private lateinit var navSubscription: Signal.Subscription
    private lateinit var progressBarHolder: View
    private lateinit var productVersionTv: TextView
    private lateinit var tagVersionTv: TextView
    private lateinit var moduleUpdateSw: SwitchCompat
    private lateinit var autoUpdateSw: SwitchCompat
    private lateinit var forceUpdateSw: SwitchCompat
    private lateinit var checkForFirmwareBtn: Button
    private lateinit var viewDownloadProgress: CustomBottomProgress
    private lateinit var applyFirmwareListener: ApplyFirmwareListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_firmware_update, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        navSubscription = viewModel.stateSignal.onNext { onNavigation(it) }
        viewModel.init()
        fwUpdateStateListener()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        applyFirmwareListener = context as ApplyFirmwareListener
    }

    override fun onResume() {
        super.onResume()
        viewModel.popLastKnownState()?.let { state ->
            PrintLogger.d(TAG, "FirmwareUpdate POP STATE: $state")
            if (state.equals(FirmwareUpdateState.Type.ERROR)) {
                firmwareUpdateNegativeUI(state.error())
            } else {
                firmwareUpdatePositiveUI(state)
            }
        }
        fwUpdateStateListener()
    }

    override fun onPause() {
        unSubscribeState()
        super.onPause()
    }

    override fun onDestroyView() {
        unsubscribe()
        super.onDestroyView()
    }

    private fun onNavigation(state: FirmwareUpdateViewModel.State) {
        when (state.type) {
            FirmwareUpdateViewModel.State.Type.CONNECTED -> onConnected(state.connected())
            FirmwareUpdateViewModel.State.Type.DISCONNECTED, FirmwareUpdateViewModel.State.Type.ERROR -> onDisconnected()
        }
    }

    private fun initView(view: View) {
        initToolbar(view)
        progressBarHolder = view.findViewById(R.id.progress_bar_holder)
        productVersionTv = view.findViewById(R.id.product_version_tv)
        tagVersionTv = view.findViewById(R.id.tag_version_tv)
        moduleUpdateSw = view.findViewById(R.id.module_update_sw)
        autoUpdateSw = view.findViewById(R.id.auto_update_sw)
        forceUpdateSw = view.findViewById(R.id.force_update_sw)
        viewDownloadProgress = view.findViewById(R.id.includeLayout)
        checkForFirmwareBtn = view.findViewById(R.id.check_for_firmware_btn)
        handleBackPress(view)
        checkForFirmwareBtn.setOnClickListener { checkFirmware() }
    }

    private fun initToolbar(root: View) {
        root.findViewById<Toolbar>(R.id.toolbar)
            .setNavigationOnClickListener { viewModel.backArrowClick() }
    }

    private fun handleBackPress(view: View) {
        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener { _: View?, keyCode: Int, _: KeyEvent? ->
            keyCode == KeyEvent.KEYCODE_BACK && progressBarHolder.visibility == View.VISIBLE
        }
    }

    private fun onConnected(tag: ConnectedJacquardTag) {
        tagVersionTv.text = tag.tagComponent().version().toString()
        gearNotificationSub = viewModel
            .getGearNotifications(tag)
            .onNext { notification ->
                if (notification.gear().type == GearState.Type.ATTACHED) {
                    productVersionTv.text = tag.gearComponent().version().toString()
                } else {
                    productVersionTv.setText(R.string.not_available)
                }
            }
    }

    private fun onDisconnected() {
        gearNotificationSub?.unsubscribe()
        tagVersionTv.setText(R.string.not_available)
        productVersionTv.setText(R.string.not_available)
    }

    private fun fwUpdateStateListener() {
        PrintLogger.d(
            TAG, "Register FirmwareUpdateState Signal: ${viewModel.getState().hashCode()}"
        )
        unSubscribeState()
        stateSubscription = viewModel
            .getState()
            .tapError { error ->
                PrintLogger.d(TAG, "State Error: $error")
                firmwareUpdateNegativeUI(error)
            }
            .onNext { firmwareUpdateState: FirmwareUpdateState ->
                PrintLogger.d(
                    TAG, "isResumed: $isResumed firmwareUpdateState: $firmwareUpdateState"
                )
                firmwareUpdatePositiveUI(firmwareUpdateState)
            }
    }

    private fun firmwareUpdatePositiveUI(firmwareUpdateState: FirmwareUpdateState) {
        PrintLogger.d(TAG, "applyFirmware firmwareUpdateState : $firmwareUpdateState")
        // Remove the state in case app is in foreground
        viewModel.popLastKnownState()
        when (firmwareUpdateState.type) {
            FirmwareUpdateState.Type.ERROR -> firmwareUpdateNegativeUI(firmwareUpdateState.error())
            FirmwareUpdateState.Type.TRANSFER_PROGRESS -> {
                autoUpdateSw.isChecked = viewModel.isAutoUpdateChecked()
                if (defaultDialog?.isAdded == true) {
                    defaultDialog?.updateProgress(firmwareUpdateState.transferProgress())
                } else {
                    showBottomDownloadProgress(firmwareUpdateState.transferProgress())
                }
            }
            FirmwareUpdateState.Type.TRANSFERRED -> {
                hideDefaultDialog(defaultDialog)
                hideBottomDownloadProgress()
                if (autoUpdateSw.isChecked) {
                    showProgressLoader(R.string.execute_updates_progress)
                } else {
                    showAlmostReadyDialog()
                }
            }
            FirmwareUpdateState.Type.COMPLETED -> {
                hideProgressLoader()
                hideDefaultDialog(defaultDialog)
                DFUUtil.showUpdateCompleteDialog(parentFragmentManager)
            }
            else -> {
                PrintLogger.d(TAG, "applyFirmware firmwareUpdateState else case.")
            }
        }
    }

    private fun showBottomDownloadProgress(progress: Int) {
        viewDownloadProgress.visibility = View.VISIBLE
        viewDownloadProgress.setProgress(progress)
        disable()
    }

    private fun showAlmostReadyDialog() {
        PrintLogger.d(TAG, "showAlmostReadyDialog")
        DFUUtil.showAlmostReadyDialog(
            parentFragmentManager,
            object : DefaultDialogButtonClickListener() {
                override fun buttonClick() {
                    executeFirmware()
                }
            })
    }

    private fun checkFirmware() {
        PrintLogger.d(TAG, "checkFirmware")
        viewModel.removeFlagDfuInProgress()
        showProgressLoader(R.string.checking_for_updates_progress)
        var isModuleUpdate = moduleUpdateSw.isChecked;
        dfuSubscriptionList.add(
            viewModel
                .checkFirmware(forceUpdateSw.isChecked, isModuleUpdate)
                .tapError { error ->
                    PrintLogger.e(TAG, "Fragment checkFirmware() : " + error.message)
                    hideProgressLoader()
                    handleError(error)
                }
                .onNext { listDfu ->
                    hideProgressLoader()
                    if (noUpdateAvailable(listDfu)) {
                        showNoDFUDialog()
                        return@onNext
                    }
                    if (hasAtleastOneMandatory(listDfu)) {
                        showMandatoryDialog()
                        return@onNext
                    }
                    showOptionalDialog()
                })
    }

    private fun noUpdateAvailable(listDfu: List<DFUInfo>): Boolean {
        if (listDfu.isEmpty()) {
            return true
        }
        for (dfuInfo in listDfu) {
            if (dfuInfo.dfuStatus() != DFUInfo.UpgradeStatus.NOT_AVAILABLE) {
                return false
            }
        }
        return true
    }

    private fun hasAtleastOneMandatory(list: List<DFUInfo>): Boolean {
        for (dfuInfo in list) {
            if (dfuInfo.dfuStatus() == DFUInfo.UpgradeStatus.MANDATORY) {
                return true
            }
        }
        return false
    }

    private fun showMandatoryDialog() {
        PrintLogger.d(TAG, "showMandatoryDialog")
        DFUUtil.showMandatoryDialog(
            parentFragmentManager,
            object : DefaultDialogButtonClickListener() {
                override fun buttonClick() {
                    applyFirmware()
                }
            })
    }

    private fun showOptionalDialog() {
        PrintLogger.d(TAG, "showOptionalDialog")
        DFUUtil.showOptionalDialog(
            parentFragmentManager,
            object : DefaultDialogButtonClickListener() {
                override fun buttonClick() {
                    applyFirmware()
                }
            })
    }

    private fun showNoDFUDialog() {
        PrintLogger.d(TAG, "showNoDFUDialog")
        DFUUtil.showNoDFUDialog(parentFragmentManager)
    }

    private fun showApplyFirmwareProgress(): DefaultDialog {
        PrintLogger.d(TAG, "showApplyFirmwareProgress")
        return DFUUtil.showApplyFirmwareProgress(
            parentFragmentManager,
            object : DefaultDialogButtonClickListener() {
                override fun buttonClick() {
                    PrintLogger.d(TAG, "Update progress ok clicked.")
                    showBottomDownloadProgress(0)
                }
            })
    }

    private fun applyFirmware() {
        PrintLogger.d(TAG, "applyFirmware")
        defaultDialog = showApplyFirmwareProgress()
        dfuSubscriptionList.add(
            viewModel.applyFirmware(autoUpdateSw.isChecked).consume()
        )
        applyFirmwareListener.applyFirmwareInitiated()
        fwUpdateStateListener()
    }

    private fun executeFirmware() {
        PrintLogger.d(TAG, "executeFirmware")
        showProgressLoader(R.string.execute_updates_progress)
        dfuSubscriptionList.add(
            viewModel
                .executeFirmware().consume()
        )
    }

    private fun firmwareUpdateNegativeUI(error: Throwable) {
        PrintLogger.e(TAG, "applyFirmware error: ${error.message}")
        // Remove the state in case app is in foreground
        viewModel.popLastKnownState()
        hideDefaultDialog(defaultDialog)
        hideBottomDownloadProgress()
        showErrorDialog(error)
    }

    private fun showErrorDialog(error: Throwable) {
        hideProgressLoader()
        handleError(error)
        dfuUnsubscribe()
    }

    private fun handleError(error: Throwable) {
        PrintLogger.d(TAG, "handleError: $error")
        if (error is InsufficientBatteryException) {
            showErrorDialog(
                R.string.dfu_error_battery_title,
                R.string.dfu_error_battery_subtitle,
                R.string.dfu_error_battery_positive_btn
            )
            return
        }
        showErrorDialog(
            R.string.dfu_error_generic_title,
            R.string.dfu_error_generic_subtitle,
            R.string.dfu_error_generic_positive_btn
        )
    }

    private fun showErrorDialog(title: Int, subTitle: Int, positiveBtn: Int) {
        PrintLogger.d(TAG, "showErrorDialog")
        DFUUtil.showErrorDialog(title, subTitle, positiveBtn, parentFragmentManager)
    }

    private fun showProgressLoader(loaderMessageId: Int) {
        PrintLogger.d(TAG, "showProgressLoader")
        progressBarHolder.visibility = View.VISIBLE
        progressBarHolder.findViewById<TextView>(R.id.progress_message).setText(
            loaderMessageId
        )
    }

    private fun hideProgressLoader() {
        PrintLogger.d(TAG, "hideProgressLoader")
        progressBarHolder.visibility = View.GONE
    }

    private fun hideDefaultDialog(defaultDialog: DefaultDialog?) {
        defaultDialog?.let {
            it.dismiss()
        }
    }

    private fun hideBottomDownloadProgress() {
        if (viewDownloadProgress.visibility != View.VISIBLE) {
            return
        }
        viewDownloadProgress.visibility = View.GONE
        enable()
    }

    private fun enable() {
        autoUpdateSw.isEnabled = true
        forceUpdateSw.isEnabled = true
        checkForFirmwareBtn.isEnabled = true
    }

    private fun disable() {
        autoUpdateSw.isEnabled = false
        forceUpdateSw.isEnabled = false
        checkForFirmwareBtn.isEnabled = false
    }

    private fun unsubscribe() {
        dfuUnsubscribe()
        gearNotificationSub?.unsubscribe()
        navSubscription?.unsubscribe()
    }

    private fun dfuUnsubscribe() {
        for (subscription in dfuSubscriptionList) {
            subscription.unsubscribe()
        }
        dfuSubscriptionList.clear()
    }

    private fun unSubscribeState() {
        stateSubscription?.unsubscribe()
    }
}