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
package com.google.android.jacquard.sample

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.jacquard.sample.firmwareupdate.FirmwareManager
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState
import com.google.android.jacquard.sdk.rx.Signal

/** View model for [MainActivity].  */
class MainActivityViewModel internal constructor(
    private val preferences: Preferences,
    private val firmwareManager: FirmwareManager,
    private val navController: NavController
) : ViewModel() {
    override fun onCleared() {
        preferences.isFirstRun = false
        super.onCleared()
    }

    val state: Signal<FirmwareUpdateState>
        get() = preferences.currentTag?.let {
            firmwareManager.getState(it.address())
        }?:Signal.empty()

    val isAutoUpdate: Boolean
        get() = preferences.isAutoUpdate

    fun removeAutoUpdate() {
        preferences.removeAutoUpdateFlag()
    }

    fun saveDFUInProgress() {
        preferences.flagDfuInProgress()
    }

    fun removeFlagDfuInProgress() {
        preferences.removeFlagDfuInProgress()
    }

    fun navigateToFirmwareUpdate() {
        navController.navigate(R.id.action_mainActivity_to_firmwareUpdateFragment)
    }

    fun isFirmwareUpdateFragment(): Boolean {
        return navController.currentDestination?.let {
            it.id == R.id.firmwareUpdateFragment
        } ?: false
    }
}