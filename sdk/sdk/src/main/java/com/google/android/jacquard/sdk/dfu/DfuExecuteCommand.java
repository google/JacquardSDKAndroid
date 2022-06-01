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

import com.google.android.jacquard.sdk.command.ProtoCommandRequest;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol.DFUExecuteRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.UpdateSchedule;

/**
 * Command for install the firmware binary.
 * <p>
 * A command can be send to a connected tag via
 * {@link com.google.android.jacquard.sdk.tag.ConnectedJacquardTag#enqueue(ProtoCommandRequest)}
 * </p>
 */
final class DfuExecuteCommand extends ProtoCommandRequest<Boolean> {

  private final int componentId;
  private final String vid;
  private final String pid;

  public DfuExecuteCommand(String vid, String pid, int componentId) {
    this.componentId = componentId;
    this.vid = vid;
    this.pid = pid;
  }

  @Override
  public Result<Boolean> parseResponse(byte[] respByte) {
    return Result.ofSuccess(true);
  }

  @Override
  public Request getRequest() {
    DFUExecuteRequest dFUExecuteRequest = DFUExecuteRequest.newBuilder()
        .setProductId(StringUtils.getInstance().hexStringToInteger(pid))
        .setVendorId(StringUtils.getInstance().hexStringToInteger(vid))
        .setUpdateSched(UpdateSchedule.UPDATE_NOW).build();
    return Request
        .newBuilder()
        .setComponentId(componentId)
        .setId(getId())
        .setOpcode(Opcode.DFU_EXECUTE)
        .setDomain(Domain.DFU)
        .setExtension(DFUExecuteRequest.dfuExecute, dFUExecuteRequest)
        .build();
  }
}