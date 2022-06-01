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
package com.google.android.jacquard.sample.home

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.Preferences
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.firmwareupdate.FirmwareManager
import com.google.android.jacquard.sample.utilities.AbilityConstants
import com.google.android.jacquard.sample.utilities.RecipeManager
import com.google.android.jacquard.sdk.command.BatteryStatus
import com.google.android.jacquard.sdk.command.BatteryStatusCommand
import com.google.android.jacquard.sdk.command.BatteryStatusNotificationSubscription
import com.google.android.jacquard.sdk.connection.ConnectionState
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.model.GearState
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.rx.Signal.ObservesNext
import com.google.android.jacquard.sdk.rx.Signal.empty
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag
import com.google.auto.value.AutoOneOf
import java.util.*

/** View model for [HomeFragment].  */
class HomeViewModel(
    private val preferences: Preferences,
    private val navController: NavController,
    private val connectivityManager: ConnectivityManager,
    private val recipeManager: RecipeManager,
    private val firmwareManager: FirmwareManager,
    private val resources: Resources
) : ViewModel(), ItemClickListener<HomeTileModel> {

    companion object {
        private val TAG = HomeViewModel::class.java.simpleName
    }

    /** Parameter for setting Adapter.*/
    data class ParamsAdapter(
        val itemClickListener: ItemClickListener<HomeTileModel>,
        val listForAdapter: List<HomeTileModel>
    )

    /** State for [HomeViewModel].  */
    @AutoOneOf(State.Type::class)
    abstract class State {
        /** Returns the Adapter for recyclerview.  */
        abstract fun adapter(): ParamsAdapter

        /** Send connected state.  */
        abstract fun connected(): ConnectedJacquardTag

        /** Send disconneted state.  */
        abstract fun disconnected()

        /** Send error state.  */
        abstract fun error(): String

        /** Returns the type of Navigation.  */
        abstract val type: Type

        /** Type of [State].  */
        enum class Type {
            ADAPTER, CONNECTED, DISCONNECTED, ERROR
        }

        companion object {
            /** Returns the State for set adapter.  */
            fun ofSetAdapter(paramsAdapter: ParamsAdapter): State {
                return AutoOneOf_HomeViewModel_State.adapter(paramsAdapter)
            }

            /** Returns the Connected state.  */
            fun ofConnected(tag: ConnectedJacquardTag): State {
                return AutoOneOf_HomeViewModel_State.connected(tag)
            }

            /** Returns the Disconnected state.  */
            fun ofDisconnected(): State {
                return AutoOneOf_HomeViewModel_State.disconnected()
            }

            /** Returns the Error state.  */
            fun ofError(error: String): State {
                return AutoOneOf_HomeViewModel_State.error(error)
            }
        }
    }

    /** Notification emitted by this view model.  */
    @AutoOneOf(Notification.Type::class)
    abstract class Notification {
        abstract val type: Type

        /** Returns the current battery status.  */
        abstract fun battery(): BatteryStatus

        /** Returns the state of the gear.  */
        abstract fun gear(): GearState
        enum class Type {
            BATTERY, GEAR
        }

        companion object {
            fun ofBattery(batteryStatus: BatteryStatus): Notification {
                return AutoOneOf_HomeViewModel_Notification.battery(batteryStatus)
            }

            fun ofGear(gearState: GearState): Notification {
                return AutoOneOf_HomeViewModel_Notification.gear(gearState)
            }
        }
    }

    private val subscriptions: MutableList<Signal.Subscription> = mutableListOf()
    private lateinit var connectionStateSignal: Signal<ConnectionState>
    private lateinit var connectedJacquardTagSignal: Signal<ConnectedJacquardTag>

    /** Signal State for [HomeViewModel]  */
    @JvmField
    val stateSignal: Signal<State> = Signal.create()

    /** Initialize view model, It should be called from onViewCreated.  */
    fun init() {
        onCleared()
        val currentTag = preferences.currentTag!!
        connectionStateSignal = connectivityManager.getConnectionStateSignal(currentTag.address())
        connectedJacquardTagSignal =
            connectivityManager.getConnectedJacquardTag(currentTag.address())
        onTagDisconnected()
        preferences.isHomeLoaded = true
        subscribeConnectionState()
    }

    /** Connects to the Notification for the Tag.  */
    val notification: Signal<Notification>
        get() = connectedJacquardTagSignal
            .distinctUntilChanged()
            .switchMap { tag ->
                Signal.merge(
                    listOf(getBatteryNotifications(tag), getGearNotifications(tag))
                )
            }

    /** Click of the Item from Recyclerview Tile.  */
    override fun itemClick(t: HomeTileModel) {
        onTileClick(t.title)
    }

    /** Triggers state signal with data for Connected Tag.  */
    fun onTagConnected() {
        stateSignal.next(
            State.ofSetAdapter(
                ParamsAdapter( /* itemClick= */
                    this,
                    getHomeTilesList(isTagConnected = true, isAttached = false)
                )
            )
        )
    }

    /** Triggers state signal with data for Attached Gear.  */
    fun onTagAttached() {
        stateSignal.next(
            State.ofSetAdapter(
                ParamsAdapter( /* itemClick= */
                    this,
                    getHomeTilesList(isTagConnected = true, isAttached = true)
                )
            )
        )
    }

    /** Triggers state signal with data for disconnected Tag.  */
    fun onTagDisconnected() {
        stateSignal.next(
            State.ofSetAdapter(
                ParamsAdapter( /* itemClick= */
                    this,
                    getHomeTilesList(isTagConnected = false, isAttached = false)
                )
            )
        )
    }

    /** Returns the current tag name.  */
    val tagName: Signal<String>
        get() = connectedJacquardTagSignal
            .distinctUntilChanged()
            .switchMap { tag -> tag.customAdvName }

    /** Returns the BatteryStatus signal of current selected tag.  */
    val batteryStatus: Signal<BatteryStatus>
        get() = connectedJacquardTagSignal
            .distinctUntilChanged()
            .flatMap { tag -> tag.enqueue(BatteryStatusCommand()) }

    /** Emits connectivity events [Events].  */
    val connectivityEvents: Signal<ConnectivityManager.Events>
        get() = preferences.currentTag?.let {
            connectivityManager
                .getEventsSignal(it.address())
                .distinctUntilChanged()
        } ?: empty()
    val state: Signal<FirmwareUpdateState>
        get() =
            preferences.currentTag?.let {
                firmwareManager.getState(it.address())
            } ?: empty()

    /** Clearance operation for ViewModel.  */
    override fun onCleared() {
        for (subscription in subscriptions) {
            subscription.unsubscribe()
        }
        subscriptions.clear()
        super.onCleared()
    }

    /** Navigates to [LedPatternFragment].  */
    private fun ledPatternClick() {
        navController.navigate(R.id.action_homeFragment_to_ledPatternFragment)
    }

    /** Navigates to [MusicalThreadsFragment].  */
    private fun musicalThreadsButtonClick() {
        navController.navigate(R.id.action_homeFragment_to_musicalThreadsFragment)
    }

    /** Navigates to [GestureFragment].  */
    private fun gestureButtonClick() {
        navController.navigate(R.id.action_homeFragment_to_gestureFragment)
    }

    /** Navigates to [HapticsFragment].  */
    private fun hapticsClick() {
        navController.navigate(R.id.action_homeFragment_to_hapticsFragment)
    }

    /** Navigates to [TagManagerFragment].  */
    private fun tagManagerClick() {
        if (preferences.isDfuInProgress) {
            onErrorState(resources.getString(R.string.feature_will_not_work))
            return
        }
        navController.navigate(R.id.action_homeFragment_to_tagManagerFragment)
    }

    /** Navigates to [TagManagerFragment].  */
    private fun capVisualiserClick() {
        navController.navigate(R.id.action_homeFragment_to_touchYourThreadsFragment)
    }

    /** Navigates to [RenameTagFragment].  */
    private fun renameClick() {
        if (preferences.isDfuInProgress) {
            onErrorState(resources.getString(R.string.feature_will_not_work))
            return
        }
        navController.navigate(R.id.action_homeFragment_to_renameTagFragment)
    }

    /** Navigates to [FirmwareUpdateFragment].  */
    private fun firmwareUpdateClick() {
        navController.navigate(R.id.action_homeFragment_to_firmwareUpdateFragment)
    }

    /** Navigates to [PlacesConfigFragment].  */
    private fun placesConfigClick() {
        val recipe = recipeManager.getRecipeForAbility(AbilityConstants.PLACES_ABILITY)
        if (recipe != null) {
            PrintLogger.d(TAG, "Places ability is already assigned.")
            navController.navigate(
                HomeFragmentDirections.actionHomeFragmentToPlacesListFragment(recipe.gestureId)
            )
        } else {
            navController.navigate(R.id.action_homeFragment_to_placesConfigFragment)
        }
    }

    private fun onTileClick(tileTitleResourceId: Int) {
        when (tileTitleResourceId) {
            R.string.tile_leds_title -> {
                ledPatternClick()
            }
            R.string.tile_musical_threads_title -> {
                musicalThreadsButtonClick()
            }
            R.string.tile_gesture_title -> {
                gestureButtonClick()
            }
            R.string.tile_haptics_title -> {
                hapticsClick()
            }
            R.string.tile_tagmanager_title -> {
                tagManagerClick()
            }
            R.string.tile_cap_visualizer_title -> {
                capVisualiserClick()
            }
            R.string.tile_rename_tag_title -> {
                renameClick()
            }
            R.string.tile_firmware_updates_title -> {
                firmwareUpdateClick()
            }
            R.string.places_title -> {
                placesConfigClick()
            }
            R.string.imu_tile_title -> {
                if (preferences.isDfuInProgress) {
                    onErrorState(resources.getString(R.string.feature_will_not_work))
                    return
                }
                navController.navigate(R.id.action_homeFragment_to_imuFragment)
            }
            R.string.imu_rtstream_title -> {
                if (preferences.isDfuInProgress) {
                    onErrorState(resources.getString(R.string.feature_will_not_work))
                    return
                }
                navController.navigate(R.id.action_homeFragment_to_imuStreamingFragment)
            }
        }
    }

    private fun getBatteryNotifications(tag: ConnectedJacquardTag): Signal<Notification> {
        return tag.subscribe(BatteryStatusNotificationSubscription())
            .map { batteryStatus -> Notification.ofBattery(batteryStatus) }
    }

    private fun getGearNotifications(tag: ConnectedJacquardTag): Signal<Notification> {
        return tag.connectedGearSignal
            .map { gearState ->
                PrintLogger.d(TAG, "## GearNotifications GearState: $gearState")
                Notification.ofGear(gearState)
            }
            .tapError { error ->
                PrintLogger.d(
                    TAG,
                    "## GearNotifications error: " + error.message
                )
            }
    }

    private fun getHomeTilesList(
        isTagConnected: Boolean,
        isAttached: Boolean
    ): List<HomeTileModel> {
        return listOf(
            HomeTileModel(
                R.string.section_api,  /* subTitle= */
                0,
                HomeTileModel.Type.SECTION,  /* enabled= */
                true
            ),
            HomeTileModel(
                R.string.tile_gesture_title,
                R.string.tile_gesture_subtitle,
                HomeTileModel.Type.TILE_API,  /* enabled= */
                isAttached
            ),
            HomeTileModel(
                R.string.tile_haptics_title,
                R.string.tile_haptics_subtitle,
                HomeTileModel.Type.TILE_API,  /* enabled= */
                isAttached
            ),
            HomeTileModel(
                R.string.tile_leds_title,
                R.string.tile_leds_subtitle,
                HomeTileModel.Type.TILE_API,  /* enabled= */
                isTagConnected
            ),
            HomeTileModel(
                R.string.tile_cap_visualizer_title,
                R.string.tile_cap_visualizer_subtitle,
                HomeTileModel.Type.TILE_API,  /* enabled= */
                isAttached
            ),
            HomeTileModel(
                R.string.tile_rename_tag_title,
                R.string.tile_rename_tag_subtitle,
                HomeTileModel.Type.TILE_API,  /* enabled= */
                isTagConnected
            ),
            HomeTileModel(
                R.string.tile_tagmanager_title,
                R.string.tile_tagmanager_subtitle,
                HomeTileModel.Type.TILE_API,  /* enabled= */
                true
            ),
            HomeTileModel(
                R.string.tile_firmware_updates_title,
                R.string.tile_firmware_updates_subtitle,
                HomeTileModel.Type.TILE_API,  /* enabled= */
                isTagConnected
            ),
            HomeTileModel(
                R.string.imu_tile_title,  /* subTitle= */
                R.string.imu_title_subtitle,
                HomeTileModel.Type.TILE_API,  /* enabled= */
                isTagConnected
            ),
            HomeTileModel(
                R.string.imu_rtstream_title,  /* subTitle= */
                R.string.imu_rtstream_title,
                HomeTileModel.Type.TILE_API,  /* enabled= */
                isTagConnected
            ),
            HomeTileModel(
                R.string.section_sample_use_cases,  /* subTitle= */
                0,
                HomeTileModel.Type.SECTION,  /* enabled= */
                true
            ),
            HomeTileModel(
                R.string.tile_musical_threads_title,  /* subTitle= */
                0,
                HomeTileModel.Type.TILE_SAMPLE_USE_CASE,  /* enabled= */
                isAttached
            ),
            HomeTileModel(
                R.string.places_title,  /* subTitle= */
                0,
                HomeTileModel.Type.TILE_SAMPLE_USE_CASE,  /* enabled= */
                isAttached
            )
        )
    }

    private fun subscribeConnectionState() {
        subscriptions.add(
            connectionStateSignal
                .tap { state ->
                    PrintLogger.d(
                        TAG, "HomeViewModel # subscribeConnectionState # state -> $state"
                    )
                }
                .distinctUntilChanged()
                .observe(
                    object : ObservesNext<ConnectionState>() {
                        override fun onNext(connectionState: ConnectionState) {
                            onConnectionState(connectionState)
                        }

                        override fun onError(t: Throwable) {
                            onErrorState(t.message)
                        }
                    })
        )
    }

    private fun onConnectionState(connectionState: ConnectionState) {
        PrintLogger.d(TAG, "onConnectionState: $connectionState")
        if (connectionState.isType(ConnectionState.Type.CONNECTED)) {
            stateSignal.next(State.ofConnected(connectionState.connected()))
        } else if (connectionState.isType(ConnectionState.Type.PREPARING_TO_CONNECT)
            || connectionState.isType(ConnectionState.Type.DISCONNECTED)
        ) {
            stateSignal.next(State.ofDisconnected())
        }
    }

    private fun onErrorState(message: String?) {
        PrintLogger.d(TAG,  /* message= */"Failed to connect : $message")
        message?.let { stateSignal.next(State.ofError(it)) }
    }
}