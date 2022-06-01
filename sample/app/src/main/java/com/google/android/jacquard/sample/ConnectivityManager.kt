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

package com.google.android.jacquard.sample

import android.content.Context
import android.content.IntentSender
import com.google.android.jacquard.sdk.JacquardManager
import com.google.android.jacquard.sdk.connection.ConnectionState
import com.google.android.jacquard.sdk.model.GearState
import com.google.android.jacquard.sdk.model.SdkConfig
import com.google.android.jacquard.sdk.rx.Fn
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.tag.AdvertisedJacquardTag
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag

/** Manager for providing access to the jacquard tag. */
class ConnectivityManager(private val jacquardManager: JacquardManager) {
    companion object {
        private val TAG = ConnectivityManager::class.java.simpleName
    }

    /** Gear and tag events  */
    enum class Events {
        TAG_DISCONNECTED, TAG_DETACHED, TAG_CONNECTED, TAG_ATTACHED
    }

    /**
     * Initialized the sdk with provided SdkConfig object.
     *
     * @param config a [SdkConfig] to initialize the sdk.
     */
    fun init(config: SdkConfig) {
        jacquardManager.init(config)
    }

    /** Starts scanning for jacquard devices.  */
    fun startScanning(): Signal<AdvertisedJacquardTag> {
        return jacquardManager.startScanning()
    }

    /** Emits [Events] for provided address tag paired/unpaired, attached/detached events.  */
    fun getEventsSignal(address: String): Signal<Events> {
        return Signal.merge(
            jacquardManager.getConnectionStateSignal(address)
                .map { state ->
                    if (state.isType(
                            ConnectionState.Type.CONNECTED
                        )
                    ) Events.TAG_CONNECTED else Events.TAG_DISCONNECTED
                },
            getGearNotification(address)
                .map { state ->
                    if (state.type == GearState.Type.ATTACHED)
                        Events.TAG_ATTACHED
                    else Events.TAG_DETACHED
                })
    }

    /**
     * Removes connection references of the tag.
     *
     * @param address of tag.
     */
    fun forget(address: String) {
        jacquardManager.forget(address)
    }

    /** Releases all allocation resources.  */
    fun destroy() {
        jacquardManager.destroy()
    }

    /** Connects to the provided address. */
    fun connect(
        activityContext: Context,
        address: String,
        senderHandler: Fn<IntentSender, Signal<Boolean>>? = null,
    ): Signal<ConnectionState> {
        return jacquardManager.connect(activityContext, address, senderHandler)
    }

    /** Connects to the provided address. */
    fun connect(address: String): Signal<ConnectionState> {
        return jacquardManager.connect(address)
    }

    /**
     * Emits a provided address connectedTag and may emit multiple values so for one-off operations
     * use first() or distinctUntilChange().
     */
    fun getConnectedJacquardTag(address: String): Signal<ConnectedJacquardTag> {
        return getConnectionStateSignal(address)
            .filter { state -> state.isType(ConnectionState.Type.CONNECTED) }
            .map { state -> state.connected() }
    }

    /**
     * Emits a provided address connectedTag or IllegalStateException if connectedTag is not connected.
     */
    fun getConnectedTag(address: String): Signal<ConnectedJacquardTag> {
        return jacquardManager.getConnectedTag(address)
    }

    /**
     * Emits connection state for provided address. Due to the nature of sticky it will emit every
     * time the signal is subscribed so use distinctUntilChange().
     */
    fun getConnectionStateSignal(address: String): Signal<ConnectionState> {
        return jacquardManager.getConnectionStateSignal(address)
    }

    private fun getGearNotification(address: String): Signal<GearState> {
        return getConnectedJacquardTag(address)
            .distinctUntilChanged()
            .switchMap { obj: ConnectedJacquardTag -> obj.connectedGearSignal }
    }
}