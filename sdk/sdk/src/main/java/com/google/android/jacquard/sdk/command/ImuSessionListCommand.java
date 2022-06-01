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
import com.google.android.jacquard.sdk.imu.model.ImuSessionInfo;
import com.google.android.jacquard.sdk.model.Component;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionTrialListRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionTrialListResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Ujt command to fetch IMU session list. Once this command is executed, ujt will send {@link
 * ImuSessionListNotification} per {@link ImuSessionInfo}.
 */
public class ImuSessionListCommand extends ProtoCommandRequest<Boolean> {

  @Override
  public Result<Boolean> parseResponse(byte[] respByte) {
    try {
      JacquardProtocol.Response response =
              JacquardProtocol.Response.parseFrom(respByte, JqExtensionRegistry.instance);
      setResponseId(response.getId());
      DataCollectionTrialListResponse dcResponse = response.getExtension(
              DataCollectionTrialListResponse.trialList);
      if (response.getStatus() != JacquardProtocol.Status.STATUS_OK) {
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
    return Request.newBuilder()
        .setComponentId(Component.TAG_ID)
        .setId(getId())
        .setOpcode(Opcode.DATA_COLLECTION_TRIAL_LIST)
        .setDomain(Domain.DATA_COLLECTION)
        .setExtension(
            DataCollectionTrialListRequest.trialList,
            DataCollectionTrialListRequest.getDefaultInstance())
        .build();
  }
}
