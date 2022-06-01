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

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.ConnectivityManager.Events
import com.google.android.jacquard.sample.Preferences
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sdk.command.HapticCommand
import com.google.android.jacquard.sdk.connection.ConnectionState
import com.google.android.jacquard.sdk.model.Component
import com.google.android.jacquard.sdk.model.GearState
import com.google.android.jacquard.sdk.rx.Fn
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag

/** View model class for the [HapticsFragment]. */
class HapticsViewModel(
    private val connectivityManager: ConnectivityManager,
    private val navController: NavController,
    private val resources: Resources,
    private val preferences: Preferences
) : ViewModel() {

    private lateinit var connectionStateSignal: Signal<ConnectionState>
    private lateinit var connectedJacquardTag: Signal<ConnectedJacquardTag>

    /**
     * Initialize view model, It should be called from onViewCreated.
     */
    fun init() {
        preferences.currentTag?.let {
            connectionStateSignal = connectivityManager
                .getConnectionStateSignal(it.address())
            connectedJacquardTag = connectivityManager
                .getConnectedJacquardTag(it.address())
        }
    }

    fun sendHapticRequest(type: HapticPatternType): Signal<Boolean> {
        return getAttachedComponent().first().flatMap { attachedComponent: Component ->
            getConnectedJacquardTag().first()
                .switchMap { connectedJacquardTag: ConnectedJacquardTag ->
                    connectedJacquardTag
                        .enqueue(HapticCommand(type.getFrame(), attachedComponent))
                }
        }
    }

    private fun getAttachedComponent(): Signal<Component> {
        return getConnectionStateSignal().first().flatMap { connectionState: ConnectionState ->
            check(connectionState.isType(ConnectionState.Type.CONNECTED)) { resources.getString(R.string.tag_not_connected) }
            getGearNotification().first()
        }.map { gearState: GearState ->
            check(gearState.type == GearState.Type.ATTACHED) { resources.getString(R.string.gear_detached) }
            gearState.attached()
        }
    }

    /**
     * Handles back arrow in toolbar.
     */
    fun backArrowClick() {
        navController.popBackStack()
    }

    /**
     * Emits connectivity events [Events].
     */
    fun getConnectivityEvents(): Signal<Events> {
        return preferences.currentTag?.let {
            connectivityManager.getEventsSignal(it.address())
                .distinctUntilChanged()
        } ?: Signal.empty()
    }

    private fun getGearNotification(): Signal<GearState> {
        return connectedJacquardTag
            .distinctUntilChanged()
            .switchMap(
                Fn { obj: ConnectedJacquardTag -> obj.connectedGearSignal } as Fn<ConnectedJacquardTag, Signal<GearState>>)
    }

    private fun getConnectionStateSignal(): Signal<ConnectionState> {
        return connectionStateSignal.distinctUntilChanged()
    }

    private fun getConnectedJacquardTag(): Signal<ConnectedJacquardTag> {
        return connectedJacquardTag.distinctUntilChanged()
    }
}