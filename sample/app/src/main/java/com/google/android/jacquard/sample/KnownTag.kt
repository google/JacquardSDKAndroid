/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.android.jacquard.sample

import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.tag.JacquardTag

/** Class representing a known tag. */
data class KnownTag(
    val identifier: String,
    val displayName: String,
    val pairingSerialNumber: String,
    @Transient val rssiValue: Signal<Int>?
) : JacquardTag {
    override fun address(): String {
        return identifier
    }

    override fun displayName(): String {
        return displayName
    }

    override fun rssiSignal(): Signal<Int>? {
        return rssiValue
    }
}

