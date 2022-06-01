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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;
import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;

/** TypeFactory for all serializable auto values. */
@GsonTypeAdapterFactory
public abstract class SdkTypeAdapterFactory  implements TypeAdapterFactory {

  public static TypeAdapterFactory create() {
    return new AutoValueGson_SdkTypeAdapterFactory();
  }

  public static Gson gson() {
    return new GsonBuilder().disableHtmlEscaping().registerTypeAdapterFactory(SdkTypeAdapterFactory.create()).create();
  }

  public static Gson runtimeGsonTypeAdapterFactory() {
    return new GsonBuilder().disableHtmlEscaping()
        .registerTypeAdapterFactory(GenerateTypeAdapter.FACTORY).create();
  }
}