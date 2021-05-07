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

package com.google.android.jacquard.sample.dialog;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.jacquard.sample.dialog.BottomSheetDialogFragment.BottomSheetMenuListener;

/**
 * Bottom sheet dialog item class.
 */
public class BottomSheetItem implements Parcelable {

  private final int id;
  private final String name;
  private BottomSheetMenuListener bottomSheetMenuListener;

  public BottomSheetItem(int id, String name, BottomSheetMenuListener bottomSheetMenuListener) {
    this.id = id;
    this.name = name;
    this.bottomSheetMenuListener = bottomSheetMenuListener;
  }

  protected BottomSheetItem(Parcel in) {
    id = in.readInt();
    name = in.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(name);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<BottomSheetItem> CREATOR =
      new Creator<BottomSheetItem>() {
        @Override
        public BottomSheetItem createFromParcel(Parcel in) {
          return new BottomSheetItem(in);
        }

        @Override
        public BottomSheetItem[] newArray(int size) {
          return new BottomSheetItem[size];
        }
      };

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public BottomSheetMenuListener getBottomSheetMenuListener() {
    return bottomSheetMenuListener;
  }
}
