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
package com.google.android.jacquard.sdk.dfu;

import java.io.IOException;
import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FakeCall<T> implements Call<T> {

  private boolean success;
  private T body;
  private Throwable error;

  FakeCall(boolean success) {
    this.success = success;
  }

  public FakeCall setBody(T body) {
    this.body = body;
    return this;
  }

  public FakeCall setError(Throwable error) {
    this.error = error;
    return this;
  }

  @Override
  public Response<T> execute() throws IOException {
    return null;
  }

  @Override
  public void enqueue(Callback<T> callback) {
      if (success) {
        callback.onResponse(/* call= */this, Response.success(body));
      } else {
        callback.onFailure(/* call= */this, error);
      }
  }

  @Override
  public boolean isExecuted() {
    return false;
  }

  @Override
  public void cancel() {

  }

  @Override
  public boolean isCanceled() {
    return false;
  }

  @Override
  public Call<T> clone() {
    return null;
  }

  @Override
  public Request request() {
    return null;
  }

  @Override
  public Timeout timeout() {
    return null;
  }
}
