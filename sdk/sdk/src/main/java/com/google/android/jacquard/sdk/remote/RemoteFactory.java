/*
 * Copyright 2021 Google LLC
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
package com.google.android.jacquard.sdk.remote;

import android.content.res.Resources;
import com.google.android.jacquard.sdk.datastore.ComponentMetaData;
import com.google.android.jacquard.sdk.datastore.DataProvider;

/**
 * Provides implementation of {@link RemoteFunction}
 */
public final class RemoteFactory {

  static RemoteFunction remoteFunction;

  public static void initialize(Resources resources) {
    // This will be replaced by cloud Implementation of RemoteFunction.
    remoteFunction = new LocalRemoteFunctionImpl(resources);
  }

  /**
   * Returns Implmentation of RemoteFunction based on Type of service.
   *
   * @return Implmentation of {@link RemoteFunction}.
   */
  public static synchronized RemoteFunction remoteInstance() {
    if (remoteFunction == null) {
      throw new Error("RemoteFunction not initialized");
    }

    return remoteFunction;
  }

  public static void createDataProvider(ComponentMetaData componentMetaData) {
    DataProvider.create(componentMetaData.vendors());
  }
}
