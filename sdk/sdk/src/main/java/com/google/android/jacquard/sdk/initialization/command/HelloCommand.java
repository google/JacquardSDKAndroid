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
package com.google.android.jacquard.sdk.initialization.command;

import com.google.android.jacquard.sdk.JqExtensionRegistry;
import com.google.android.jacquard.sdk.command.ProtoCommandRequest;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.protobuf.InvalidProtocolBufferException;

/** Command to send hello command to ujt. */
public class HelloCommand extends ProtoCommandRequest<JacquardProtocol.Response> {

  @Override
  public Result<JacquardProtocol.Response> parseResponse(byte[] protoResponse) {
    try {
      JacquardProtocol.Response resp =
          JacquardProtocol.Response.parseFrom(protoResponse, JqExtensionRegistry.instance);
      return Result.ofSuccess(resp);
    } catch (InvalidProtocolBufferException e) {
      return Result.ofFailure(e);
    }
  }

  @Override
  public JacquardProtocol.Request getRequest() {
    return JacquardProtocol.Request.newBuilder()
        .setOpcode(JacquardProtocol.Opcode.HELLO)
        .setId(getId())
        .setDomain(JacquardProtocol.Domain.BASE)
        .build();
  }
}
