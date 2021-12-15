package com.google.android.jacquard.sdk.model;
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

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;
import java.util.List;

/** Defines Vendor type of the Gear. */
@GenerateTypeAdapter
@AutoValue
public abstract class Vendor {

  /** Unique identifier of the Vendor. */
  public abstract String id();

  /** Name of the Vendor. */
  public abstract String name();

  /**
   * List of products for this vendor.
   */
  public abstract List<Product> products();

  public static Vendor of(String id, String name, List<Product> products) {
    return new AutoValue_Vendor(id, name, products);
  }
}
