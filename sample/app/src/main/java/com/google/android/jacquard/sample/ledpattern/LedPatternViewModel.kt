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

package com.google.android.jacquard.sample.ledpattern

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.ConnectivityManager.Events
import com.google.android.jacquard.sample.KnownTag
import com.google.android.jacquard.sample.Preferences
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sdk.command.PlayLedPatternCommand.*
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.model.GearState
import com.google.android.jacquard.sdk.model.Product.Capability
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag
import com.google.android.jacquard.sdk.util.StringUtils

/** View model class for the [LedPatternFragment]. */
class LedPatternViewModel(
    private val connectivityManager: ConnectivityManager,
    private val navController: NavController,
    private val stringUtils: StringUtils,
    private val preferences: Preferences
) : ViewModel() {
    companion object {
        private val TAG: String = LedPatternViewModel::class.java.simpleName
    }

    private lateinit var connectedJacquardTag: Signal<ConnectedJacquardTag>

    /**
     * Initialize view model, It should be called from onViewCreated.
     */
    fun init() {
        connectedJacquardTag = preferences.currentTag?.let {
            connectivityManager
                .getConnectedJacquardTag(it.address())
        } ?: Signal.empty()
    }

    /**
     * Gives notification on gear state change.
     */
    fun getGearNotification(): Signal<GearState> {
        return connectedJacquardTag.distinctUntilChanged()
            .switchMap(ConnectedJacquardTag::getConnectedGearSignal)
    }

    /**
     * Returns led patterns list to be played on tag/gear.
     */
    fun getLedPatterns(): MutableList<LedPatternItem> {
        val ledPatternItems = mutableListOf<LedPatternItem>()
        ledPatternItems.add(
            getLedPatternItem(
                R.drawable.ic_blue,
                "Blue Blink",
                listOf(Frame.of(Color.of(0, 0, 255), 1000)),
                resumable = true,
                haltAll = false,
                playType = PlayType.TOGGLE,
                ledPatternType = LedPatternType.PATTERN_TYPE_SINGLE_BLINK
            )
        )
        ledPatternItems.add(
            getLedPatternItem(
                R.drawable.ic_green,
                "Green Blink",
                listOf(Frame.of(Color.of(0, 255, 0), 1000)),
                resumable = true,
                haltAll = false,
                playType = PlayType.TOGGLE,
                ledPatternType = LedPatternType.PATTERN_TYPE_SINGLE_BLINK
            )
        )
        ledPatternItems.add(
            getLedPatternItem(
                R.drawable.ic_pink,
                "Pink Blink",
                listOf(Frame.of(Color.of(255, 102, 178), 1000)),
                resumable = true,
                haltAll = false,
                playType = PlayType.TOGGLE,
                ledPatternType = LedPatternType.PATTERN_TYPE_SINGLE_BLINK
            )
        )
        ledPatternItems.add(
            getLedPatternItem(
                R.drawable.ic_blink,
                "Blink",
                listOf(Frame.of(Color.of(220, 255, 255), 1000)),
                resumable = true,
                haltAll = false,
                playType = PlayType.TOGGLE,
                ledPatternType = LedPatternType.PATTERN_TYPE_SINGLE_BLINK
            )
        )
        ledPatternItems.add(
            getLedPatternItem(
                R.drawable.ic_strobe,
                "Strobe",
                listOf(
                    Frame.of(Color.of(255, 0, 0), 100),
                    Frame.of(Color.of(247, 95, 0), 100),
                    Frame.of(Color.of(255, 204, 0), 100),
                    Frame.of(Color.of(0, 255, 0), 100),
                    Frame.of(Color.of(2, 100, 255), 100),
                    Frame.of(Color.of(255, 0, 255), 100),
                    Frame.of(Color.of(100, 255, 255), 100),
                    Frame.of(Color.of(2, 202, 255), 100),
                    Frame.of(Color.of(255, 0, 173), 100),
                    Frame.of(Color.of(113, 5, 255), 100),
                    Frame.of(Color.of(15, 255, 213), 100)
                ),
                resumable = true,
                haltAll = false,
                playType = PlayType.TOGGLE,
                ledPatternType = LedPatternType.PATTERN_TYPE_CUSTOM
            )
        )
        ledPatternItems.add(
            getLedPatternItem(
                R.drawable.ic_shine,
                "Shine",
                listOf(Frame.of(Color.of(220, 255, 255), 1000)),
                resumable = true,
                haltAll = false,
                playType = PlayType.TOGGLE,
                ledPatternType = LedPatternType.PATTERN_TYPE_SOLID
            )
        )
        ledPatternItems.add(
            getLedPatternItem(
                R.drawable.ic_stop_all,
                "Stop All",
                listOf(Frame.of(Color.of(0, 0, 0), 1000)),
                resumable = false,
                haltAll = true,
                playType = PlayType.PLAY,
                ledPatternType = LedPatternType.PATTERN_TYPE_NONE
            )
        )
        return ledPatternItems;
    }

    /**
     * Handles the back arrow click in the toolbar.
     */
    fun backArrowClick() {
        navController.popBackStack()
    }

    /**
     * Returns true if gear supports led otherwise false.
     */
    fun checkGearCapability(state: GearState): Boolean {
        for (capability: Capability in state.attached().gearCapabilities()) {
            if (capability.ordinal == Capability.LED.ordinal) {
                return true
            }
        }
        return false
    }

    /**
     * Plays LED Pattern on Gear.
     * @param patternItem pattern to be played.
     * @return Signal<Boolean> defines if pattern played successfully.
     */
    fun playLedCommandOnGear(patternItem: LedPatternItem, duration: Int): Signal<Boolean> {
        val ledPatternCommandBuilder = getPlayLEDBuilder(patternItem, duration)
        return getGearNotification()
            .first()
            .filter { it.type == GearState.Type.ATTACHED }
            .map(GearState::attached)
            .tap { PrintLogger.d(TAG, "LED pattern Component # $it") }
            .flatMap { component ->
                connectedJacquardTag
                    .first()
                    .flatMap { tag ->
                        tag.enqueue(ledPatternCommandBuilder.setComponent(component).build())
                    }
            }
    }

    /**
     * Plays LED Pattern on UJT.
     * @param patternItem pattern to be played.
     * @return Signal<Boolean> defines if pattern played successfully.
     */
    fun playLEDCommandOnUJT(patternItem: LedPatternItem, durationMs: Int): Signal<Boolean> {
        val ledPatternCommandBuilder = getPlayLEDBuilder(patternItem, durationMs)
        return connectedJacquardTag
            .first()
            .flatMap { tag ->
                tag.enqueue(ledPatternCommandBuilder.setComponent(tag.tagComponent()).build())
            }
    }

    /**
     * Plays LED Pattern on all UJT.
     * @param patternItem pattern to be played.
     */
    fun playLEDCommandOnAllUJT(patternItem: LedPatternItem, durationMs: Int) {
        val ledPatternCommandBuilder = getPlayLEDBuilder(patternItem, durationMs)
        for (tag: KnownTag in preferences.knownTags) {
            connectivityManager.getConnectedTag(tag.address())
                .tapError { PrintLogger.d(TAG,  /* message= */"Tag is not connected") }
                .onNext { tag ->
                    tag.enqueue(ledPatternCommandBuilder.setComponent(tag.tagComponent()).build())
                        .consume()
                }
        }
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

    /**
     * Persists tag led state.
     *
     * @param isChecked true if tag led control is active
     */
    fun persistTagLedState(isChecked: Boolean) {
        preferences.persistTagLedState(isChecked);
    }

    /**
     * Returns true if tag led is active.
     */
    fun isTagLedActive(): Boolean {
        return preferences.isTagLedActive;
    }

    /**
     * Persists gear led state.
     *
     * @param isActive true if gear led control is active
     */
    fun persistGearLedState(isActive: Boolean) {
        preferences.persistGearLedState(isActive);
    }

    /**
     * Returns true if gear led is active.
     */
    fun isGearLedActive(): Boolean {
        return preferences.isGearLedActive;
    }

    private fun getPlayLEDBuilder(
        patternItem: LedPatternItem,
        durationMs: Int
    ): PlayLedPatternCommandBuilder {
        return newBuilder()
            .setFrames(patternItem.frames)
            .setResumable(patternItem.resumable)
            .setPlayType(patternItem.playType)
            .setLedPatternType(patternItem.ledPatternType)
            .setHaltAll(patternItem.haltAll)
            .setIntensityLevel(100)
            .setDurationInMs(durationMs)
            .setStringUtils(stringUtils);
    }

    private fun getLedPatternItem(
        icon: Int, text: String, frames: List<Frame>,
        resumable: Boolean, haltAll: Boolean, playType: PlayType,
        ledPatternType: LedPatternType
    ): LedPatternItem {
        return LedPatternItem(
            icon,
            text,
            frames,
            resumable,
            ledPatternType,
            playType,
            haltAll
        )
    }
}