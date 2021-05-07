package com.google.android.jacquard.sample.home;

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

/** Model to hold data for {@link HomeTilesListAdapter}. */
@AutoValue
public abstract class HomeTileModel {

  /** Creating object for {@link HomeTileModel}. */
  public static HomeTileModel of(int title, int subTitle, Type type, boolean enabled) {
    return new AutoValue_HomeTileModel(title, subTitle, type, enabled);
  }

  /** Tile title. */
  public abstract int title();

  /** Tile subtitle. */
  public abstract int subTitle();

  /** Type of Tile. */
  public abstract Type type();

  /** Enabled state of Tile. */
  public abstract boolean enabled();

  /** Enum type for {@link HomeTileModel}. */
  public enum Type {
    SECTION,
    TILE_API,
    TILE_SAMPLE_USE_CASE
  }
}
