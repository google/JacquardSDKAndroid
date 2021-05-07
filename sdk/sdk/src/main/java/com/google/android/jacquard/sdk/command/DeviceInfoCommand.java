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
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.atap.jacquard.protocol.JacquardProtocol.DeviceInfoRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DeviceInfoResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;

/**
 * Command for getting device information.
 */
public class DeviceInfoCommand implements CommandRequest<DeviceInfo> {

  private static final String TAG = DeviceInfoCommand.class.getSimpleName();
  private final int componentId;

  /**
   * Creates a new instance of DeviceInfoCommand.
   *
   * @param componentId componentId for the command.
   */
  //need component id in parameter
  public DeviceInfoCommand(int componentId) {
    this.componentId = componentId;
  }

  @Override
  public Result<DeviceInfo> parseResponse(Response response) {
    PrintLogger.d(TAG, "response = " + response);
    if (!response.hasExtension(DeviceInfoResponse.deviceInfo)) {
      return Result
          .ofFailure(new IllegalStateException("Response does not contain device information."));
    }
    DeviceInfoResponse deviceInfoResponse = response
        .getExtension(DeviceInfoResponse.deviceInfo);
    return Result.ofSuccess(
        componentId == Component.TAG_ID ? DeviceInfo
            .ofTag(deviceInfoResponse) : DeviceInfo.ofGear(deviceInfoResponse));
  }

  @Override
  public Request getRequest() {
    return getBaseRequestBuilder().build();
  }

  private Request.Builder getBaseRequestBuilder() {
    DeviceInfoRequest deviceInfoRequest = DeviceInfoRequest.newBuilder()
        .setComponent(componentId)
        .build();
    Request.Builder builder = Request
        .newBuilder()
        .setComponentId(componentId)
        .setId(0)
        .setOpcode(Opcode.DEVICEINFO)
        .setExtension(DeviceInfoRequest.deviceInfo, deviceInfoRequest);
    if (componentId == Component.TAG_ID) {
      return builder.setDomain(Domain.BASE);
    } else {
      return builder.setDomain(Domain.GEAR);
    }
  }
}
