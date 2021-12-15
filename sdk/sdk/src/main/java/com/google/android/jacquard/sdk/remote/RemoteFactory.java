/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.android.jacquard.sdk.util.StringUtils;

/**
 * Provides implementation of {@link RemoteFunction}
 */
public final class RemoteFactory {

  public enum Server {
    LOCAL
  }

  private static final Server SERVICE = Server.LOCAL;

  /**
   * Returns Implmentation of RemoteFunction based on Type of service.
   *
   * @param resources Resources to load local file from Raw resources.
   * @return Implmentation of {@link RemoteFunction}.
   */
  public static RemoteFunction remoteInstance(Resources resources) {
    switch (SERVICE) {
      case LOCAL:
        return new LocalRemoteFunctionImpl(resources);
    }
    throw new IllegalArgumentException("Server is not defined");
  }

  public static void createDataProvider(ComponentMetaData componentMetaData) {
    DataProvider.create(componentMetaData.vendors());
  }
}
