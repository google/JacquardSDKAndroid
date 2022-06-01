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

import com.google.android.jacquard.sdk.JqExtensionRegistry;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.TouchMode;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataChannelRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataStreamState;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Command for setting the {@link TouchMode} for a component.
 * <p>
 * A command can be send to a connected tag via
 * {@link com.google.android.jacquard.sdk.tag.ConnectedJacquardTag#enqueue(ProtoCommandRequest)}
 */
public class SetTouchModeCommand extends ProtoCommandRequest<Response> {

  private final Component component;
  private final TouchMode touchMode;

  /**
   * Creates a new instance of SetTouchModeCommand and when executed will set TouchMode to the
   * component.
   * @param component the connected component to change touch mode for.
   * @param touchMode the touch mode to apply.
   */
  public SetTouchModeCommand(Component component, TouchMode touchMode) {
    this.component = component;
    this.touchMode = touchMode;
  }

  @Override
  public Result<Response> parseResponse(byte[] respByte) {
    try {
      JacquardProtocol.Response response =
          JacquardProtocol.Response.parseFrom(respByte, JqExtensionRegistry.instance);
      return Result.ofSuccess(response);
    } catch (InvalidProtocolBufferException e) {
      return Result.ofFailure(e);
    }
  }

  @Override
  public Request getRequest() {
    DataChannelRequest.Builder builder = DataChannelRequest.newBuilder();

    switch (touchMode) {
      case GESTURE:
        builder.setInference(DataStreamState.DATA_STREAM_ENABLE);
        builder.setTouch(DataStreamState.DATA_STREAM_DISABLE);
        break;
      case CONTINUOUS:
        builder.setInference(DataStreamState.DATA_STREAM_DISABLE);
        builder.setTouch(DataStreamState.DATA_STREAM_ENABLE);
        break;
    }

    return Request
        .newBuilder()
        .setComponentId(component.componentId())
        .setId(getId())
        .setOpcode(Opcode.GEAR_DATA)
        .setDomain(Domain.GEAR)
        .setExtension(DataChannelRequest.data, builder.build())
        .build();
  }
}
