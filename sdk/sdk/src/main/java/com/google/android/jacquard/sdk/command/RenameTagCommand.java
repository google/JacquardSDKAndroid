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
package com.google.android.jacquard.sdk.command;

import com.google.android.jacquard.sdk.connection.CommandResponseStatus;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.model.Component;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;
import com.google.atap.jacquard.protocol.JacquardProtocol.UJTConfigResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.UJTConfigWriteRequest;

/**
 * Command for renaming the Jacquard tag.
 * <p>
 * A command can be send to a connected tag via
 * {@link com.google.android.jacquard.sdk.tag.ConnectedJacquardTag#enqueue(RenameTagCommand)}
 * </p>
 */
public final class RenameTagCommand implements CommandRequest<String> {

  private final String tagName;

  /**
   * Creates a new instance of RenameTagCommand and when executed will rename the tag with provided
   * tagName.
   *
   * @param tagName the advertising name to rename to the tag.
   */
  public RenameTagCommand(String tagName) {
    this.tagName = tagName;
  }

  @Override
  public Result<String> parseResponse(Response response) {
    if (response.getStatus() != Status.STATUS_OK) {
      Throwable error = CommandResponseStatus.from(response.getStatus().getNumber());
      if (response.getStatus().getNumber() == CommandResponseStatus.ERROR_BAD_PARAM
          .getStatusCode()) {
        return Result.ofFailure(new IllegalArgumentException("Invalid input.", error));
      }
      return Result.ofFailure(error);
    }
    if (!response.hasExtension(UJTConfigResponse.configResponse)) {
      return Result
          .ofFailure(
              new IllegalStateException("Response does not contain rename config information"));
    }
    UJTConfigResponse ujtConfigResponse = response.getExtension(UJTConfigResponse.configResponse);
    return Result.ofSuccess(ujtConfigResponse.getBleConfig().getCustomAdvName());
  }

  @Override
  public Request getRequest() {
    JacquardProtocol.BleConfiguration bleConfiguration = JacquardProtocol.BleConfiguration
        .newBuilder().setCustomAdvName(tagName)
        .build();
    UJTConfigWriteRequest ujtConfigWriteRequest = UJTConfigWriteRequest.newBuilder()
        .setBleConfig(bleConfiguration)
        .build();
    return Request
        .newBuilder()
        .setComponentId(Component.TAG_ID)
        .setId(0)
        .setOpcode(Opcode.CONFIG_WRITE)
        .setDomain(Domain.BASE)
        .setExtension(UJTConfigWriteRequest.configWrite, ujtConfigWriteRequest)
        .build();
  }
}
