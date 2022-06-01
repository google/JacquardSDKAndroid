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
package com.google.android.jacquard.sample.imu.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ImuSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(sessionInfo: JqSessionInfo)

    @Query("DELETE FROM imu_sessions where tagSerialNumber = :tagSerialNumber")
    fun deleteAll(tagSerialNumber: String)

    @Delete
    fun delete(sessionInfo: JqSessionInfo)

    @get:Query("SELECT * FROM imu_sessions ORDER BY imuSessionId DESC")
    val allImuSessions: LiveData<List<JqSessionInfo>>

    @Query("SELECT * FROM imu_sessions WHERE imuSessionId = :imuSessionId")
    fun getImuSession(imuSessionId: String): JqSessionInfo?
}