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

package com.google.android.jacquard.sample.storage

import android.app.Application
import androidx.lifecycle.LiveData
import com.google.android.jacquard.sample.places.db.PlaceItem
import com.google.android.jacquard.sample.places.db.PlaceItemDao

/**
 * Place data repository class.
 */
class PlacesRepository(app: Application) {
    private var dao: PlaceItemDao
    private var getPlaces: LiveData<List<PlaceItem>>

    init {
        val db = JQRoomDatabase.getDatabase(app)
        dao = db.placeData()
        getPlaces = dao.all
    }

    fun getPlaces(): LiveData<List<PlaceItem>> {
        return getPlaces
    }

    fun insert(placeItem: PlaceItem) {
        JQRoomDatabase.databaseWriteExecutor.execute { dao.insert(placeItem) }
    }

    fun delete(id: Int) {
        JQRoomDatabase.databaseWriteExecutor.execute { dao.deleteRecordById(id) }
    }

    fun deleteAll() {
        JQRoomDatabase.databaseWriteExecutor.execute { dao.deleteAllRecords() }
    }

    fun getAllRecordsForDate(
        selectedDateTime: Long,
        dayAfterTime: Long
    ): LiveData<List<PlaceItem>> {
        return dao.getAllRecordsForDate(selectedDateTime, dayAfterTime)
    }
}