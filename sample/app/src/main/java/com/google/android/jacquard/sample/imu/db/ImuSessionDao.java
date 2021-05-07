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

package com.google.android.jacquard.sample.imu.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ImuSessionDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insert(JqSessionInfo sessionInfo);

  @Query("DELETE FROM imu_sessions where tagSerialNumber = :tagSerialNumber")
  void deleteAll(String tagSerialNumber);

  @Delete
  void delete(JqSessionInfo sessionInfo);

  @Query("SELECT * FROM imu_sessions ORDER BY imuSessionId DESC")
  LiveData<List<JqSessionInfo>> getAllImuSessions();

  @Query("SELECT * FROM imu_sessions WHERE imuSessionId = :imuSessionId")
  JqSessionInfo getImuSession(String imuSessionId);
}
