/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sample.musicalthreads

import android.bluetooth.BluetoothGatt
import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.ConnectivityManager.Events
import com.google.android.jacquard.sample.Preferences
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.musicalthreads.player.ThreadsPlayer
import com.google.android.jacquard.sdk.command.ContinuousTouchNotificationSubscription
import com.google.android.jacquard.sdk.model.GearState
import com.google.android.jacquard.sdk.model.TouchMode
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag

/** View model for the musical threads feature, Fragment [MusicalThreadsFragment]. */
class MusicalThreadsViewModel(
    private val connectivityManager: ConnectivityManager,
    private val threadsPlayer: ThreadsPlayer,
    private val navController: NavController,
    private val preferences: Preferences,
    private val resource: Resources
) : ViewModel() {

    companion object {
        private const val THREAD_COUNT = 12;
    }

    lateinit var connectedJacquardTagSignal: Signal<ConnectedJacquardTag>

    /** Initialize view model, It should be called from onViewCreated. */
    fun init() {
        connectedJacquardTagSignal = preferences.currentTag?.let {
            connectivityManager.getConnectedJacquardTag(it.address())
        } ?: Signal.empty()
        threadsPlayer.init()
    }

    override fun onCleared() {
        threadsPlayer.destroy()
        super.onCleared()
    }

    /** Starts the touch stream. */
    fun startTouchStream(): Signal<List<Int>> {
        return Signal.create { signal ->
            val subscription =
                getTouchData().tap { lines -> threadsPlayer.play(lines) }.forward(signal)
            object : Signal.Subscription() {
                override fun onUnsubscribe() {
                    requestNormalConnectionPriority()
                    subscription.unsubscribe()
                    setGestureTouchMode().consume()
                    signal.complete()
                }
            }
        }
    }

    /** Emits connectivity events [Events] */
    fun getConnectivityEvents(): Signal<Events> {
        return preferences.currentTag?.let {
            connectivityManager.getEventsSignal(it.address())
                .distinctUntilChanged()
        } ?: Signal.empty()
    }

    /** Returns the number of supported touch thread. */
    fun getThreadCount(): Int {
        return THREAD_COUNT
    }

    /** Called when the back arrow in the toolbar is clicked. */
    fun backArrowClick() {
        navController.popBackStack()
    }

    private fun setGestureTouchMode(): Signal<Boolean> {
        return connectedJacquardTagSignal.flatMap { tag ->
            tag.connectedGearSignal.map { gearState ->
                Pair(
                    tag,
                    gearState
                )
            }
        }
            .first()
            .filter { pair -> pair.second.type.equals(GearState.Type.ATTACHED) }
            .flatMap { pair -> pair.first.setTouchMode(pair.second.attached(), TouchMode.GESTURE) }
    }

    private fun requestNormalConnectionPriority() {
        connectedJacquardTagSignal.first()
            .onNext { tag -> tag.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED) }
    }

    /** Emits line data from the gear or a list of 0's when the gear is detached. */
    private fun getTouchData(): Signal<List<Int>> {
        return Signal.merge(getTouchLinesFromGear(), getTouchLinesForDetachedState())
    }

    /** Returns a List touch data from the gear. */
    private fun getTouchLinesFromGear(): Signal<List<Int>> {
        return connectedJacquardTagSignal
            .distinctUntilChanged()
            .switchMap { tag -> tag.connectedGearSignal.map { gearState -> Pair(tag, gearState) } }
            .distinctUntilChanged()
            .filter { pair -> pair.second.type.equals(GearState.Type.ATTACHED) }
            .flatMap { pair ->
                pair.first.setTouchMode(pair.second.attached(), TouchMode.CONTINUOUS)
                    .flatMap { bool ->
                        if (!bool) {
                            return@flatMap Signal.empty(IllegalStateException(resource.getString(R.string.touch_mode_error)))
                        }
                        return@flatMap Signal.just(pair.first)
                    }
            }
            .switchMap { tag ->
                tag.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                return@switchMap tag.subscribe(ContinuousTouchNotificationSubscription())
                    .map { touchData -> touchData.lines() }
            }
    }

    /** Returns a List of 0's when the gear is detached. */
    private fun getTouchLinesForDetachedState(): Signal<List<Int>> {
        return getGearNotification().filter { gearState -> gearState.type.equals(GearState.Type.DETACHED) }
            .map { emptyLines() }
    }

    /** Returns the attached state of the gear. */
    private fun getGearNotification(): Signal<GearState> {
        return connectedJacquardTagSignal.distinctUntilChanged()
            .switchMap { tag -> tag.connectedGearSignal }
    }

    /** Returns a list of 0's. */
    private fun emptyLines(): List<Int> {
        return ArrayList<Int>(THREAD_COUNT).apply {
            repeat(THREAD_COUNT, { index -> add(index) })
        }
    }
}