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

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.KnownTag
import com.google.android.jacquard.sample.Preferences
import com.google.android.jacquard.sdk.connection.ConnectionState
import com.google.android.jacquard.sdk.dfu.DFUInfo
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.model.GearState
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.rx.Signal.ObservesNext
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag
import com.google.auto.value.AutoOneOf

/** View model for the [FirmwareUpdateFragment]. */
class FirmwareUpdateViewModel(
    private val firmwareManager: FirmwareManager,
    private val navController: NavController,
    private val connectivityManager: ConnectivityManager,
    private val preferences: Preferences
) : ViewModel() {
    companion object {
        private val TAG = FirmwareUpdateViewModel::class.java.simpleName
    }

    // currentTag should never be empty in this viewmodel.
    val currentTag: KnownTag = preferences.currentTag!!
    val stateSignal: Signal<State> = Signal.create()
    private lateinit var connectionStateSub: Signal.Subscription

    /** State for [FirmwareUpdateViewModel]. */
    @AutoOneOf(State.Type::class)
    abstract class State {
        /** Send connected state. */
        abstract fun connected(): ConnectedJacquardTag

        /** Send disconnected state. */
        abstract fun disconnected()

        /** Send error state. */
        abstract fun error(): String

        /** Returns the type of Navigation. */
        abstract val type: Type

        companion object {
            /** Returns the Connected state. */
            fun ofConnected(tag: ConnectedJacquardTag): State {
                return AutoOneOf_FirmwareUpdateViewModel_State.connected(tag)
            }

            /** Returns the Disconnected state. */
            fun ofDisconnected(): State {
                return AutoOneOf_FirmwareUpdateViewModel_State.disconnected()
            }

            /** Returns the Error state. */
            fun ofError(error: String): State {
                return AutoOneOf_FirmwareUpdateViewModel_State.error(error)
            }
        }

        enum class Type {
            CONNECTED,
            DISCONNECTED,
            ERROR
        }
    }

    /** Notification emitted by this view model. */
    @AutoOneOf(Notification.Type::class)
    abstract class Notification {
        companion object {
            fun ofGear(gearState: GearState): Notification {
                return AutoOneOf_FirmwareUpdateViewModel_Notification.gear(gearState)
            }
        }

        abstract val type: Type

        /** Returns the state of the gear. */
        abstract fun gear(): GearState

        enum class Type {
            GEAR
        }
    }

    /** Initialize view model, It should be called from onViewCreated.  */
    fun init() {
        subscribeConnectionState()
    }

    /** Check for firmware updated of current connect tag.  */
    fun checkFirmware(forceUpdate: Boolean, moduleUpdate: Boolean): Signal<List<DFUInfo>> {
        return firmwareManager.checkFirmware(currentTag.address(), forceUpdate)
            .flatMap { fwList ->
                PrintLogger.d(TAG, "ujt dfu size: " + fwList.size)
                val fullDfuInfo = mutableListOf<DFUInfo>()
                fullDfuInfo.addAll(fwList)
                if (moduleUpdate) {
                    firmwareManager.checkModuleUpdate(currentTag.address(), forceUpdate)
                        .flatMap { moduleList ->
                            PrintLogger.d(TAG, "module dfu size: " + moduleList.size)
                            fullDfuInfo.addAll(moduleList);
                            Signal.just(fullDfuInfo)
                        }
                } else {
                    PrintLogger.d(TAG, "Module update is false")
                    Signal.just(fullDfuInfo)
                }
            }
    }

    /** Apply firmware updated of current connect tag.  */
    fun applyFirmware(autoExecute: Boolean): Signal<FirmwareUpdateState> {
        preferences.autoUpdateFlag(autoExecute)
        return firmwareManager.applyFirmware(currentTag.address(), autoExecute)
    }

    /** Execute firmware updated of current connect tag.  */
    fun executeFirmware(): Signal<FirmwareUpdateState> {
        return firmwareManager.executeFirmware(currentTag.address())
    }

    fun getState(): Signal<FirmwareUpdateState> {
        return firmwareManager.getState(currentTag.address())
    }

    /** Called when the back arrow in the toolbar is clicked.  */
    fun backArrowClick() {
        navController.popBackStack()
    }

    /** Connects to the Notification for the Tag.  */
    fun getGearNotifications(tag: ConnectedJacquardTag): Signal<Notification> {
        return tag.connectedGearSignal.map { gearState ->
            Notification.ofGear(gearState)
        }
    }

    fun isAutoUpdateChecked(): Boolean {
        return preferences.isAutoUpdate
    }

    override fun onCleared() {
        connectionStateSub.unsubscribe()
        super.onCleared()
    }

    fun popLastKnownState(): FirmwareUpdateState? {
        return firmwareManager.popLastKnownState(currentTag.address())
    }

    fun removeFlagDfuInProgress() {
        preferences.removeFlagDfuInProgress()
    }

    private fun subscribeConnectionState() {
        connectionStateSub =
            connectivityManager.getConnectionStateSignal(currentTag.address())
                .distinctUntilChanged()
                .observe(object : ObservesNext<ConnectionState>() {
                    override fun onNext(connectionState: ConnectionState) {
                        onConnectionState(connectionState)
                    }

                    override fun onError(t: Throwable) {
                        t.message?.let { it ->
                            onErrorState(it)
                        }
                    }
                })
    }

    private fun onConnectionState(connectionState: ConnectionState) {
        if (connectionState.isType(ConnectionState.Type.CONNECTED)) {
            stateSignal.next(State.ofConnected(connectionState.connected()))
        } else {
            stateSignal.next(State.ofDisconnected())
        }
    }

    private fun onErrorState(message: String) {
        PrintLogger.d(
            TAG, "Failed to connect : $message"
        )
        stateSignal.next(State.ofError(message))
    }
}