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

import androidx.annotation.NonNull;

import com.google.android.jacquard.sdk.JqExtensionRegistry;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.imu.exception.InvalidStateDCException;
import com.google.android.jacquard.sdk.imu.model.ImuSessionInfo;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionTrialDataRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionTrialDataResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.protobuf.GeneratedMessageLite;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Ujt command to request for imu session data. Once this command is executed, start observing
 * {@link ConnectedJacquardTag#getDataTransport()}.
 */
public class GetImuSessionDataCommand extends ProtoCommandRequest<DataCollectionTrialDataResponse> {

  private static final String TAG = GetImuSessionDataCommand.class.getSimpleName();
  private final ImuSessionInfo sessionData;
  private final int offset;

  public GetImuSessionDataCommand(@NonNull ImuSessionInfo sessionData, int offset) {
    this.sessionData = sessionData;
    PrintLogger.d(TAG, "Offset ## " + offset);
    this.offset = offset >= 0 ? offset : 0;
  }

  @Override
  public GeneratedMessageLite.GeneratedExtension<Response, DataCollectionTrialDataResponse>
      getExtension() {
    return DataCollectionTrialDataResponse.trialData;
  }

  @Override
  public Result<DataCollectionTrialDataResponse> parseResponse(byte[] respByte) {
    try {
      JacquardProtocol.Response response =
          JacquardProtocol.Response.parseFrom(respByte, JqExtensionRegistry.instance);
      setResponseId(response.getId());
      DataCollectionTrialDataResponse dcResponse = response.getExtension(getExtension());
      if (response.getStatus() != JacquardProtocol.Status.STATUS_OK) {
        return Result.ofFailure(new InvalidStateDCException(dcResponse.getDcStatus()));
      }
      return Result.ofSuccess(dcResponse);
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
    DataCollectionTrialDataRequest request = DataCollectionTrialDataRequest.newBuilder()
        .setCampaignId(sessionData.campaignId())
        .setProductId(sessionData.productId())
        .setSessionId(sessionData.dcSessionId())
        .setTrialId(sessionData.imuSessionId())
        .setSubjectId(sessionData.subjectId())
        .setSensorId(sessionData.sensor().id())
        .setOffset(offset)
        .build();
    return Request.newBuilder()
        .setComponentId(Component.TAG_ID)
        .setId(getId())
        .setOpcode(Opcode.DATA_COLLECTION_TRIAL_DATA)
        .setDomain(Domain.DATA_COLLECTION)
        .setExtension(DataCollectionTrialDataRequest.trialData, request)
        .build();
  }
}
