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
import com.google.android.jacquard.sdk.imu.model.ImuSessionInfo;
import com.google.android.jacquard.sdk.model.Component;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionTrialListRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionTrialListResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;

/**
 * Ujt command to fetch IMU session list. Once this command is executed, ujt will send {@link
 * ImuSessionListNotification} per {@link ImuSessionInfo}.
 */
public class ImuSessionListCommand implements CommandRequest<Boolean> {

  @Override
  public Result<Boolean> parseResponse(Response response) {
    if (response.getStatus() != Status.STATUS_OK) {
      Throwable error = CommandResponseStatus.from(response.getStatus().getNumber());
      return Result.ofFailure(error);
    }
    if (!response.hasExtension(DataCollectionTrialListResponse.trialList)) {
      return Result
          .ofFailure(new IllegalStateException(
              "Response does not contain DataCollectionStopResponse.stop"));
    }
    return Result.ofSuccess(true);
  }

  @Override
  public Request getRequest() {
    return Request.newBuilder()
        .setComponentId(Component.TAG_ID)
        .setId(0)
        .setOpcode(Opcode.DATA_COLLECTION_TRIAL_LIST)
        .setDomain(Domain.DATA_COLLECTION)
        .setExtension(DataCollectionTrialListRequest.trialList,
            DataCollectionTrialListRequest.getDefaultInstance())
        .build();
  }
}
