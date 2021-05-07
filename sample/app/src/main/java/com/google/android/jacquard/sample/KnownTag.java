/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.jacquard.sample;

import com.google.android.jacquard.sdk.tag.AdvertisedJacquardTag;
import com.google.android.jacquard.sdk.tag.JacquardTag;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

/** Class representing a known tag. */
@AutoValue
public abstract class KnownTag implements JacquardTag {

  public static KnownTag of(String identifier, String displayName, String pairingSerialNumber) {
    return new AutoValue_KnownTag(identifier, displayName, pairingSerialNumber);
  }

  public static KnownTag of(AdvertisedJacquardTag tag) {
    return of(tag.identifier(), tag.displayName(), tag.pairingSerialNumber());
  }

  public static TypeAdapter<KnownTag> typeAdapter(Gson gson) {
    return new AutoValue_KnownTag.GsonTypeAdapter(gson);
  }

  @Override
  public abstract String identifier();

  @Override
  public abstract String displayName();

  /** Last 4 digit of the UJT serial number. */
  public abstract String pairingSerialNumber();
}
