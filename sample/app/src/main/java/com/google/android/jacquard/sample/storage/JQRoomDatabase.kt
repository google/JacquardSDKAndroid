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

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.android.jacquard.sample.imu.db.ImuSessionDao
import com.google.android.jacquard.sample.imu.db.JqSessionInfo
import com.google.android.jacquard.sample.places.db.PlaceItem
import com.google.android.jacquard.sample.places.db.PlaceItemDao
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Database(entities = [JqSessionInfo::class, PlaceItem::class], version = 1, exportSchema = false)
abstract class JQRoomDatabase : RoomDatabase() {
    abstract fun imuSessions(): ImuSessionDao
    abstract fun placeData(): PlaceItemDao

    companion object {
        private var INSTANCE: JQRoomDatabase? = null
        private const val NUMBER_OF_THREADS = 4
        internal val databaseWriteExecutor: ExecutorService =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS)

        @Synchronized
        fun getDatabase(context: Context): JQRoomDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    JQRoomDatabase::class.java, "jq_Database"
                ).build()
            }
            return INSTANCE!!
        }
    }
}