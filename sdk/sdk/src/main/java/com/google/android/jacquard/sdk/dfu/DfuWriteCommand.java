/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.jacquard.sdk.dfu;

import com.google.android.jacquard.sdk.JqExtensionRegistry;
import com.google.android.jacquard.sdk.command.ProtoCommandRequest;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.atap.jacquard.protocol.JacquardProtocol.DFUWriteRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DFUWriteResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageLite;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Command for sending firmware binary to device.
 *
 * <p>A command can be send to a connected tag via {@link
 * com.google.android.jacquard.sdk.tag.ConnectedJacquardTag#enqueue(ProtoCommandRequest)}
 */
final class DfuWriteCommand extends ProtoCommandRequest<DFUWriteResponse> {

  static final int DFU_BLOCK_SIZE = 128;
  private static final String TAG = DfuWriteCommand.class.getSimpleName();

  private final int componentId;
  private final int offset;
  private final byte[] firmware;

  DfuWriteCommand(int componentId, int offset, byte[] firmware) {
    this.componentId = componentId;
    this.offset = offset;
    this.firmware = firmware;
  }

  @Override
  public GeneratedMessageLite.GeneratedExtension<Response, DFUWriteResponse> getExtension() {
    return DFUWriteResponse.dfuWrite;
  }

  @Override
  public Result<DFUWriteResponse> parseResponse(byte[] respByte) {
    try {
      JacquardProtocol.Response response =
          JacquardProtocol.Response.parseFrom(respByte, JqExtensionRegistry.instance);
      PrintLogger.d(TAG, "response = " + response);
      return Result.ofSuccess(response.getExtension(getExtension()));
    } catch (InvalidProtocolBufferException e) {
      return Result.ofFailure(e);
    }
  }

  @Override
  public Request getRequest() {
    int blockSize = Math.min(DFU_BLOCK_SIZE, firmware.length - offset);
    DFUWriteRequest dFUWriteRequest =
        DFUWriteRequest.newBuilder()
            .setData(ByteString.copyFrom(firmware, offset, blockSize))
            .setOffset(offset)
            .build();
    return Request.newBuilder()
        .setComponentId(componentId)
        .setId(getId())
        .setOpcode(Opcode.DFU_WRITE)
        .setDomain(Domain.DFU)
        .setExtension(DFUWriteRequest.dfuWrite, dFUWriteRequest)
        .build();
  }
}
