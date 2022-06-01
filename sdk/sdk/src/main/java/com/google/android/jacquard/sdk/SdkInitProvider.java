/*
 * Copyright 2021 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.jacquard.sdk;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.jacquard.sdk.remote.RemoteFactory;

/**
 * ContentsProviders are initialized and called directly after the application’s object is created.
 * This class creates an instance of {@link JacquardManagerImpl} and makes sure that it is
 * accessible via {@link JacquardManager#getInstance()} when the application is launched.
 */
public final class SdkInitProvider extends ContentProvider {

  @Override
  public boolean onCreate() {
    RemoteFactory.initialize(getContext().getResources());
    JacquardManagerImpl.instance = new JacquardManagerImpl(getContext());
    return true; // successfully loaded.
  }

  @Nullable
  @Override
  public Cursor query(
      @NonNull Uri uri,
      @Nullable String[] projection,
      @Nullable String selection,
      @Nullable String[] selectionArgs,
      @Nullable String sortOrder) {
    // Empty
    return null;
  }

  @Nullable
  @Override
  public String getType(@NonNull Uri uri) {
    // Empty
    return null;
  }

  @Nullable
  @Override
  public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
    // Empty
    return null;
  }

  @Override
  public int delete(
      @NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
    // Empty
    return 0;
  }

  @Override
  public int update(
      @NonNull Uri uri,
      @Nullable ContentValues values,
      @Nullable String selection,
      @Nullable String[] selectionArgs) {
    // Empty
    return 0;
  }
}
