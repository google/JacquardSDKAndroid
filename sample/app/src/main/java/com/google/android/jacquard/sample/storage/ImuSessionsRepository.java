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

package com.google.android.jacquard.sample.storage;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.google.android.jacquard.sample.imu.db.ImuSessionDao;
import com.google.android.jacquard.sample.imu.db.JqSessionInfo;
import java.util.List;

public class ImuSessionsRepository {

  private ImuSessionDao dao;
  private LiveData<List<JqSessionInfo>> getSessions;

  public ImuSessionsRepository(Application application) {
    JQRoomDatabase db = JQRoomDatabase.getDatabase(application);
    dao = db.imuSessions();
    getSessions = dao.getAllImuSessions();
  }

  public LiveData<List<JqSessionInfo>> getImuSessions() {
    return getSessions;
  }

  public void insert(JqSessionInfo sessionInfo) {
    JQRoomDatabase.databaseWriteExecutor.execute(() -> {
      dao.insert(sessionInfo);
    });
  }

  public void delete(JqSessionInfo sessionInfo) {
    JQRoomDatabase.databaseWriteExecutor.execute(() -> {
      dao.delete(sessionInfo);
    });
  }

  public void deleteAll(String tagSerialNumber) {
    JQRoomDatabase.databaseWriteExecutor.execute(() -> {
      dao.deleteAll(tagSerialNumber);
    });
  }
}
