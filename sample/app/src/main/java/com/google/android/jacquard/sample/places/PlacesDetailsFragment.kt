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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sample.places.PlacesDetailsViewModel.State.Type.ERROR
import com.google.android.jacquard.sample.places.PlacesDetailsViewModel.State.Type.PLACES
import com.google.android.jacquard.sample.places.db.PlaceItem
import com.google.android.jacquard.sample.utilities.Util.showSnackBar
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Signal
import java.text.SimpleDateFormat
import java.util.*

class PlacesDetailsFragment : Fragment(), OnMapReadyCallback {
    companion object {
        private val TAG = PlacesDetailsFragment::class.java.simpleName
    }

    private val dateFormat = SimpleDateFormat(
        "hh:mm aa",
        Locale.getDefault()
    )
    private val subscriptions: MutableList<Signal.Subscription> = mutableListOf()
    private val viewModel by lazy { getViewModel<PlacesDetailsViewModel>() }
    private lateinit var toolbar: Toolbar
    private var map: GoogleMap? = null
    private lateinit var mapView: MapView
    private var selectedPlaceTimeStamp: Long = 0
    private var placeItems: List<PlaceItem> = mutableListOf()

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        addMarkers(placeItems)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        subscriptions.add(viewModel.stateSignal.onNext { state: PlacesDetailsViewModel.State ->
            this.onState(state)
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(view)
        initView(view)
        selectedPlaceTimeStamp = PlacesDetailsFragmentArgs.fromBundle(requireArguments())
            .selectedPlace
        viewModel
            .getPlacesFromDb(selectedPlaceTimeStamp)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_places_details, container, false)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        unsubscribe()
        mapView.onDestroy()
        super.onDestroy()
    }

    private fun addMarkers(placeItems: List<PlaceItem>) {
        PrintLogger.d(TAG, "Adding markers, map status :$map")
        map?.let {
            for (placeItem in placeItems) {
                val latLng = LatLng(placeItem.latitude, placeItem.longitude)
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    .title(dateFormat.format(Date(placeItem.timestamp)))
                if (selectedPlaceTimeStamp == placeItem.timestamp) {
                    PrintLogger.d(TAG, "addMarkers() keep default marker.")
                    toolbar.title = placeItem.title
                    val cameraPosition = CameraPosition.Builder()
                        .target(latLng)
                        .zoom(19f).build()
                    it.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                    it.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                    val marker: Marker? = it.addMarker(markerOptions)
                    marker!!.showInfoWindow()
                } else {
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    it.addMarker(markerOptions)
                }
            }
        }
    }

    private fun unsubscribe() {
        for (subscription in subscriptions) {
            subscription.unsubscribe()
        }
        subscriptions.clear()
    }

    private fun initView(view: View) {
        mapView = view.findViewById(R.id.map)
    }

    private fun initToolbar(root: View) {
        toolbar = root.findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { viewModel.backArrowClick() }
    }

    private fun onState(state: PlacesDetailsViewModel.State) {
        when (state.type) {
            PLACES -> {
                PrintLogger.d(TAG, "onState() :: assign state.")
                placeItems = state.places()
                addMarkers(placeItems)
            }
            ERROR -> showSnackBar(
                view, state.error()
            )
        }
    }
}