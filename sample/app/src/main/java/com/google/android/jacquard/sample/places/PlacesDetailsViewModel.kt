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

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.jacquard.sample.places.db.PlaceItem
import com.google.android.jacquard.sample.storage.PlacesRepository
import com.google.android.jacquard.sample.utilities.DateUtil.getZeroDateTime
import com.google.android.jacquard.sample.utilities.DateUtil.incrementDateByOne
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Signal
import com.google.auto.value.AutoOneOf
import java.util.*


class PlacesDetailsViewModel(
    private var navController: NavController,
    private var repository: PlacesRepository) : ViewModel() {
    companion object {
        private val TAG = PlacesDetailsViewModel::class.java.simpleName
    }

    val stateSignal: Signal<State> = Signal.create()
    private val subscriptions: MutableList<Signal.Subscription> = mutableListOf()
    private lateinit var listLiveData: LiveData<List<PlaceItem>>
    private var observer: Observer<List<PlaceItem>>? = null

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

    }

    /**
     * Fetches places data from local database.
     */
    fun getPlacesFromDb(selectedPlace: Long) {
        val selectedDate: Date = getZeroDateTime(Date(selectedPlace))
        listLiveData = repository.getAllRecordsForDate(
            selectedDate.time,
            incrementDateByOne(selectedDate).time
        )
        observer?.let {
            listLiveData.removeObserver(observer!!)
        }
        observer =
            Observer<List<PlaceItem>> { placeItems: List<PlaceItem> ->
                this.getPlacesList(
                    placeItems
                )
            }
        listLiveData.observeForever(observer!!)
    }

    private fun getPlacesList(placeItems: List<PlaceItem>) {
        PrintLogger.d(TAG, "getPlacesList() :: number of records " + placeItems.size)
        stateSignal.next(State.ofPlaces(placeItems))
    }

    @AutoOneOf(State.Type::class)
    abstract class State {
        abstract val type: Type

        abstract fun places(): List<PlaceItem>
        abstract fun error(): String
        enum class Type {
            PLACES, ERROR
        }

        companion object {
            fun ofPlaces(placeItems: List<PlaceItem>): State {
                return AutoOneOf_PlacesDetailsViewModel_State.places(placeItems)
            }

            fun ofError(errorMsg: String): State {
                return AutoOneOf_PlacesDetailsViewModel_State.error(errorMsg)
            }
        }
    }
}