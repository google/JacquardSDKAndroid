/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk.command;

import com.google.android.jacquard.sdk.connection.Result;
import com.google.atap.jacquard.protocol.JacquardProtocol.BatteryStatusRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.BatteryStatusResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;

/**
 * Command for request the current battery status.
 * <p>
 * A command can be send to a connected tag via
 * {@link com.google.android.jacquard.sdk.tag.ConnectedJacquardTag#enqueue(CommandRequest)}
 */
public class BatteryStatusCommand implements CommandRequest<BatteryStatus> {

  @Override
  public Result<BatteryStatus> parseResponse(Response response) {
    if (!response.hasExtension(BatteryStatusResponse.batteryStatusResponse)) {
      return Result
          .ofFailure(new IllegalStateException("Response does not contain battery information"));
    }
    BatteryStatusResponse batteryStatusResponse = response
        .getExtension(BatteryStatusResponse.batteryStatusResponse);

    return Result.ofSuccess(BatteryStatus.of(batteryStatusResponse));

  }

  @Override
  public Request getRequest() {
    BatteryStatusRequest batteryStatusRequest = BatteryStatusRequest.newBuilder()
        .setReadBatteryLevel(true).setReadChargingStatus(true).build();

    return Request
        .newBuilder()
        .setComponentId(0)
        .setId(0)
        .setOpcode(Opcode.BATTERY_STATUS)
        .setDomain(Domain.BASE)
        .setExtension(BatteryStatusRequest.batteryStatusRequest, batteryStatusRequest)
        .build();
  }
}
