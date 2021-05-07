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

package com.google.android.jacquard.sample.places.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity class holding places item details.
 */
@Entity
public class PlaceItem {

  public PlaceItem(double latitude, double longitude, String title, String subtitle,
      long timestamp) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.title = title;
    this.subtitle = subtitle;
    this.timestamp = timestamp;
  }

  @PrimaryKey(autoGenerate = true)
  public int uid;

  public double latitude;

  public double longitude;

  public String title;

  public String subtitle;

  public long timestamp;
}
