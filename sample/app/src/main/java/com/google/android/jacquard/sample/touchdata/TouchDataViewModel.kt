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

package com.google.android.jacquard.sample.touchdata

import android.bluetooth.BluetoothGatt
import android.content.res.Resources
import androidx.core.util.Pair
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.ConnectivityManager.Events
import com.google.android.jacquard.sample.Preferences
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sdk.command.ContinuousTouchNotificationSubscription
import com.google.android.jacquard.sdk.model.GearState
import com.google.android.jacquard.sdk.model.TouchMode
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag


class TouchDataViewModel(
    private val connectivityManager: ConnectivityManager,
    private val navController: NavController,
    private val preferences: Preferences,
    private val resources: Resources
) : ViewModel() {

    companion object {
        private val EMPTY_THREADS = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    }

    private lateinit var connectedJacquardTagSignal: Signal<ConnectedJacquardTag>

    /**
     * Initialize view model, It should be called from onViewCreated.
     */
    fun init() {
        connectedJacquardTagSignal = preferences.currentTag?.let {
            connectivityManager
                .getConnectedJacquardTag(it.address())
        } ?: Signal.empty()
    }

    /** Starts the touch stream. */
    fun startTouchStream(): Signal<List<Int>> {
        return Signal.create { signal ->
            val subscription = getTouchData().forward(signal)
            object : Signal.Subscription() {
                override fun onUnsubscribe() {
                    subscription.unsubscribe()
                    requestNormalConnectionPriority()
                    setGestureTouchMode().consume()
                    signal.complete()
                }
            }
        }
    }

    /** Called when the back arrow in the toolbar is clicked.  */
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

    /** Returns the number of touch threads.  */
    fun getThreadCount(): Int {
        return EMPTY_THREADS.size
    }

    private fun setGestureTouchMode(): Signal<Boolean> {
        return connectedJacquardTagSignal
            .flatMap { tag ->
                tag.connectedGearSignal
                    .map { gearState ->
                        Pair.create(
                            tag,
                            gearState
                        )
                    }
            }
            .first()
            .filter { pair -> pair.second.type == GearState.Type.ATTACHED }
            .flatMap { pair ->
                pair.first.setTouchMode(
                    pair.second.attached(),
                    TouchMode.GESTURE
                )
            }
    }

    private fun requestNormalConnectionPriority() {
        connectedJacquardTagSignal.first()
            .onNext { tag ->
                tag.requestConnectionPriority(
                    BluetoothGatt.CONNECTION_PRIORITY_BALANCED
                )
            }
    }


    /** Returns a List touch data from the gear. */
    private fun getTouchLinesFromGear(): Signal<List<Int>> {
        return connectedJacquardTagSignal
            .distinctUntilChanged()
            .switchMap { tag ->
                tag.connectedGearSignal
                    .map<Pair<ConnectedJacquardTag, GearState>> { gearState ->
                        Pair.create(
                            tag,
                            gearState
                        )
                    }
            }
            .distinctUntilChanged()
            .filter { pair -> pair.second.type == GearState.Type.ATTACHED }
            .flatMap { pair ->
                pair.first.setTouchMode(pair.second.attached(), TouchMode.CONTINUOUS)
                    .flatMap { value ->
                        if (!value) {
                            return@flatMap Signal.empty<ConnectedJacquardTag>(
                                IllegalStateException(
                                    resources.getString(R.string.touch_mode_error)
                                )
                            )
                        }
                        Signal.just(pair.first)
                    }
            }
            .switchMap { tag ->
                tag.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                tag.subscribe(
                    ContinuousTouchNotificationSubscription()
                )
                    .map { obj -> obj.lines() }
            }
    }

    /** Emits line data from the gear or a list of 0's when the gear is detached.  */
    private fun getTouchData(): Signal<List<Int>> {
        return Signal.merge(getTouchLinesFromGear(), getTouchLinesForDetachedState())
    }

    private fun getGearNotification(): Signal<GearState> {
        return connectedJacquardTagSignal
            .distinctUntilChanged()
            .switchMap { connectedJacquardTag -> connectedJacquardTag.connectedGearSignal }
    }

    private fun getTouchLinesForDetachedState(): Signal<List<Int>> {
        return getGearNotification()
            .filter { gearState -> gearState.type == GearState.Type.DETACHED }
            .map { EMPTY_THREADS }
    }

}