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

import com.google.android.jacquard.sdk.connection.CommandResponseStatus;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.model.Component;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatus;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatusRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatusResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;

/**
 * Ujt command get current data collection status.
 */
public class DataCollectionStatusCommand implements CommandRequest<DataCollectionStatus> {

  @Override
  public Result<DataCollectionStatus> parseResponse(Response response) {
    if (response.getStatus() != Status.STATUS_OK) {
      Throwable error = CommandResponseStatus.from(response.getStatus().getNumber());
      return Result.ofFailure(error);
    }
    if (!response.hasExtension(DataCollectionStatusResponse.status)) {
      return Result
          .ofFailure(new IllegalStateException(
              "Response does not contain DataCollectionStatusResponse.status"));
    }
    DataCollectionStatusResponse dcResponse = response
        .getExtension(DataCollectionStatusResponse.status);
    return Result.ofSuccess(dcResponse.getDcStatus());
  }

  @Override
  public Request getRequest() {
    return Request.newBuilder()
        .setComponentId(Component.TAG_ID)
        .setId(0)
        .setOpcode(Opcode.DATA_COLLECTION_STATUS)
        .setDomain(Domain.DATA_COLLECTION)
        .setExtension(DataCollectionStatusRequest.status,
            DataCollectionStatusRequest.newBuilder().build())
        .build();
  }
}
