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

import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.LatLng
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.Preferences
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.places.PlacesListAdapter.ItemClickListener
import com.google.android.jacquard.sample.places.db.PlaceItem
import com.google.android.jacquard.sample.storage.PlacesRepository
import com.google.android.jacquard.sample.utilities.DateUtil.getUserReadableString
import com.google.android.jacquard.sdk.command.GestureNotificationSubscription
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Signal
import com.google.auto.value.AutoOneOf
import java.io.IOException
import java.util.*
import kotlin.collections.HashSet


class PlacesListViewModel(
    private val connectivityManager: ConnectivityManager,
    private val navController: NavController,
    private val geocoder: Geocoder,
    private val repository: PlacesRepository,
    private val preferences: Preferences
) : ViewModel(), ItemClickListener {
    companion object {
        private val TAG = PlacesListViewModel::class.java.simpleName

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        private const val UPDATE_INTERVAL_IN_MILLISECONDS = 10000

        /**
         * The fastest rate for active location updates. Updates will never be more frequent than this
         * value.
         */
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2
    }

    val stateSignal: Signal<State> = Signal.create()
    private val subscriptions: MutableList<Signal.Subscription> = mutableListOf()
    private lateinit var selectedPlaceItem: PlacesItem
    private lateinit var listLiveData: LiveData<List<PlaceItem>>
    private lateinit var observer: Observer<List<PlaceItem>>
    private var savedLocations = 0

    override fun onMoreOptionClick(placesItem: PlacesItem) {
        PrintLogger.d(TAG, "onMoreOptionClick() :: more options clicked.")
        this.selectedPlaceItem = placesItem
        stateSignal.next(State.ofMoreOptions())
    }

    override fun onMapClick(placeItem: PlacesItem) {
        PrintLogger.d(TAG, "onMapClick() :: map item clicked.")
        navController
            .navigate(
                PlacesListFragmentDirections.actionPlacesListFragmentToPlacesDetailsFragment(
                    placeItem.time
                )
            )
    }

    /**
     * Returns location request.
     */
    fun getLocationRequest(): LocationRequest {
        return LocationRequest.create()
            .setInterval(UPDATE_INTERVAL_IN_MILLISECONDS.toLong())
            .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS.toLong())
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
    }

    /**
     * Handles the back arrow click in the toolbar.
     */
    fun backArrowClick() {
        navController.popBackStack()
    }

    override fun onCleared() {
        super.onCleared()
        for (subscription in subscriptions) {
            subscription.unsubscribe()
        }
        subscriptions.clear()
        listLiveData.removeObserver(observer)
    }

    /**
     * Listens to gesture to trigger places ability.
     */
    fun listenToGesture(gestureAssigned: Int): Signal.Subscription {
        return preferences.currentTag?.let {
            connectivityManager.getConnectedJacquardTag(it.address())
                .distinctUntilChanged()
                .switchMap { tag ->
                    tag.subscribe(
                        GestureNotificationSubscription()
                    )
                }
                .onNext { gesture ->
                    PrintLogger.d(
                        TAG,
                        "listenToGesture() :: gesture triggered : " + gesture.gestureType()
                            .description
                                + " >> " + gestureAssigned
                    )
                    if (gestureAssigned == gesture.gestureType().id) {
                        PrintLogger.d(
                            TAG,
                            "listenToGesture() :: gesture matched : " + gesture.gestureType()
                                .id + " >> "
                                    + gestureAssigned
                        )
                        stateSignal.next(State.ofGestureTriggered())
                    }
                }
        } ?: Signal.Subscription()
    }

    /**
     * Fetches places data from local database.
     */
    fun getPlacesFromDb(context: Context) {
        listLiveData = repository.getPlaces()
        observer =
            Observer<List<PlaceItem>> { placeItems ->
                getPlacesList(
                    placeItems,
                    context
                )
            }
        listLiveData.observeForever(observer)
    }


    /**
     * Returns number of locations saved.
     */
    fun getSavedLocations(): Int {
        return savedLocations
    }

    /**
     * Deletes selected record from database.
     */
    fun deleteItem() {
        repository.delete(selectedPlaceItem.id)
        stateSignal.next(State.ofDeleted())
    }

    /**
     * Deletes all places record from database.
     */
    fun deleteAllRecords() {
        repository.deleteAll()
        savedLocations = 0
        stateSignal.next(State.ofDeleted())
    }


    /**
     * Save this location place item to database.
     */
    fun savePlace(context: Context, location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        try {
            val addresses = geocoder.getFromLocation(
                latLng.latitude, latLng.longitude,
                1
            ) // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            val address = addresses[0].getAddressLine(
                0
            ) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
            val city = addresses[0].locality
            // Saving to local database
            val placeItem = PlaceItem(
                location.latitude, location.longitude, city,
                address, Date().time
            )
            repository.insert(placeItem)
            stateSignal.next(State.ofPlacesItem())
        } catch (e: IOException) {
            PrintLogger.e(TAG, e.message)
            stateSignal.next(State.ofError(context.getString(R.string.location_details_error)))
        }
    }

    private fun getPlacesList(placeItemsList: List<PlaceItem>, context: Context) {
        savedLocations = placeItemsList.size
        val placesItems: MutableList<PlacesItem> = mutableListOf()
        val dateHeaders: MutableSet<String> = HashSet()
        for (placeItem in placeItemsList) {
            val header = getUserReadableString(Date(placeItem.timestamp), context)
            PrintLogger.d(
                TAG,
                "Header >> $header"
            )
            if (!dateHeaders.contains(header)) {
                dateHeaders.add(header)
                val placesItem =
                    PlacesItem(0, LatLng(0.0, 0.0), header, "", 0, PlacesItem.Type.SECTION)
                placesItems.add(placesItem)
            }
            val placesItem = PlacesItem(
                placeItem.uid,
                LatLng(placeItem.latitude, placeItem.longitude),
                placeItem.title, placeItem.subtitle, placeItem.timestamp,
                PlacesItem.Type.PLACES_ITEM
            )
            placesItems.add(placesItem)
        }
        stateSignal.next(State.ofPlacesItems(placesItems))
    }

    @AutoOneOf(State.Type::class)
    abstract class State {
        abstract val type: Type

        abstract fun moreOptions()
        abstract fun placesItem()
        abstract fun placesItems(): List<PlacesItem>
        abstract fun gestureTriggered()
        abstract fun error(): String
        abstract fun deleted()
        enum class Type {
            MORE_OPTIONS, ERROR, PLACES_ITEM, GESTURE_TRIGGERED, PLACES_ITEMS, DELETED
        }

        companion object {
            fun ofError(errorMsg: String): State {
                return AutoOneOf_PlacesListViewModel_State.error(errorMsg)
            }

            fun ofPlacesItem(): State {
                return AutoOneOf_PlacesListViewModel_State.placesItem()
            }

            fun ofMoreOptions(): State {
                return AutoOneOf_PlacesListViewModel_State.moreOptions()
            }

            fun ofPlacesItems(placesItems: List<PlacesItem>): State {
                return AutoOneOf_PlacesListViewModel_State.placesItems(placesItems)
            }

            fun ofGestureTriggered(): State {
                return AutoOneOf_PlacesListViewModel_State.gestureTriggered()
            }

            fun ofDeleted(): State {
                return AutoOneOf_PlacesListViewModel_State.deleted()
            }
        }
    }
}