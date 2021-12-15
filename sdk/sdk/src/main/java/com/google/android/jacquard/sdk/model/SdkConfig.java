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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.auto.value.AutoValue;

/**
 * Data class for sdk initialization.
 */
@AutoValue
public abstract class SdkConfig {

  public static SdkConfig of(@NonNull String clientId, @NonNull String apiKey,
      @Nullable String cloudEndpointUrl) {
    return new AutoValue_SdkConfig(clientId, apiKey, cloudEndpointUrl);
  }

  /**
   * The client id.
   */
  public abstract String clientId();

  /**
   * The api key.
   */
  public abstract String apiKey();

  /**
   * Cloud endpoint url. Defaults to production cloud endpoint.
   */
  @Nullable
  public abstract String cloudEndpointUrl();
}
