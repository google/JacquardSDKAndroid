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
package com.google.android.jacquard.sample.renametag

import android.text.TextUtils
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.ConnectivityManager.Events
import com.google.android.jacquard.sample.KnownTag
import com.google.android.jacquard.sample.Preferences
import com.google.android.jacquard.sdk.command.RenameTagCommand
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag

/** View model for the [RenameTagFragment].  */
class RenameTagViewModel(
    private val preferences: Preferences, private val navController: NavController,
    private val connectivityManager: ConnectivityManager
) : ViewModel() {

    companion object {
        private val TAG = RenameTagViewModel::class.java.simpleName
    }

    private var currentTag: KnownTag? = null

    /** Initialises the current tag in the view model.  */
    fun initCurrentTag() {
        currentTag = preferences.currentTag
    }

    /** Handles the back arrow click in the toolbar.  */
    fun backArrowClick() {
        navController.popBackStack()
    }

    /** Returns the current tag name.  */
    val tagName: String
        get() = currentTag?.displayName() ?: "NA"

    /** Updates the tag name of current paired tag.  */
    fun updateTagName(tagName: String): Signal<String> {
        if (TextUtils.isEmpty(tagName.trim())) {
            return Signal.empty(Exception("Tag name cannot be empty"))
        }
        return preferences.currentTag?.let {
            connectivityManager.getConnectedJacquardTag(it.address())
                .first()
                .flatMap { tag: ConnectedJacquardTag ->
                    if (tagName.contentEquals(tag.displayName())){
                        Signal.empty(Exception("Tag name cannot be the same"))
                    } else {
                        tag.enqueue(RenameTagCommand(tagName))
                    }
                }
        } ?: Signal.empty()
    }

    /**
     * Updates the current device data and tag name into the known devices list of given tag
     * identifier.
     */
    fun updateKnownDevices(updatedTagName: String) {
        preferences.currentTag?.let {
            val tagIdentifier = it.address()
            val knownTags = preferences.knownTags
            val knownTag = KnownTag(
                tagIdentifier,
                updatedTagName,
                it.pairingSerialNumber,
                /* rssiValue= */ null
            )
            for (i in knownTags.indices) {
                if (knownTags[i].address() == tagIdentifier) {
                    knownTags[i] = knownTag
                    break
                }
            }
            PrintLogger.d(TAG, "Persisting devices: $knownTags")
            preferences.putKnownDevices(knownTags)
            preferences.putCurrentDevice(knownTag)
            currentTag = knownTag
        }
    }

    /**
     * Emits connectivity events [Events].
     */
    val connectivityEvents: Signal<Events>
        get() = preferences.currentTag?.let {
            connectivityManager.getEventsSignal(it.address())
                .distinctUntilChanged()
        } ?: Signal.empty()
}