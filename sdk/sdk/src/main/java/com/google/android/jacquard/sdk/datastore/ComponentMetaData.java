package com.google.android.jacquard.sdk.datastore;
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

import com.google.android.jacquard.sdk.model.Vendor;
import com.google.auto.value.AutoValue;

import java.util.Map;

/** Requires for parsing the data loaded from cloud json. */
@AutoValue
public abstract class ComponentMetaData {

  /** Returns Map of Vendors with its id as Map key. */
  public abstract Map<String, Vendor> vendors();

  /** Creator method for {@link ComponentMetaData}. */
  public static ComponentMetaData of(Map<String, Vendor> vendors) {
    return new AutoValue_ComponentMetaData(vendors);
  }
}
