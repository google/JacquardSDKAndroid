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

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.ConnectivityManager.Events
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sample.utilities.Util
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.model.GearState
import com.google.android.jacquard.sdk.rx.Signal.Subscription

/**
 * Fragment for LED patterns screen.
 */
class LedPatternFragment : Fragment(), LedPatternAdapter.ItemClickListener {
    private companion object {
        private val TAG = LedPatternFragment::class.java.simpleName
        private const val DEFAULT_LED_DURATION = 5 * 1000 // 5 sec
        private const val MIN_DURATION = 1
        private const val MAX_DURATION = Integer.MAX_VALUE / 1000 // in seconds
        private const val DEFAULT_TXT = "5"
    }

    private val subscriptions: MutableList<Subscription> = mutableListOf()
    private val viewModel by lazy { getViewModel<LedPatternViewModel>() }
    private lateinit var gearSwitch: SwitchCompat
    private lateinit var tagSwitch: SwitchCompat
    private lateinit var imgTag: ImageView
    private lateinit var imgGarment: ImageView
    private lateinit var txtTag: TextView
    private lateinit var txtGarment: TextView
    private lateinit var editDuration: EditText
    private var playLedOnGarment = false
    private var playLedOnTag = false
    private var isTagLedClickedWithoutUser = false
    private var isGearLedClickedWithoutUser = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_led_pattern, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gearSwitch = view.findViewById(R.id.gearSwitch)
        tagSwitch = view.findViewById(R.id.tagSwitch)
        txtGarment = view.findViewById(R.id.garmentTxt)
        txtTag = view.findViewById(R.id.tagTxt)
        editDuration = view.findViewById(R.id.led_duration)
        imgGarment = view.findViewById(R.id.garmentImg)
        imgTag = view.findViewById(R.id.tagImg)
        viewModel.init()
        initToolbar(view)
        editDuration.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val text: String = v.text.toString()
                if (!TextUtils.isEmpty(text)) {
                    val duration = text.toInt()
                    if (duration < MIN_DURATION) {
                        showSnackbar(getString(R.string.led_duration_min_error))
                        setDefault()
                    } else if (duration > MAX_DURATION) {
                        showSnackbar(getString(R.string.led_duration_max_error, MAX_DURATION))
                        setDefault()
                    }
                }
            }
            false
        }
        gearSwitch.setOnCheckedChangeListener { _, isChecked ->
            playLedOnGarment = isChecked
            if (isGearLedClickedWithoutUser) {
                isGearLedClickedWithoutUser = false
                return@setOnCheckedChangeListener
            }
            viewModel.persistGearLedState(isChecked)
        }
        tagSwitch.setOnCheckedChangeListener { _, isChecked ->
            playLedOnTag = isChecked
            if (isTagLedClickedWithoutUser) {
                isTagLedClickedWithoutUser = false
                return@setOnCheckedChangeListener
            }
            viewModel.persistTagLedState(isChecked)
        }
        gearSwitch.isChecked = viewModel.isGearLedActive()
        updateSwitchState()
        subscriptions.add(
            viewModel.getGearNotification().distinctUntilChanged().onNext(this::updateGearState)
        )
        subscriptions.add(viewModel.getConnectivityEvents().onNext(this::updateTagLedSwitch))
        initLedPatternList(view)
        subscribeEvents()
    }

    override fun onPause() {
        Util.hideSoftKeyboard(requireActivity())
        super.onPause()
    }

    override fun onDestroyView() {
        unSubscribeSubscription()
        super.onDestroyView()
    }

    override fun onItemClick(patternItem: LedPatternItem) {
        var ledDuration = DEFAULT_LED_DURATION
        val durationText: String = editDuration.text.toString().trim()
        if (!TextUtils.isEmpty(durationText)) {
            val duration = editDuration.text.toString().toInt()
            if (duration in MIN_DURATION..MAX_DURATION) {
                ledDuration = duration * 1000
            } else {
                setDefault()
            }
        } else {
            PrintLogger.e(TAG, "Using default led duration.")
        }
        if (playLedOnGarment) {
            subscriptions.add(viewModel.playLedCommandOnGear(patternItem, ledDuration)
                .onError { error: Throwable ->
                    PrintLogger.e(TAG, error.message)
                    showSnackbar(error.message)
                })
        }

        if (playLedOnTag) {
            subscriptions.add(viewModel.playLEDCommandOnUJT(patternItem, ledDuration)
                .onError { error: Throwable ->
                    PrintLogger.e(TAG, error.message)
                    showSnackbar(error.message)
                })
        }
    }

    private fun unSubscribeSubscription() {
        for (subscription in subscriptions) {
            subscription.unsubscribe()
        }
        subscriptions.clear()
    }

    private fun initToolbar(root: View) {
        root.findViewById<Toolbar>(R.id.toolbar).setOnClickListener {
            viewModel.backArrowClick()
        }
    }

    private fun updateGearState(state: GearState) {
        PrintLogger.d(TAG, "Gear state >> $state")
        when (checkNotNull(state.type)) {
            GearState.Type.DETACHED -> onDetached()
            GearState.Type.ATTACHED -> onAttach(state)
        }
    }

    private fun onDetached() {
        gearSwitch.isChecked = false
        showSnackbar(getString(R.string.gear_detached))
    }

    private fun onAttach(state: GearState) {
        val isGearLedSupported = viewModel.checkGearCapability(state)
        updateGarmentLedSwitch(isGearLedSupported)
    }

    private fun updateTagLedSwitch(enabled: Boolean) {
        if (enabled) {
            tagSwitch.isChecked = viewModel.isTagLedActive()
        } else {
            isTagLedClickedWithoutUser = true
            tagSwitch.isChecked = false
        }
        tagSwitch.isEnabled = enabled
        imgTag.isEnabled = enabled
        txtTag.isEnabled = enabled
    }

    private fun updateTagLedSwitch(events: Events) {
        when (events) {
            Events.TAG_CONNECTED -> updateTagLedSwitch(true)
            Events.TAG_DISCONNECTED -> updateTagLedSwitch(false)
            else -> PrintLogger.d(TAG, "Event received > " + events.name)
        }
    }

    private fun updateGarmentLedSwitch(enabled: Boolean) {
        if (enabled) {
            gearSwitch.isChecked = viewModel.isGearLedActive()
        } else {
            isGearLedClickedWithoutUser = true
            gearSwitch.isChecked = false
        }
        imgGarment.isEnabled = enabled
        txtGarment.isEnabled = enabled
        gearSwitch.isEnabled = enabled
    }

    private fun initLedPatternList(view: View) {
        view.findViewById<RecyclerView>(R.id.recyclerviewPattern).adapter =
            LedPatternAdapter(viewModel.getLedPatterns(), this)
    }

    private fun showSnackbar(message: String?) {
        message?.let { Util.showSnackBar(view, message) }
    }

    private fun subscribeEvents() {
        subscriptions.add(viewModel.getConnectivityEvents().onNext(this::onEvents))
    }

    private fun onEvents(events: Events) {
        when (events) {
            Events.TAG_DISCONNECTED -> showSnackbar(getString(R.string.tag_not_connected))
            Events.TAG_DETACHED -> showSnackbar(getString(R.string.gear_detached))
            else -> PrintLogger.d(TAG, "Event received > " + events.name)
        }
    }

    private fun updateSwitchState() {
        playLedOnTag = viewModel.isTagLedActive()
        tagSwitch.isChecked = playLedOnTag
    }

    private fun setDefault() {
        editDuration.setText(DEFAULT_TXT)
    }
}