/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sample.tagmanager

import android.content.Context
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.KnownTag
import com.google.android.jacquard.sample.Preferences
import com.google.android.jacquard.sample.home.HomeViewModel
import com.google.android.jacquard.sdk.command.BatteryStatus
import com.google.android.jacquard.sdk.command.BatteryStatusCommand
import com.google.android.jacquard.sdk.command.BatteryStatusNotificationSubscription
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.model.Revision
import com.google.android.jacquard.sdk.rx.Fn
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag

/**
 * A viewModel for fragment [TagDetailsFragment].
 */
class TagDetailsViewModel(
  private val connectivityManager: ConnectivityManager,
  private val preferences: Preferences,
  private val navController: NavController
) : ViewModel() {

  companion object {
    private val TAG = TagDetailsViewModel::class.java.simpleName
  }

  private val subscriptions: MutableList<Signal.Subscription> = mutableListOf()
  private lateinit var connectedJacquardTagSignal: Signal<ConnectedJacquardTag>

  /**
   * Initialize view model, It should be called from onViewCreated.
   */
  fun init() {
    connectedJacquardTagSignal = connectivityManager
      .getConnectedJacquardTag(preferences.currentTag.address())
  }

  /**
   * Connects to the Notification for the Tag.
   */
  fun getNotification(): Signal<HomeViewModel.Notification> {
    return connectedJacquardTagSignal.distinctUntilChanged()
      .switchMap { tag ->
        Signal.merge(mutableListOf(getBatteryNotifications(tag), getGearNotifications(tag)))
      }
  }

  /**
   * Gets battery status for selected tag.
   */
  fun getBatteryStatus(): Signal<BatteryStatus> {
    return connectedJacquardTagSignal.first()
      .flatMap { tag -> tag.enqueue(BatteryStatusCommand()) }
  }

  /**
   * Gets device version info for selected tag.
   */
  fun getVersion(): Signal<Revision?> {
    return connectedJacquardTagSignal.first()
      .map { tag -> tag.tagComponent().version() }
  }

  /**
   * Returns true if selected tag is current tag.
   */
  fun checkIfCurrentTag(knownTag: KnownTag): Boolean {
    return if (preferences.currentTag.address() == knownTag.address()) {
      PrintLogger.d(TAG, "Selected tag is current tag")
      true
    } else false
  }

  /**
   * Handles the back arrow click in the toolbar.
   */
  fun backArrowClick() {
    navController.popBackStack()
  }

  /**
   * Sets selected tag as current tag and initiates connect call for the tag.
   */
  fun selectAsCurrentTag(
    context: Context, knownTag: KnownTag,
    senderHandler: Fn<IntentSender, Signal<Boolean>>
  ) {
    PrintLogger.d(TAG, "Change current tag to :: ${knownTag.displayName()}")
    connectivityManager.connect(context, knownTag.address(), senderHandler)
    preferences.putCurrentDevice(knownTag)
    backArrowClick()
  }

  /**
   * Removes selected tag from known tag list.
   */
  fun forgetTag(
    context: Context, knownTag: KnownTag,
    senderHandler: Fn<IntentSender, Signal<Boolean>>
  ) {
    connectivityManager.forget(knownTag.address())
    val knownTags = preferences.knownTags
    knownTags.remove(knownTag)
    preferences.putKnownDevices(knownTags)
    if (knownTags.isEmpty()) {
      preferences.removeCurrentDevice()
      initiateScan()
    } else selectAsCurrentTag(context, knownTags.first(), senderHandler)
  }

  /**
   * Clearance operation for ViewModel.
   */
  override fun onCleared() {
    for (subscription in subscriptions) {
      subscription.unsubscribe()
    }
    subscriptions.clear()
    super.onCleared()
  }

  private fun getBatteryNotifications(tag: ConnectedJacquardTag): Signal<HomeViewModel.Notification> {
    return tag.subscribe(BatteryStatusNotificationSubscription())
      .map { batteryStatus -> HomeViewModel.Notification.ofBattery(batteryStatus) }
  }

  private fun getGearNotifications(tag: ConnectedJacquardTag): Signal<HomeViewModel.Notification> {
    return tag.connectedGearSignal.map { gearState ->
      HomeViewModel.Notification.ofGear(gearState)
    }
  }

  private fun initiateScan() {
    PrintLogger.d(TAG, "Initiate paring flow.")
    navController.navigate(TagDetailsFragmentDirections.actionTagDetailsFragmentToScanFragment())
  }
}