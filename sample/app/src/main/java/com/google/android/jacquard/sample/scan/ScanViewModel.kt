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

package com.google.android.jacquard.sample.scan

import android.content.Context
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.KnownTag
import com.google.android.jacquard.sample.Preferences
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.splash.SplashFragmentDirections
import com.google.android.jacquard.sdk.connection.ConnectionState
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Fn
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.rx.Signal.ObservesNext
import com.google.android.jacquard.sdk.rx.Signal.Subscription
import com.google.android.jacquard.sdk.tag.AdvertisedJacquardTag
import com.google.auto.value.AutoOneOf
import java.util.concurrent.TimeoutException

/** View model for {@link ScanFragment}. */
class ScanViewModel(
  val connectivityManager: ConnectivityManager,
  val preferences: Preferences,
  val navController: NavController
) : ViewModel() {

  companion object {
    private val TAG = ScanViewModel::class.java.simpleName
    private const val PAIRING_TIMEOUT = 30000
  }

  @AutoOneOf(State.Type::class)
  abstract class State {
    abstract fun getType(): Type
    abstract fun connecting()
    abstract fun connected()
    abstract fun error(): String
    enum class Type {
      CONNECTING, CONNECTED, ERROR
    }

    companion object {
      fun ofConnecting(): State {
        return AutoOneOf_ScanViewModel_State.connecting()
      }

      fun ofConnected(): State {
        return AutoOneOf_ScanViewModel_State.connected()
      }

      fun ofError(errorMsg: String): State {
        return AutoOneOf_ScanViewModel_State.error(errorMsg)
      }
    }
  }

  val stateSignal: Signal<State> = Signal.create()
  private val subscriptions: MutableList<Subscription> = mutableListOf()
  private lateinit var tag: KnownTag

  fun startScanning(): Signal<List<AdapterItem>> {
    val tags: MutableList<AdvertisedJacquardTag> = mutableListOf()
    val scanningSignal: Signal<List<AdapterItem>> = connectivityManager
      .startScanning()
      .distinct()
      .scan(
        tags,
        { advertisedJacquardTags, tag ->
          advertisedJacquardTags.add(tag)
          advertisedJacquardTags
        }
      )
      .map { advertisedJacquardTags ->
        getAdvertisingTagSection(advertisedJacquardTags)
      }
      .map { advertisingSection ->
        val items: MutableList<AdapterItem> = mutableListOf()
        items.addAll(advertisingSection)
        items.addAll(getKnownTagsSection())
        items
      }

    val knownTags: Signal<MutableList<AdapterItem>> = Signal.just(getKnownTagsSection())
    return Signal.merge(scanningSignal, knownTags)
  }

  /** Sets the selected known tag.  */
  fun setSelected(knownTag: KnownTag) {
    tag = knownTag
  }

  /** Connects to the selected tag.  */
  fun connect(context: Context, senderHandler: Fn<IntentSender, Signal<Boolean>>) {
    stateSignal.next(State.ofConnecting())
    subscriptions
      .add(connectivityManager
             .connect(context, tag.address(), senderHandler)
             .filter { connectionState: ConnectionState ->
               connectionState.isType(ConnectionState.Type.CONNECTED)
             }
             .first()
             .timeout(PAIRING_TIMEOUT.toLong())
             .observe(object : ObservesNext<ConnectionState>() {
               override fun onNext(connectionState: ConnectionState) {
                 stateSignal.next(State.ofConnected())
               }

               override fun onError(t: Throwable) {
                 PrintLogger.e(TAG, "Failed to connect: $t")
                 val errorMsg: String =
                   if (t is TimeoutException)
                     context.getString(R.string.scan_page_pairing_issue_msg)
                   else t.message.toString()
                 stateSignal.next(State.ofError(errorMsg))
               }
             })
      )
  }

  /**
   * Saves the connected tag details and notify the tag is connected.
   */
  fun successfullyConnected(isUserAlreadyOnboarded: Boolean) {
    persistKnownDevices(tag)
    if (isUserAlreadyOnboarded)
      navController.popBackStack()
    else
      navController.navigate(SplashFragmentDirections.actionToHomeFragment())
  }

  override fun onCleared() {
    super.onCleared()
    for (subscription in subscriptions) {
      subscription.unsubscribe()
    }
    subscriptions.clear()
  }

  private fun getAdvertisingTagSection(advertisedJacquardTags: MutableList<AdvertisedJacquardTag>)
    : MutableList<AdapterItem> {
    val tags: MutableList<AdapterItem> = mutableListOf()
    tags.add(AdapterItem.ofSectionHeader(R.string.scan_page_nearby_tags_header))
    for (advertisedJacquardTag in advertisedJacquardTags) {
      tags.add(AdapterItem.ofTag(KnownTag.of(advertisedJacquardTag)))
    }
    return tags
  }

  private fun getKnownTagsSection(): MutableList<AdapterItem> {
    val knownTagSection: MutableList<AdapterItem> = mutableListOf()
    val knownTags = preferences.knownTags
    if (knownTags.isNotEmpty()) {
      knownTagSection
        .add(AdapterItem.ofSectionHeader(R.string.scan_adapter_section_title_previously_connected_tags))
    }
    for (knownTag in knownTags) {
      knownTagSection.add(AdapterItem.ofTag(knownTag))
    }
    return knownTagSection
  }

  private fun persistKnownDevices(tag: KnownTag) {
    val knownTags: MutableSet<KnownTag> = mutableSetOf()
    knownTags.addAll(preferences.knownTags)
    knownTags.add(tag)
    PrintLogger.d(TAG, "Persisting devices: $knownTags")
    preferences.putKnownDevices(knownTags)
    preferences.putCurrentDevice(tag)
  }
}