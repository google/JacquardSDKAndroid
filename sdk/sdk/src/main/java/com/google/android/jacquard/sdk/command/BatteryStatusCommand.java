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
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.atap.jacquard.protocol.JacquardProtocol.BatteryStatusRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.BatteryStatusResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.protobuf.GeneratedMessageLite;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Command for request the current battery status.
 * <p>
 * A command can be send to a connected tag via
 * {@link com.google.android.jacquard.sdk.tag.ConnectedJacquardTag#enqueue(ProtoCommandRequest)}
 */
public class BatteryStatusCommand extends ProtoCommandRequest<BatteryStatus> {

  @Override
  public GeneratedMessageLite.GeneratedExtension<Response, BatteryStatusResponse> getExtension() {
    return BatteryStatusResponse.batteryStatusResponse;
  }

  @Override
  public Result<BatteryStatus> parseResponse(byte[] respByte) {
    try {
      JacquardProtocol.Response response =
          JacquardProtocol.Response.parseFrom(respByte, JqExtensionRegistry.instance);
      BatteryStatusResponse batteryStatusResponse = response.getExtension(getExtension());
      return Result.ofSuccess(BatteryStatus.of(batteryStatusResponse));
    } catch (InvalidProtocolBufferException e) {
      return Result.ofFailure(e);
    }
  }

  @Override
  public Request getRequest() {
    BatteryStatusRequest batteryStatusRequest = BatteryStatusRequest.newBuilder()
        .setReadBatteryLevel(true).setReadChargingStatus(true).build();

    return Request
        .newBuilder()
        .setComponentId(0)
        .setId(getId())
        .setOpcode(Opcode.BATTERY_STATUS)
        .setDomain(Domain.BASE)
        .setExtension(BatteryStatusRequest.batteryStatusRequest, batteryStatusRequest)
        .build();
  }
}
