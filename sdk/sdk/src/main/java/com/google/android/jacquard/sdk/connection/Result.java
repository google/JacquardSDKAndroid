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
package com.google.android.jacquard.sdk.connection;

import com.google.auto.value.AutoOneOf;

/** Data class for holding the result of a tag command.  */
@AutoOneOf(Result.Type.class)
public abstract class Result<Res> {

  public enum Type {
    SUCCESS, FAILURE
  }

  public abstract Type getType();

  public abstract Res success();

  public abstract Throwable failure();

  public static <Res> Result<Res> ofSuccess(Res response) {
    return AutoOneOf_Result.success(response);
  }

  public static <Res> Result<Res> ofFailure(Throwable throwable) {
    return AutoOneOf_Result.failure(throwable);
  }

}
