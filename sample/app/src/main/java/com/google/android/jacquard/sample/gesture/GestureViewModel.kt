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
package com.google.android.jacquard.sample.gesture

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.Preferences
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sdk.command.GestureNotificationSubscription
import com.google.android.jacquard.sdk.model.Gesture
import com.google.android.jacquard.sdk.model.Gesture.GestureType
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag
import java.util.*
import kotlin.collections.ArrayList

/** ViewModel for [GestureFragment].  */
class GestureViewModel(
    private val connectivityManager: ConnectivityManager,
    private val navController: NavController, private val preferences: Preferences
) : ViewModel() {

    private val gestures: MutableList<GestureViewItem> = mutableListOf()
    private lateinit var tag: Signal<ConnectedJacquardTag>

    /**
     * Initialize view model, It should be called from onViewCreated.
     */
    fun init() {
        tag = preferences.currentTag?.let {
            connectivityManager.getConnectedJacquardTag(it.address())
        } ?: Signal.empty()
    }

    /** Emits a list of all performed gestures when a gesture is detected.mutableListOf  */
    fun getGestures(): Signal<List<GestureViewItem>> {
        return tag.distinctUntilChanged()
            .switchMap { tag -> tag.subscribe(GestureNotificationSubscription()) }
            .scan(
                gestures,
                { _: List<GestureViewItem>, gesture: Gesture ->
                    val item =
                        GestureViewItem(System.nanoTime(), gesture, getDrawableResId(gesture))
                    gestures.add(0, item)
                    ArrayList(gestures)
                })
    }

    /**
     * Emits connectivity events [Events].
     */
    val connectivityEvents: Signal<ConnectivityManager.Events>
        get() = preferences.currentTag?.let {
            connectivityManager.getEventsSignal(it.address())
                .distinctUntilChanged()
        } ?: Signal.empty()

    private fun getDrawableResId(gesture: Gesture): Int {
        return when (gesture.gestureType()) {
            GestureType.DOUBLE_TAP -> R.drawable.ic_double_tap
            GestureType.BRUSH_OUT -> R.drawable.ic_brush_out
            GestureType.SHORT_COVER -> R.drawable.ic_cover
            GestureType.BRUSH_IN -> R.drawable.ic_brush_in
            else -> R.drawable.ic_brush_in
        }
    }

    /** Called when the info button is clicked.  */
    fun gestureInfoClick() {
        navController.navigate(R.id.action_gestureFragment_to_gestureInfoFragment)
    }

    /** Called when the up button is clicked.  */
    fun upClick() {
        navController.popBackStack()
    }

    /** Return the tooltip string resId for first application run and null on subsequent runs.  */
    @get:StringRes
    val infoButtonToolTip: Int?
        get() = if (preferences.isFirstRun) R.string.gesture_info_tooltip else null

    /** Data class consumed by [GestureAdapter].  */
    data class GestureViewItem(val id:Long, val gesture:Gesture, val drawableResId:Int)
}