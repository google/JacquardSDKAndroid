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
package com.google.android.jacquard.sdk.model;

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;
import java.util.List;

/** Defines Capabilities assign to specific component from cloud. */
@GenerateTypeAdapter
@AutoValue
public abstract class ComponentCapability {

    /** Returns vendor id for the Component. */
    public abstract long vendorId();

    /** Returns product id for the Component. */
    public abstract long productId();

    /** Returns list of capability ids for the Component. */
    public abstract List<Long> capabilityIds();
}
