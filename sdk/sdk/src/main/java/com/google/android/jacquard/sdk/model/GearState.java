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
package com.google.android.jacquard.sdk.model;

import com.google.auto.value.AutoOneOf;

/** The attached state of a gear. */
@AutoOneOf(GearState.Type.class)
public abstract class GearState {

  /** Returns the component in attached state. */
  public abstract Component attached();

  public abstract Type getType();

  public enum Type {
    ATTACHED, DETACHED
  }

  public abstract void detached();

  public static GearState ofAttached(Component component) {
    return AutoOneOf_GearState.attached(component);
  }

  public static GearState ofDetached() {
    return AutoOneOf_GearState.detached();
  }
}
