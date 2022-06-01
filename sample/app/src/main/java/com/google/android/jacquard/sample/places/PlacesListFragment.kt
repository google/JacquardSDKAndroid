/*
 *
 *
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
 */

package com.google.android.jacquard.sample.places

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.dialog.BottomSheetDialogFragment.BottomSheetMenuListener
import com.google.android.jacquard.sample.dialog.BottomSheetItem
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogButtonClickListener
import com.google.android.jacquard.sample.dialog.DialogUtils.Companion.showBottomOptions
import com.google.android.jacquard.sample.dialog.DialogUtils.Companion.showDialog
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sample.utilities.Util.showSnackBar
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.model.Gesture.GestureType
import com.google.android.jacquard.sdk.rx.Signal
import java.util.*

/**
 * Fragment for showing places list.
 */
class PlacesListFragment : Fragment() {
    companion object {
        private val TAG = PlacesListFragment::class.java.simpleName
        private const val CANCEL = 0
        private const val DELETE = 1
        private const val DELETE_ALL = 2
        private const val CHANGE_GESTURE = 3
        private const val PLACES_TIME_OUT = (10 * 1000).toLong()
        private val handler = Handler()
    }

    private lateinit var placesListAdapter: PlacesListAdapter
    private val viewModel by lazy { getViewModel<PlacesListViewModel>() }
    private val subscriptions: MutableList<Signal.Subscription> = mutableListOf()
    private val placesItems: MutableList<PlacesItem> = mutableListOf()
    private lateinit var locationsTxt: TextView
    private var isGestureTriggered = false
    private lateinit var progressBar: LinearLayout
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            PrintLogger.d(TAG, "onLocationResult() : got latest location.")
            progressBar.visibility = View.GONE
            if (isGestureTriggered) {
                PrintLogger.d(TAG, "onLocationResult() : saving location.")
                viewModel.savePlace(requireContext(), locationResult.lastLocation)
                isGestureTriggered = false
                handler.removeCallbacks(locationTimeOut)
            }
        }
    }


    private var locationReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action!!.matches(LocationManager.PROVIDERS_CHANGED_ACTION.toRegex())) {
                startLocationUpdates()
            }
        }
    }

    var optionsMenuBottomSheetListener: BottomSheetMenuListener =
        object : BottomSheetMenuListener {
            override fun onMenuItemClicked(id: Int) {
                when (id) {
                    DELETE_ALL -> showDialog(
                        R.string.delete_all_location,
                        R.string.delete_all_location_desc,
                        R.string.delete,
                        R.string.cancel,
                        object : DefaultDialogButtonClickListener() {
                            override fun buttonClick() {
                                viewModel.deleteAllRecords()
                            }
                        },
                        childFragmentManager
                    )
                    CHANGE_GESTURE -> {
                        PrintLogger.d(
                            TAG,
                            "Change assigned gesture clicked."
                        )
                        getNavController().navigate(
                            PlacesListFragmentDirections.actionPlacesListFragmentToPlacesConfigFragment()
                        )
                    }
                }
            }
        }

    var listItemBottomSheetListener: BottomSheetMenuListener =
        object : BottomSheetMenuListener {
            override fun onMenuItemClicked(id: Int) {
                when (id) {
                    DELETE -> showDialog(
                        R.string.delete_location,
                        R.string.delete_location_desc,
                        R.string.delete,
                        R.string.cancel,
                        object : DefaultDialogButtonClickListener() {
                            override fun buttonClick() {
                                viewModel.deleteItem()
                            }
                        },
                        childFragmentManager
                    )
                }
            }
        }

    val locationTimeOut = Runnable {
        PrintLogger.d(TAG, "locationTimeOut() : location not received yet.")
        isGestureTriggered = false
        progressBar.visibility = View.GONE
        showSnackBar(view, getString(R.string.places_time_out))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel
            .listenToGesture(PlacesListFragmentArgs.fromBundle(requireArguments()).gestureAssigned)
            .let {
                subscriptions.add(
                    it
                )
            }
        subscriptions.add(viewModel.stateSignal.onNext { onState(it) })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_places_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(view)
        initView(view)
        viewModel.getPlacesFromDb(requireContext())
    }

    override fun onDestroy() {
        unsubscribe()
        handler.removeCallbacks(locationTimeOut)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
        requireContext().registerReceiver(
            locationReceiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        )
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        requireContext().unregisterReceiver(locationReceiver)
    }

    private fun unsubscribe() {
        for (subscription in subscriptions) {
            subscription.unsubscribe()
        }
        subscriptions.clear()
    }

    private fun initView(view: View) {
        locationsTxt = view.findViewById(R.id.subTitleTxt)
        progressBar = view.findViewById(R.id.progress)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerview)
        placesListAdapter = PlacesListAdapter(placesItems, viewModel)
        recyclerView.addItemDecoration(
            DividerItemDecoration(requireContext(), LinearLayout.VERTICAL)
        )
        recyclerView.adapter = placesListAdapter
        recyclerView.setRecyclerListener(recyclerListener)
    }

    private fun initToolbar(root: View) {
        root.findViewById<Toolbar>(R.id.toolbar)
            .setNavigationOnClickListener { viewModel.backArrowClick() }
        root.findViewById<ImageView>(R.id.more_options)
            .setOnClickListener { showOptionsMenuDialog() }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!checkPermissions()) {
            PrintLogger.d(TAG, "startLocationUpdates but no permission.")
            return
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationClient
            .requestLocationUpdates(
                viewModel.getLocationRequest(), locationCallback,
                Looper.getMainLooper()
            )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * RecycleListener that completely clears the [com.google.android.gms.maps.GoogleMap]
     * attached to a row in the RecyclerView. Sets the map type to [ ][com.google.android.gms.maps.GoogleMap.MAP_TYPE_NONE] and clears the map.
     */
    private val recyclerListener =
        RecyclerView.RecyclerListener { holder: RecyclerView.ViewHolder ->
            val mapHolder =
                holder as PlacesListAdapter.AbstractViewHolder
            if (mapHolder.map != null) {
                // Clear the map and free up resources by changing the map type to none.
                // Also reset the map when it gets reattached to layout, so the previous map would
                // not be displayed.
                mapHolder.map!!.clear()
                mapHolder.map!!.mapType = GoogleMap.MAP_TYPE_NONE
            }
        }

    private fun showOptionsMenuDialog() {
        showBottomOptions(object : ArrayList<BottomSheetItem>() {
            init {
                add(
                    BottomSheetItem(
                        CHANGE_GESTURE,
                        getString(R.string.change_assign_gesture),
                        optionsMenuBottomSheetListener
                    )
                )
                if (viewModel.getSavedLocations() > 0) {
                    add(
                        BottomSheetItem(
                            DELETE_ALL,
                            getString(R.string.delete_all),
                            optionsMenuBottomSheetListener
                        )
                    )
                }
                add(
                    BottomSheetItem(
                        CANCEL, getString(R.string.cancel),
                        optionsMenuBottomSheetListener
                    )
                )
            }
        }, requireActivity())
    }

    private fun onState(state: PlacesListViewModel.State) {
        PrintLogger.d(TAG, "onState() state : ${state.type}.")
        when (state.type) {
            PlacesListViewModel.State.Type.MORE_OPTIONS -> {
                showBottomOptions(object : ArrayList<BottomSheetItem>() {
                    init {
                        add(
                            BottomSheetItem(
                                DELETE, getString(R.string.delete),
                                listItemBottomSheetListener
                            )
                        )
                        add(
                            BottomSheetItem(
                                CANCEL, getString(R.string.cancel),
                                listItemBottomSheetListener
                            )
                        )
                    }
                }, requireActivity())
            }
            PlacesListViewModel.State.Type.PLACES_ITEMS -> {
                placesItems.clear()
                placesItems.addAll(state.placesItems())
                placesListAdapter.notifyDataSetChanged()
                updateLocationsText(viewModel.getSavedLocations())
            }
            PlacesListViewModel.State.Type.GESTURE_TRIGGERED -> {
                if (checkPermissions()) {
                    progressBar.visibility = View.VISIBLE
                    isGestureTriggered = true
                    handler.removeCallbacks(locationTimeOut)
                    handler.postDelayed(
                        locationTimeOut,
                        PLACES_TIME_OUT
                    )
                }
            }
            PlacesListViewModel.State.Type.ERROR -> showSnackBar(
                view, state.error()
            )
        }
    }

    private fun updateLocationsText(numberOfLocations: Int) {
        locationsTxt.text = if (numberOfLocations > 0) {
            getString(R.string.locations, numberOfLocations)
        } else {
            val assignedGesture =
                GestureType.from(PlacesListFragmentArgs.fromBundle(requireArguments()).gestureAssigned)
            getString(R.string.use_gesture, assignedGesture.description.lowercase())
        }
    }

    private fun checkPermissions(): Boolean {
        return if (!hasPermissions()) {
            false
        } else hasLocationEnabled()
    }

    private fun hasPermissions(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                requireActivity(),
                permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            requireAccess()
            false
        }
    }

    private fun hasLocationEnabled(): Boolean {
        val locationManager = requireContext()
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (LocationManagerCompat.isLocationEnabled(locationManager)) {
            return true
        }
        showSnackBar(view, getString(R.string.location_disabled))
        return false
    }

    private fun requireAccess() {
        showSnackBar(view, getString(R.string.requires_location_access))
    }

    private fun getNavController(): NavController {
        return NavHostFragment.findNavController(this)
    }
}