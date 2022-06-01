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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

/**
 * Data class to bundle vendor id, product id & module id together.
 */
@AutoValue
public abstract class VidPidMid {

    /**
     * Vendor id. Should be in the format xx-xx-xx-xx.
     */
    public abstract String vid();

    /**
     * Product id. Should be in the format xx-xx-xx-xx.
     */
    public abstract String pid();

    /**
     * Loadable module id. Should be in the format xx-xx-xx-xx.
     */
    @Nullable
    public abstract String mid();

    public static Builder builder() {
        return new AutoValue_VidPidMid.Builder();
    }

    /**
     * Builder for {@link VidPidMid}
     */
    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder vid(@NonNull String vid);

        public abstract Builder pid(@NonNull String pid);

        public abstract Builder mid(@Nullable String mid);

        public abstract VidPidMid build();
    }
}
