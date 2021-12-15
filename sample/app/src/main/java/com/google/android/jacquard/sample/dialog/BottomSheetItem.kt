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

package com.google.android.jacquard.sample.dialog

import android.os.Parcel
import android.os.Parcelable

/**
 * Bottom sheet dialog item class.
 */
class BottomSheetItem(
    val id: Int,
    val name: String?,
    var bottomSheetMenuListener: BottomSheetDialogFragment.BottomSheetMenuListener?
) : Parcelable {

    constructor(parcel: Parcel) : this(parcel.readInt(), parcel.readString(), null)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BottomSheetItem> {
        override fun createFromParcel(parcel: Parcel): BottomSheetItem {
            return BottomSheetItem(parcel)
        }

        override fun newArray(size: Int): Array<BottomSheetItem?> {
            return arrayOfNulls(size)
        }
    }
}