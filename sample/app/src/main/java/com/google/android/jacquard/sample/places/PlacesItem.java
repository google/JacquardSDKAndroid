package com.google.android.jacquard.sample.places;

import com.google.android.gms.maps.model.LatLng;
import com.google.auto.value.AutoValue;

/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Model to hold data for places item.
 */
@AutoValue
public abstract class PlacesItem {

  public abstract int id();

  public abstract LatLng latLng();

  public abstract String title();

  public abstract String subTitle();

  public abstract long time();

  public abstract Type type();

  public static Builder builder() {
    return new AutoValue_PlacesItem.Builder();
  }

  /**
   * Enum type for {@link PlacesItem}.
   */
  public enum Type {
    SECTION,
    PLACES_ITEM
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder id(int id);

    public abstract Builder latLng(LatLng latLng);

    public abstract Builder title(String title);

    public abstract Builder subTitle(String subTitle);

    public abstract Builder time(long time);

    public abstract Builder type(Type type);

    public abstract PlacesItem build();
  }
}
