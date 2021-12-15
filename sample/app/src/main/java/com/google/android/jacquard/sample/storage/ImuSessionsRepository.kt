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
import com.google.android.jacquard.sample.imu.db.ImuSessionDao
import com.google.android.jacquard.sample.imu.db.JqSessionInfo

/**
 * Imu sessions data repository class.
 */
class ImuSessionsRepository(application: Application) {
    private val dao: ImuSessionDao = JQRoomDatabase.getDatabase(application).imuSessions()
    private val getSessions: LiveData<List<JqSessionInfo>> = dao.allImuSessions

    fun getImuSessions(): LiveData<List<JqSessionInfo>> {
        return getSessions
    }

    fun insert(sessionInfo: JqSessionInfo) {
        JQRoomDatabase.databaseWriteExecutor.execute { dao.insert(sessionInfo) }
    }

    fun delete(sessionInfo: JqSessionInfo) {
        JQRoomDatabase.databaseWriteExecutor.execute { dao.delete(sessionInfo) }
    }

    fun deleteAll(tagSerialNumber: String) {
        JQRoomDatabase.databaseWriteExecutor.execute { dao.deleteAll(tagSerialNumber) }
    }
}