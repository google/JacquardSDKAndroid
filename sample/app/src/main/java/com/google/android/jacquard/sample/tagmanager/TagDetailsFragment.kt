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

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.jacquard.sample.KnownTag
import com.google.android.jacquard.sample.MainActivity
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sample.home.HomeViewModel
import com.google.android.jacquard.sample.utilities.Util
import com.google.android.jacquard.sdk.command.BatteryStatus
import com.google.android.jacquard.sdk.command.BatteryStatus.ChargingState
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.model.GearState
import com.google.android.jacquard.sdk.rx.Signal

/**
 * Fragment for showing tag details.
 */
class TagDetailsFragment : Fragment() {

  companion object {
    private val TAG = TagDetailsFragment::class.java.simpleName
  }

  private val subscriptions: MutableList<Signal.Subscription> = mutableListOf()
  private val tagDetailsViewModel by lazy { getViewModel<TagDetailsViewModel>() }
  private lateinit var knownTag: KnownTag
  private lateinit var txtSerialNumber: TextView
  private lateinit var txtVersion: TextView
  private lateinit var txtBattery: TextView
  private lateinit var txtAttached: TextView
  private lateinit var txtTagStatus: TextView
  private lateinit var txtSelectAsCurrentTag: TextView
  private lateinit var txtForgetTag: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    knownTag = TagDetailsFragmentArgs.fromBundle(requireArguments()).knownTag
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_tag_details, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    tagDetailsViewModel.init()
    initToolbar(view)
    initUI(view)
    getTagDetails()
    showMessageWhenTagIsNotCurrent()
    txtSerialNumber.text = knownTag.pairingSerialNumber()
    txtSelectAsCurrentTag.setOnClickListener {
      tagDetailsViewModel
        .selectAsCurrentTag(requireActivity(), knownTag) { intentSender ->
          (requireActivity() as MainActivity)
            .startForResult(intentSender, MainActivity.COMPANION_DEVICE_REQUEST)
            .map { result -> result.resultCode() == Activity.RESULT_OK }
        }
      showSnackBar(getString(R.string.tag_details_tag_selected, knownTag.displayName()))
    }
    txtForgetTag.setOnClickListener {
      tagDetailsViewModel
        .forgetTag(requireActivity(), knownTag) { intentSender ->
          (requireActivity() as MainActivity)
            .startForResult(intentSender, MainActivity.COMPANION_DEVICE_REQUEST)
            .map { result -> result.resultCode() == Activity.RESULT_OK }
        }
      showSnackBar(getString(R.string.tag_details_tag_removed, knownTag.displayName()))
    }
  }

  override fun onDestroyView() {
    for (subscription in subscriptions) {
      subscription.unsubscribe()
    }
    subscriptions.clear()
    super.onDestroyView()
  }

  private fun initUI(view: View) {
    txtSerialNumber = view.findViewById(R.id.serialNumberValueTxt)
    txtVersion = view.findViewById(R.id.versionNumberValueTxt)
    txtBattery = view.findViewById(R.id.batteryValueTxt)
    txtAttached = view.findViewById(R.id.attachStatusTxt)
    txtTagStatus = view.findViewById(R.id.tagStatusTxt)
    txtSelectAsCurrentTag = view.findViewById(R.id.currentTagTxt)
    txtForgetTag = view.findViewById(R.id.forgetTagTxt)
  }

  private fun getFirmwareVersion() {
    subscriptions.add(tagDetailsViewModel.getVersion().observe(
      { version ->
        PrintLogger.d(TAG, "Received device info: $version")
        txtVersion.text = version.toString()
      }) { error ->
      if (error == null) return@observe
      PrintLogger.e(TAG, "getFirmwareVersion: ${error.message}")
      showSnackBar(error.message)
    })
  }

  private fun getBatteryStatus() {
    subscriptions.add(tagDetailsViewModel.getBatteryStatus().observe(
      { notification ->
        PrintLogger.d(TAG, "Received battery status info: $notification")
        onBatteryStatus(notification)
      }) { error ->
      if (error == null) return@observe
      PrintLogger.e(TAG, "getBatteryStatus: ${error.message}")
      showSnackBar(error.message)
    })
  }

  private fun subscribeToNotifications() {
    subscriptions.add(tagDetailsViewModel.getNotification().observe(
      { notification ->
        PrintLogger.d(TAG, "Received notification: $notification")
        when (notification.type) {
          HomeViewModel.Notification.Type.BATTERY -> onBatteryStatus(notification.battery())
          HomeViewModel.Notification.Type.GEAR -> onGearState(notification.gear())
        }
      }) { error ->
      if (error == null) return@observe
      PrintLogger.e(TAG, "subscribeToNotifications: ${error.message}")
      showSnackBar(error.message)
    })
  }

  private fun onGearState(gearState: GearState) {
    if (gearState.type == GearState.Type.ATTACHED)
      txtAttached.text = getString(
        R.string.tag_details_tag_attached,
        gearState.attached().product().name()
      )
    else txtAttached.text = getString(R.string.gear_attached_no)
  }

  private fun getTagDetails() {
    if (tagDetailsViewModel.checkIfCurrentTag(knownTag)) {
      txtAttached.text = getString(R.string.gear_attached_no)
      txtTagStatus.visibility = View.VISIBLE
      txtSelectAsCurrentTag.visibility = View.GONE
      getFirmwareVersion()
      getBatteryStatus()
      subscribeToNotifications()
    } else {
      txtSelectAsCurrentTag.visibility = View.VISIBLE
      showSnackBar(getString(R.string.tag_details_tag_not_current))
    }
  }

  private fun onBatteryStatus(batteryStatus: BatteryStatus) {
    val chargingStateText =
      getString(if (batteryStatus.chargingState() == ChargingState.CHARGING) R.string.home_page_battery_charge_state_charging else R.string.home_page_battery_charge_state_not_charging)
    txtBattery.text = getString(
      R.string.tag_details_battery_info,
      batteryStatus.batteryLevel().toString(),
      chargingStateText
    )
    txtTagStatus.text = getString(R.string.current_tag)
    txtTagStatus.isEnabled = true
  }

  private fun initToolbar(view: View) {
    view.findViewById<Toolbar>(R.id.toolbar)
      .setNavigationOnClickListener { tagDetailsViewModel.backArrowClick() }
    view.findViewById<TextView>(R.id.toolbar_title).text = knownTag.displayName()
  }

  private fun showSnackBar(text: String?) {
    text?.let { Util.showSnackBar(view, it) }
  }

  private fun showMessageWhenTagIsNotCurrent() {
    if (tagDetailsViewModel.checkIfCurrentTag(knownTag)) {
      PrintLogger.d(TAG, "${knownTag.address()} is current tag")
      return
    }
    Util.showSnackBar(view, getString(R.string.tag_details_tag_not_current))
  }
}