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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogBuilder
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sample.utilities.CustomBottomProgress
import com.google.android.jacquard.sample.utilities.JacquardRelativeLayout
import com.google.android.jacquard.sample.utilities.JacquardRelativeLayout.OnDoubleListener
import com.google.android.jacquard.sample.utilities.Util.getRSSIColor
import com.google.android.jacquard.sample.utilities.Util.showSnackBar
import com.google.android.jacquard.sdk.command.BatteryStatus
import com.google.android.jacquard.sdk.command.BatteryStatus.ChargingState
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.model.GearState
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.rx.Signal.ObservesNext
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Objects

/** Fragment for Dashboard UI.  */
class HomeFragment : Fragment(), OnDoubleListener {
    private val subscriptions: MutableList<Signal.Subscription> = mutableListOf()
    private val blurProductImageName = "product_blur_image_hear"

    companion object {
        private val TAG = HomeFragment::class.java.simpleName
    }

    private val homeViewModel by lazy { getViewModel<HomeViewModel>() }

    private val spanSizeLookup = object : SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return if (HomeTileModel.Type.values()[Objects.requireNonNull(recyclerView.adapter)
                    .getItemViewType(position)]
                === HomeTileModel.Type.SECTION
            ) {
                2
            } else 1
        }
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var txtGearState: TextView
    private lateinit var txtTagName: TextView
    private lateinit var txtBattery: TextView
    private lateinit var txtRssi: TextView
    private lateinit var imgProduct: ImageView
    private lateinit var viewDownloadProgress: CustomBottomProgress
    private lateinit var viewTop: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        subscriptions.add(homeViewModel.stateSignal.onNext { state ->
            onNavigation(
                state
            )
        })
        val view =
            inflater.inflate(R.layout.fragment_home, container, false) as JacquardRelativeLayout
        view.setDoubleTouchListener(this)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imgProduct = view.findViewById(R.id.imgProduct)
        txtGearState = view.findViewById(R.id.txtTagState)
        txtTagName = view.findViewById(R.id.txtTagName)
        txtBattery = view.findViewById(R.id.txtBattery)
        txtRssi = view.findViewById(R.id.txtRssi)
        recyclerView = view.findViewById(R.id.recyclerGearOptions)
        viewDownloadProgress = view.findViewById(R.id.customBottomProgress)
        viewTop = view.findViewById(R.id.relTop)
        val layoutManager = GridLayoutManager(requireContext(),  /* spanCount= */2)
        layoutManager.spanSizeLookup = spanSizeLookup
        recyclerView.setLayoutManager(layoutManager)
        recyclerView.addItemDecoration(ItemOffsetDecoration(requireContext(), R.dimen.item_offset))
        homeViewModel.init()

        populateView()
        subscribeToNotifications()

        subscriptions.add(homeViewModel.batteryStatus.onNext { batteryStatus ->
            onBatteryStatus(
                batteryStatus
            )
        })

        subscribeEvents()
        fwUpdateStateListener()
    }

    override fun onTwoFingerDoubleTap() {
        try {
            showAppVersionDialog()
        } catch (e: ParseException) {
            PrintLogger.e(TAG, "Parsing issue for build date.")
        }
    }

    @Throws(ParseException::class)
    private fun showAppVersionDialog() {
        val defaultDialog = DefaultDialogBuilder()
            .setTitle(R.string.app_version_dialog_title)
            .setSubtitle(
                getString(
                    R.string.app_version_dialog_desc,
                    com.google.android.jacquard.sdk.BuildConfig.SDK_VERSION,
                    com.google.android.jacquard.sample.BuildConfig.GIT_HEAD,
                    buildDate
                )
            )
            .setPositiveButtonTitleId(R.string.ok_caps)
            .setShowNegativeButton(false)
            .setShowPositiveButton(true)
            .setCancellable(true)
            .setShowSubtitle(true)
            .setShowProgress(false)
            .build()
        defaultDialog.show(parentFragmentManager,  /* tag= */null)
    }

    @get:Throws(ParseException::class)
    private val buildDate: String
        get() {
            val versionStr = com.google.android.jacquard.sample.BuildConfig.VERSION_CODE.toString()
            val original = SimpleDateFormat("yyyyMMddHH")
            val updated = SimpleDateFormat("yyyy-MM-dd hh aa")
            val parsedDate = original.parse(versionStr)
            return if (parsedDate != null) updated.format(parsedDate) else "Not Available"
        }

    override fun onDestroyView() {
        for (subscription in subscriptions) {
            subscription.unsubscribe()
        }
        subscriptions.clear()
        super.onDestroyView()
    }

    private fun onNavigation(state: HomeViewModel.State) {
        PrintLogger.d(TAG, "onNavigation: $state")
        when (state.type) {
            HomeViewModel.State.Type.ADAPTER -> {
                onSetAdapter(state.adapter().listForAdapter, state.adapter().itemClickListener)
            }
            HomeViewModel.State.Type.CONNECTED -> {
                onConnected(state.connected())
            }
            HomeViewModel.State.Type.DISCONNECTED -> {
                onDisconnected()
            }
            HomeViewModel.State.Type.ERROR -> showSnackbar(state.error())
        }
    }

    private fun onSetAdapter(
        list: List<HomeTileModel>, itemClickListener: ItemClickListener<HomeTileModel>
    ) {
        if (recyclerView.adapter == null) {
            recyclerView.adapter = HomeTilesListAdapter(requireContext(), itemClickListener)
        }
        (recyclerView.adapter as HomeTilesListAdapter?)!!.submitList(list)
    }

    private fun populateView() {
        homeViewModel.tagName.onNext { text -> txtTagName.text = text }
    }

    private fun subscribeToNotifications() {
        subscriptions.add(
            homeViewModel
                .notification
                .observe(
                    object : ObservesNext<HomeViewModel.Notification>() {
                        override fun onNext(notification: HomeViewModel.Notification) {
                            PrintLogger.d(TAG, "Received notification: $notification")
                            when (notification.type) {
                                HomeViewModel.Notification.Type.BATTERY -> onBatteryStatus(
                                    notification.battery()
                                )
                                HomeViewModel.Notification.Type.GEAR -> onGearState(notification.gear())
                            }
                        }

                        override fun onError(t: Throwable) {
                            PrintLogger.e(TAG, "Failed Notification: $t")
                        }
                    })
        )
    }

    private fun onRSSIChanged(rssiValue: Int) {
        if (isAdded) {
            txtRssi.text = getString(R.string.home_page_rssi_info, rssiValue.toString())
            txtRssi.setTextColor(requireContext().getColor(getRSSIColor(rssiValue)))
        }
    }

    private fun onBatteryStatus(batteryStatus: BatteryStatus) {
        val chargingStateText = getString(
            if (batteryStatus.chargingState() == ChargingState.CHARGING) R.string.home_page_battery_charge_state_charging else R.string.home_page_battery_charge_state_not_charging
        )
        txtBattery.text = getString(
            R.string.home_page_battery_info, batteryStatus.batteryLevel().toString(),
            chargingStateText
        )
    }

    private fun onGearState(gearState: GearState) {
        PrintLogger.d(TAG, "onGearState: $gearState")
        if (gearState.type == GearState.Type.ATTACHED) {
            updateGearImage(gearState.attached().product().image())
            homeViewModel.onTagAttached()
            txtGearState.text = gearState.attached().product().name()
            txtGearState.setTextColor(requireContext().getColor(R.color.grey_700))
            txtTagName.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                R.drawable.green_indicator,
                0
            )
        } else {
            updateGearImage(blurProductImageName)
            homeViewModel.onTagConnected()
            txtGearState.text = getString(R.string.state_not_attached)
            txtGearState.setTextColor(requireContext().getColor(R.color.home_tile_grey_shade_2))
            txtTagName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.red_indicator, 0)
        }
    }

    private fun updateGearImage(productImage: String) {
        val imageResourceId = resources.getIdentifier(
            productImage,  /* defType = */
            "drawable",
            requireContext().packageName
        )
        imgProduct.setImageResource(imageResourceId)
    }

    private fun onConnected(tag: ConnectedJacquardTag) {
        subscriptions.add(tag.rssiSignal()!!.onNext { rssiValue -> onRSSIChanged(rssiValue) })
        onGearState(GearState.ofDetached())
    }

    private fun onDisconnected() {
        homeViewModel.onTagDisconnected()
        txtBattery.text = ""
        txtRssi.text = ""
        txtTagName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.red_indicator, 0)
        txtGearState.text = getString(R.string.state_disconnected)
        txtGearState.setTextColor(requireContext().getColor(R.color.home_tile_grey_shade_2))
    }

    private fun showSnackbar(message: String) {
        showSnackBar(view, message)
    }

    private fun showDisconnectSnackbar() {
        showSnackBar(
            view,
            getString(R.string.tag_not_connected),
            getString(R.string.disconnect_help)
        ) { showDisconnectDialog() }
    }

    private fun showDisconnectDialog() {
        DefaultDialogBuilder()
            .setTitle(R.string.disconnect_help_dialog_title)
            .setSubtitle(R.string.disconnect_help_dialog_subtitle)
            .setPositiveButtonTitleId(R.string.disconnect_help_dialog_positive_btn)
            .setShowNegativeButton(false)
            .setShowPositiveButton(true)
            .setCancellable(true)
            .setShowSubtitle(true)
            .setShowProgress(false)
            .build()
            .show(parentFragmentManager,  /* tag= */null)
    }

    private fun subscribeEvents() {
        subscriptions.add(homeViewModel.connectivityEvents.onNext { events ->
            onEvents(
                events
            )
        })
    }

    private fun onEvents(events: ConnectivityManager.Events) {
        when (events) {
            ConnectivityManager.Events.TAG_DISCONNECTED -> showDisconnectSnackbar()
            ConnectivityManager.Events.TAG_DETACHED -> showSnackbar(getString(R.string.gear_detached))
        }
    }

    private fun firmwareUpdatePositiveUI(firmwareUpdateState: FirmwareUpdateState) {
        when (firmwareUpdateState.type) {
            FirmwareUpdateState.Type.ERROR -> {
                hideBottomDownloadProgress()
            }
            FirmwareUpdateState.Type.TRANSFER_PROGRESS -> {
                showBottomDownloadProgress(firmwareUpdateState.transferProgress())
            }
            FirmwareUpdateState.Type.TRANSFERRED -> {
                hideBottomDownloadProgress()
            }
        }
    }

    private fun firmwareUpdateNegativeUI(error: Throwable) {
        PrintLogger.e(TAG, "applyFirmware error: " + error.message)
        hideBottomDownloadProgress()
    }

    private fun fwUpdateStateListener() {
        subscriptions.add(
            homeViewModel
                .state
                .tapError { error ->
                    PrintLogger.d(TAG, "isResumed: $isResumed")
                    firmwareUpdateNegativeUI(error)
                }
                .onNext { firmwareUpdateState ->
                    PrintLogger.d(TAG, "isResumed: $isResumed")
                    firmwareUpdatePositiveUI(firmwareUpdateState)
                })
    }

    private fun showBottomDownloadProgress(progress: Int) {
        viewDownloadProgress.visibility = View.VISIBLE
        viewDownloadProgress.setProgress(progress)
        shiftUpRecycleView()
    }

    private fun hideBottomDownloadProgress() {
        if (viewDownloadProgress.visibility != View.VISIBLE) {
            return
        }
        viewDownloadProgress.visibility = View.GONE
        shiftDownRecycleView()
    }

    private fun shiftUpRecycleView() {
        val params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.BELOW, viewTop.id)
        params.addRule(RelativeLayout.ABOVE, viewDownloadProgress.id)
        recyclerView.layoutParams = params
    }

    private fun shiftDownRecycleView() {
        val params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.BELOW, viewTop.id)
        recyclerView.layoutParams = params
    }
}