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

import static com.google.android.jacquard.sdk.util.Objects.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Helper function for list operations. */
public final class Lists {

  private Lists() {
    // Empty
  }

  /**
   * Returns an unmodifiable copy of the array.
   *
   * @param  <E> the class of the objects in the list
   * @param  array the array for which an unmodifiable {@link List} is to be returned.
   * @return an unmodifiable {@link List} of the specified array.
   */
  public static <E> List<E> unmodifiableCopyOf(E[] array) {
    checkNotNull(array);
    List<E> list = Arrays.asList(array);
    return Collections.unmodifiableList(list);
  }

  /**
   * Returns an unmodifiable copy of the list.
   *
   * @param  <E> the class of the objects in the list
   * @param  list the list for which an unmodifiable {@link List} is to be returned.
   * @return an unmodifiable {@link List} of the specified list.
   */
  public static <E> List<E> unmodifiableCopyOf(List<E> list) {
    checkNotNull(list);
    return Collections.unmodifiableList(list);
  }


  /**
   * Returns an unmodifiable list of elements.
   *
   * @param  <E> the class of the objects in the list
   * @param  elements the array for which an unmodifiable {@link List} is to be returned.
   * @return an unmodifiable {@link List} of the specified list.
   */
  @SafeVarargs
  public static <E> List<E> unmodifiableListOf(E... elements) {
    checkNotNull(elements);
    List<E> list = Arrays.asList(elements);
    return Collections.unmodifiableList(list);
  }

  /** Builder for an unmodifiable list. */
  public static class UnmodifiableListBuilder<E> {

    private final List<E> list = new ArrayList<>();

    /**
     * Appends the specified element to the end of this list.
     * @param arg element to be appended to this list
     */
    public UnmodifiableListBuilder<E> add(E arg) {
      list.add(arg);
      return this;
    }

    /** Returns an unmodifiable list of elements. */
    public List<E> build() {
      return Collections.unmodifiableList(list);
    }
  }
}
