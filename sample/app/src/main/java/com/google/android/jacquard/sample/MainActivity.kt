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
package com.google.android.jacquard.sample

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.IntentSender.SendIntentException
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.jacquard.sample.firmwareupdate.ApplyFirmwareListener
import com.google.android.jacquard.sample.fragment.extensions.activityViewModels
import com.google.android.jacquard.sample.utilities.ShakeDetector
import com.google.android.jacquard.sample.utilities.Util
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState.Type.COMPLETED
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState.Type.ERROR
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState.Type.EXECUTING
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState.Type.IDLE
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState.Type.PREPARING_TO_TRANSFER
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState.Type.TRANSFERRED
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState.Type.TRANSFER_PROGRESS
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Signal

/**
 * MainActivity for all fragments.
 */
class MainActivity : AppCompatActivity(), ShakeDetector.ShakeListener, ApplyFirmwareListener {
    companion object {
        const val COMPANION_DEVICE_REQUEST = 3
    }

    private val TAG = MainActivity::class.java.simpleName
    private val bluetoothReceiver = BluetoothStateChangeReceiver()
    private val viewModel by lazy { activityViewModels<MainActivityViewModel>() }
    private val activityResults: Signal<ActivityResult> = Signal.create()
    private val shakeDetector by lazy { ShakeDetector(baseContext) }
    private var fwUpdateStateSubscription: Signal.Subscription? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrintLogger.d(
            TAG,
            "onCreate # App Version ${BuildConfig.VERSION_NAME} ${BuildConfig.VERSION_CODE}"
        )
        setContentView(R.layout.activity_main)
        registerBluetoothReceiver()
        firmwareStateListener()
    }

    override fun onResume() {
        super.onResume()
        shakeDetector.addShakeDetector( /* shakeListener = */this)
    }

    override fun onPause() {
        super.onPause()
        shakeDetector.removeShakeDetector()
    }

    override fun onDestroy() {
        unregisterReceiver(bluetoothReceiver)
        super.onDestroy()
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothReceiver, filter)
    }

    /**
     * Starts an [IntentSender] for result, adapted to work within a [Signal] framework.
     */
    fun startForResult(intentSender: IntentSender?, requestCode: Int): Signal<ActivityResult> {
        try {
            startIntentSenderForResult(intentSender, requestCode, null, 0, 0, 0)
        } catch (e: SendIntentException) {
            PrintLogger.e(TAG, "failed to launch intent sender: " + e.message, e)
            return Signal.empty(e)
        }
        return activityResults.filter { a: ActivityResult -> a.requestCode() == requestCode }
            .first()
    }

    @RequiresApi(api = VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        activityResults.next(ActivityResult.create(requestCode, resultCode, data))
    }

    override fun onShakeDetected() {
        val fileUri = FileProvider
            .getUriForFile(
                this,
                "$packageName.provider",
                PrintLogger.getLogFile(applicationContext)
            )
        val sendIntent = Intent()
        sendIntent.apply {
            action = Intent.ACTION_SEND
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_log_file)))
    }

    override fun applyFirmwareInitiated() {
        firmwareStateListener()
    }

    private fun showSnackbar(message: String) {
        Util.showSnackBar(findViewById(android.R.id.content), message)
    }

    private fun firmwareStateListener() {
        fwUpdateStateSubscription?.apply { unsubscribe() }
        fwUpdateStateSubscription = viewModel.state.tapError { error ->
            PrintLogger.d(TAG, "Main Activity state error : $error")
            viewModel.removeFlagDfuInProgress()
            error.message?.let { showSnackbar(it) }
        }.onNext { state ->
            PrintLogger.d(TAG, "Main Activity : $state")
            when(state.type) {
                ERROR -> {
                    viewModel.removeFlagDfuInProgress()
                    state.error()?.apply {
                        message?.let { showSnackbar(it) }
                    }
                }
                PREPARING_TO_TRANSFER -> {
                    PrintLogger.d(TAG, "Reset AlmostReady dialog preference")
                    viewModel.removeFlagDfuInProgress()
                    viewModel.resetAlmostReadyShown()
                }
                TRANSFERRED -> {
                    if (viewModel.isAutoUpdate) {
                        showSnackbar(getString(R.string.execute_updates_progress))
                    } else {
                        viewModel.removeAutoUpdate()
                        viewModel.removeFlagDfuInProgress()
                        showSnackbar(getString(R.string.continue_execute))
                    }
                }
                EXECUTING -> {
                    PrintLogger.d(TAG, "Tag Reboot")
                    viewModel.removeAutoUpdate()
                    viewModel.removeFlagDfuInProgress()
                    showSnackbar(getString(R.string.tag_reboot))
                }
                COMPLETED -> {
                    PrintLogger.d(TAG, "Update Complete")
                    showSnackbar(getString(R.string.firmware_upload_complete))
                }
                TRANSFER_PROGRESS -> viewModel.saveDFUInProgress()
                IDLE -> viewModel.removeFlagDfuInProgress()
            }
        }
    }
}