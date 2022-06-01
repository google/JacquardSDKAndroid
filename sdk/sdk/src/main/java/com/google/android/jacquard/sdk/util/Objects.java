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
package com.google.android.jacquard.sdk.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Helper functions for Objects. */
public final class Objects {

  private Objects() {
    // Empty
  }

  /**
   * Returns {@code true} if the provided reference is non-{@code null} otherwise returns {@code
   * false}.
   *
   * @param obj a reference to be checked against {@code null}
   * @return {@code true} if the provided reference is non-{@code null} otherwise {@code false}
   * @apiNote This method exists to be used as a {@link java.util.function.Predicate}, {@code
   * filter(Objects::nonNull)}
   */
  public static boolean nonNull(Object obj) {
    return obj != null;
  }

  /**
   * Ensures that an object reference passed as a parameter to the calling method is not null.
   *
   * @param reference an object reference
   * @return the non-null reference that was validated
   * @throws NullPointerException if {@code reference} is null
   */
  @NonNull
  public static <T> T checkNotNull(@Nullable T reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
    return reference;
  }
}
