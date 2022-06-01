/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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
import com.google.android.jacquard.sdk.imu.exception.InvalidStateDCException;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionMetadata;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStartRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStartResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatus;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * UJT command to start Imu samples streaming. </br> Observe {@link
 * ConnectedJacquardTag#getRawData()} to receive {@link
 * com.google.android.jacquard.sdk.imu.model.ImuStream}.
 */
public class StartImuStreamingCommand extends ProtoCommandRequest<Boolean> {

  private final DataCollectionMetadata metadata;

  /** Initializes the command. */
  public StartImuStreamingCommand(DataCollectionMetadata metadata) {
    this.metadata = metadata;
  }

  @Override
  public Result<Boolean> parseResponse(byte[] respByte) {
    try {
      JacquardProtocol.Response response =
          JacquardProtocol.Response.parseFrom(respByte, JqExtensionRegistry.instance);
      setResponseId(response.getId());
      if (!response.hasExtension(DataCollectionStartResponse.start)) {
        return Result.ofFailure(
                new IllegalStateException(
                        "Response does not contain DataCollectionStartResponse.start."));
      }
      DataCollectionStartResponse dcResponse =
              response.getExtension(DataCollectionStartResponse.start);
      if (response.getStatus() != JacquardProtocol.Status.STATUS_OK
              || dcResponse.getDcStatus().equals(DataCollectionStatus.DATA_COLLECTION_LOW_BATTERY)
              || dcResponse.getDcStatus().equals(DataCollectionStatus.DATA_COLLECTION_LOW_STORAGE)) {
        return Result.ofFailure(new InvalidStateDCException(dcResponse.getDcStatus()));
      }
      return Result.ofSuccess(true);
    } catch (InvalidProtocolBufferException e) {
      return Result.ofFailure(e);
    }
  }

  @Override
  public boolean excludeResponseErrorChecks() {
    return true;
  }

  @Override
  public Request getRequest() {
    DataCollectionStartRequest request =
        DataCollectionStartRequest.newBuilder().setMetadata(metadata).build();
    return Request.newBuilder()
        .setComponentId(Component.TAG_ID)
        .setId(getId())
        .setOpcode(Opcode.DATA_COLLECTION_START)
        .setDomain(Domain.DATA_COLLECTION)
        .setExtension(DataCollectionStartRequest.start, request)
        .build();
  }
}
