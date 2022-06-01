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
package com.google.android.jacquard.sdk.command;

import androidx.annotation.NonNull;

import com.google.android.jacquard.sdk.connection.Result;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.protobuf.GeneratedMessageLite;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.concurrent.atomic.AtomicInteger;

/** Base interface for all command send to the tag. */
public abstract class ProtoCommandRequest<T> {

  private int requestId;
  private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);
  private int respId;

  public ProtoCommandRequest() {
    requestId = ID_GENERATOR.getAndIncrement();
  }

  /**
   * All classes must call this api to include id to the ProtoRequest
   * @return unique id ranging from 1 to 255.
   */
  public final int getId() {
    // Protocol v2 only uses 1...255 for request id.
    requestId = requestId % 255;
    return requestId;
  }

  /**
   * Parses the raw response received from the tag into a {@link Result} object.
   *
   * @param response the response received from the tag
   * @return a Result with either the parsed data or an error.
   */
  public abstract Result<T> parseResponse(byte[] response);

  /** Returns the request to be sent to the tag. */
  public abstract <ProtoRequest extends GeneratedMessageLite.ExtendableMessage<?, ?>> ProtoRequest getRequest();

  /** Returns an extension from proto to parse the base response to the required form. */
  public GeneratedMessageLite.GeneratedExtension<?, ?> getExtension() {
    return null;
  }

  /** Returns the response id from the tag. */
  public int responseId() {
    return respId;
  }

  /**
   * Sets the response id from the tag.
   * Should be called by the ProtoCommandRequest if excludeResponseErrorChecks returns true.
   */
  public void setResponseId(int id) {
    respId = id;
  }

  /**
   * Excludes response status check for the implementation.
   * Need to call setResponseId(id) from parseResponse(byte[]) if returned true.
   */
  public boolean excludeResponseErrorChecks() {
    return false; // Default
  }

  public Result<T> responseErrorCheck(byte[] response) {
    return !excludeResponseErrorChecks()
        ? checkIfError(response)
        : parseResponse(response);
  }

  private Result<T> checkIfError(byte[] respBytes) {
    try {
      JacquardProtocol.Response response = JacquardProtocol.Response.parseFrom(respBytes);
      setResponseId(response.getId());
      if (response.getStatus() != JacquardProtocol.Status.STATUS_OK) {
        return Result.ofFailure(
            new Exception("Command Failure: " + response.getStatus().name()));
      }
    } catch (InvalidProtocolBufferException e) {
      return Result.ofFailure(e);
    }
    return parseResponse(respBytes);
  }

  @NonNull
  @Override
  public String toString() {
    return "ProtoCommandRequest{" +
        "request=" + getRequest() +
        '}';
  }
}
