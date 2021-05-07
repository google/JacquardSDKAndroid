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
package com.google.android.jacquard.sdk.dfu;

import retrofit2.Call;

public class FakeCloudManager<T> implements CloudManager{

  private boolean success;
  private T body;
  private Throwable error;

  public FakeCloudManager<T> setSuccess(boolean success) {
    this.success = success;
    return this;
  }

  public FakeCloudManager<T> setBody(T body) {
    this.body = body;
    return this;
  }

  public FakeCloudManager<T> setError(Throwable error) {
    this.error = error;
    return this;
  }

  @Override
  public Call<RemoteDfuInfo> getDeviceFirmware(String clientId, String vid, String pid, String mid,
      String currentVersion, String tagVersion, String obfuscatedComponentId, String platform,
      String countryCode, String sdkVersion) {
    return new FakeCall<>(success).setBody(body).setError(error);
  }
}
