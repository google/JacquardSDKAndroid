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
import com.google.android.jacquard.sdk.model.Component;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionEraseAllDataRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionEraseTrialDataRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Ujt command to erase specific or all IMU Sessions.
 */
public class EraseImuSessionCommand extends ProtoCommandRequest<Boolean> {

  private final ImuSessionInfo trialData;
  private final boolean eraseAll;

  /**
   * @param trialData Send null to erase all imu sessions.
   */
  public EraseImuSessionCommand(@NonNull ImuSessionInfo trialData) {
    this(trialData, trialData == null);
  }

  private EraseImuSessionCommand(ImuSessionInfo data, boolean eraseAll) {
    this.trialData = data;
    this.eraseAll = eraseAll;
  }

  @Override
  public Result<Boolean> parseResponse(byte[] respByte) {
    try {
      JacquardProtocol.Response response =
              JacquardProtocol.Response.parseFrom(respByte, JqExtensionRegistry.instance);
      setResponseId(response.getId());
      if (response.getStatus() != JacquardProtocol.Status.STATUS_OK) {
        JacquardProtocol.DataCollectionEraseTrialDataResponse dcResponse = response.getExtension(
                JacquardProtocol.DataCollectionEraseTrialDataResponse.eraseTrialData);
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
    DataCollectionEraseTrialDataRequest eraseTrialDataRequest = null;
    if (trialData != null) {
      eraseTrialDataRequest = DataCollectionEraseTrialDataRequest.newBuilder()
          .setCampaignId(trialData.campaignId())
          .setProductId(trialData.productId())
          .setSessionId(trialData.dcSessionId())
          .setSubjectId(trialData.subjectId())
          .setTrialId(trialData.imuSessionId())
          .build();
    }
    Request.Builder builder = Request
        .newBuilder()
        .setComponentId(Component.TAG_ID)
        .setDomain(Domain.DATA_COLLECTION)
        .setId(getId());
    if (eraseAll) {
      builder.setOpcode(Opcode.DATA_COLLECTION_DATA_ERASE)
          .setExtension(DataCollectionEraseAllDataRequest.eraseAllData,
              DataCollectionEraseAllDataRequest.getDefaultInstance());
    } else {
      builder.setOpcode(Opcode.DATA_COLLECTION_TRIAL_DATA_ERASE)
          .setExtension(DataCollectionEraseTrialDataRequest.eraseTrialData, eraseTrialDataRequest);
    }
    return builder.build();
  }
}
