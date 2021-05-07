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
import com.google.android.jacquard.sample.places.db.PlaceItem;
import com.google.android.jacquard.sample.places.db.PlaceItemDao;
import java.util.List;

/**
 * Place data repository class.
 */
public class PlacesRepository {

  private final PlaceItemDao dao;
  private final LiveData<List<PlaceItem>> getPlaces;

  public PlacesRepository(Application application) {
    JQRoomDatabase db = JQRoomDatabase.getDatabase(application);
    dao = db.placeData();
    getPlaces = dao.getAll();
  }

  public LiveData<List<PlaceItem>> getPlaces() {
    return getPlaces;
  }

  public void insert(PlaceItem placeItem) {
    JQRoomDatabase.databaseWriteExecutor.execute(() -> dao.insert(placeItem));
  }

  public void delete(int placeId) {
    JQRoomDatabase.databaseWriteExecutor.execute(() -> dao.deleteRecordById(placeId));
  }

  public void deleteAll() {
    JQRoomDatabase.databaseWriteExecutor.execute(dao::deleteAllRecords);
  }

  public LiveData<List<PlaceItem>> getAllRecordsForDate(long selectedDateTime, long dayAfterTime) {
    return dao.getAllRecordsForDate(selectedDateTime, dayAfterTime);
  }
}
