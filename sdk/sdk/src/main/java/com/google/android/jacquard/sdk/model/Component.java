package com.google.android.jacquard.sdk.model;

/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static com.google.android.jacquard.sdk.util.Lists.unmodifiableCopyOf;

import androidx.annotation.Nullable;
import com.google.android.jacquard.sdk.model.Product.Capability;
import com.google.auto.value.AutoValue;
import java.util.List;

/** Class describing a entity that has a set of {@link Capability} that can be executed. */
@AutoValue
public abstract class Component {

  /** Unique id for the Tag Component. */
  public static final int TAG_ID = 0;

  /** Creates a new instance of Component. */
  public static Component of(int componentId, Vendor vendor, Product product,
      List<Capability> capabilities, @Nullable Revision revision, @Nullable String serialNumber) {
    return new AutoValue_Component(componentId, vendor, product, unmodifiableCopyOf(capabilities),
        revision, serialNumber);
  }

  /** The Id of the component. Used when executing commands.  */
  public abstract int componentId();

  /** The vendor of this component. */
  public abstract Vendor vendor();

  /** The product associated with this component. */
  public abstract Product product();

  /** List of supported capabilities. */
  public abstract List<Capability> gearCapabilities();

  /**
   * Firmware version of the device .
   */
  @Nullable
  public abstract Revision version();

  /**
   * Manufacturer serial number.
   */
  @Nullable
  public abstract String serialNumber();
}
