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
package com.google.android.jacquard.sdk.initialization;

import com.google.android.jacquard.sdk.command.ProtoCommandRequest;
import com.google.android.jacquard.sdk.model.Peripheral.WriteType;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.protobuf.GeneratedMessageLite;

/** A request in flight. */
class PendingRequest<ProtoRequest extends ProtoCommandRequest, ProtoResponse extends GeneratedMessageLite.ExtendableMessage<?, ?>> {

  /** The Request to send to the tag. */
  final ProtoRequest request;
  /** The Write type for the request. */
  final WriteType writeType;
  /** A Signal to emitting the response. */
  final Signal< byte[]> response;
  /** The number of retries if the request fails. */
  int retries;
  /**
   * Timeout duration of this request in milliseconds.
   */
  long timeout;

  /**
   * Creates a pending request used by {@link Transport}.
   * @param request the Request to send to the tag
   * @param writeType the Write type for the request.
   * @param retries the number of retries if the request fails.
   * @param response emitting a single response
   */
  public PendingRequest(ProtoRequest request, WriteType writeType, int retries,
                        Signal<byte[]> response, long timeout) {
    this.request = request;
    this.writeType = writeType;
    this.retries = retries;
    this.response = response;
    this.timeout = timeout;
  }
}
