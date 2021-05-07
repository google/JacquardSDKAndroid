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

/**
 * Acts as a Key to hold list of capabilities, which are assigned to combination of Vendor and
 * Product.
 */
@AutoValue
public abstract class ComponentKey<K,V> {

    /** Return first key for ComponentKey. */
    public abstract K key1();

    /** Return second key for ComponentKey. */
    public abstract V key2();

    /**
     * Return the ComponentKey for the combination of {@link ComponentKey#key1()} and {@link
     * ComponentKey#key2()}.
     */
    public static <K, V> ComponentKey<K,V> of(K key1, V key2) {
        return new AutoValue_ComponentKey(key1, key2);
    }
}
