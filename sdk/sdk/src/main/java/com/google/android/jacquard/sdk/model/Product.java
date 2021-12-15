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
import com.google.gson.annotations.SerializedName;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;
import java.util.List;

/** Defines Product type of the Gear. */
@GenerateTypeAdapter
@AutoValue
public abstract class Product {

  /** Unique identifier of the product. */
  public abstract String id();

  /** Name of the product. */
  public abstract String name();

  /** Image of the product. */
  public abstract String image();

  /**
   * List of supported capabilities for this product.
   */
  public abstract List<Capability> capabilities();

  public static Product of(String id, String name, String image, List<Capability> capabilities) {
    return new AutoValue_Product(id, name, image, capabilities);
  }

  public enum Capability {
    /**
     * <code>LED = 0;</code>
     */
    @SerializedName("0")
    LED,
    /**
     * <code>GESTURE = 1;</code>
     */
    @SerializedName("1")
    GESTURE,
    /**
     * <code>TOUCH_DATA_STREAM = 2;</code>
     */
    @SerializedName("2")
    TOUCH_DATA_STREAM,
    /**
     * <code>HAPTIC = 3;</code>
     */
    @SerializedName("3")
    HAPTIC;
  }
}
