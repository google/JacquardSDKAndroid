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
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import java.util.Locale;

/** Data class for firmware revisions. */
@AutoValue
public abstract class Revision implements Comparable<Revision> {

  /** Creates a new instance of Revision. */
  public static Revision create(int major, int minor, int patch) {
    return new AutoValue_Revision(major, minor, patch);
  }

  /** The major part of the revision. */
  public abstract int major();

  /** The minor part of the revision. */
  public abstract int minor();

  /** The patch part of the revision. */
  public abstract int patch();

  @Override
  public int compareTo(Revision revision) {
    int m = major() - revision.major();
    int n = minor() - revision.minor();
    int p = patch() - revision.patch();

    return m != 0 ? m : (n != 0 ? n : p);
  }

  @Override
  public final String toString() {
    return String.format(Locale.US, "%d.%d.%d", major(), minor(), patch());
  }

  /** Returns a "%03d%03d%03d" formatted representation of the version number. */
  public String toZeroString() {
    return String.format(Locale.US, "%03d%03d%03d", major(), minor(), patch());
  }

  /**
   * Parse a zero-padded version string into a Revision
   *
   * @param zeroPadded input "%03d%03d%03d" string
   * @return a Revision from the input string
   */
  public static Revision fromZeroString(String zeroPadded) {
    if (zeroPadded == null || zeroPadded.length() != 9) {
      return Revision.create(0, 0, 0);
    }
    int major = Integer.parseInt(zeroPadded.substring(0, 3));
    int minior = Integer.parseInt(zeroPadded.substring(3, 6));
    int micro = Integer.parseInt(zeroPadded.substring(6, 9));
    return Revision.create(major, minior, micro);
  }

  public static TypeAdapter<Revision> typeAdapter(Gson gson) {
    return new AutoValue_Revision.GsonTypeAdapter(gson);
  }
}