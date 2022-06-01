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
 */
package com.google.android.jacquard.sample.places.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

/**
 * Dao interface for @link [PlaceItem]
 */
@Dao
interface PlaceItemDao {
    @get:Query("SELECT * FROM placeItem ORDER BY timestamp DESC")
    val all: LiveData<List<PlaceItem>>

    @Query("SELECT * FROM placeitem WHERE timestamp >= :selectedDate and timestamp < :dayAfterSelectedDate")
    fun getAllRecordsForDate(
        selectedDate: Long,
        dayAfterSelectedDate: Long
    ): LiveData<List<PlaceItem>>

    @Query("DELETE FROM placeitem WHERE uid = :id")
    fun deleteRecordById(id: Int): Int

    @Query("DELETE FROM placeitem")
    fun deleteAllRecords()

    @Insert
    fun insert(placesItem: PlaceItem)

    @Delete
    fun delete(placesItem: PlaceItem)
}