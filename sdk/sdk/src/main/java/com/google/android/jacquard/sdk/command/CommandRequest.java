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
package com.google.android.jacquard.sdk.command;

import com.google.android.jacquard.sdk.connection.Result;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;

/**
 * Base interface for all command send to the tag.
 * @param <T> the type of the response.
 */
public interface CommandRequest<T> {

  /**
   * Parses the raw response received from the tag into a {@link Result} object.
   * @param response the response received from the tag
   * @return a Result with either the parsed data or an error.
   */
  Result<T> parseResponse(Response response);

  /** Returns the request to be sent to the tag. */
  Request getRequest();

}
