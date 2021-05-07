/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.jacquard.sdk.dfu;

import com.google.android.jacquard.sdk.command.CommandRequest;
import com.google.android.jacquard.sdk.connection.CommandResponseStatus;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol.DFUPrepareRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;

/**
 * Command for dfu prepare to device.
 * <p>
 * A command can be send to a connected tag via
 * {@link com.google.android.jacquard.sdk.tag.ConnectedJacquardTag#enqueue(DfuPrepareCommand)}
 * </p>
 */
final class DfuPrepareCommand implements CommandRequest<Response> {

  private final int componentId;
  private final String vid;
  private final String pid;
  private final byte[] firmware;

  DfuPrepareCommand(String vid, String pid, int componentId, byte[] firmware) {
    this.componentId = componentId;
    this.vid = vid;
    this.pid = pid;
    this.firmware = firmware;
  }

  @Override
  public Result<Response> parseResponse(Response response) {
    if (response.getStatus() != Status.STATUS_OK) {
      Throwable error = CommandResponseStatus.from(response.getStatus().getNumber());
      return Result.ofFailure(error);
    }
    return Result.ofSuccess(response);
  }

  @Override
  public Request getRequest() {
    DFUPrepareRequest dFUPrepareRequest = DFUPrepareRequest.newBuilder()
        .setProductId(StringUtils.getInstance().hexStringToInteger(pid))
        .setVendorId(StringUtils.getInstance().hexStringToInteger(vid)).setComponent(componentId)
        .setFinalSize(firmware.length).setFinalCrc(DfuUtil.crc16(firmware, firmware.length)).build();
    return Request
        .newBuilder()
        .setComponentId(componentId)
        .setId(0)
        .setOpcode(Opcode.DFU_PREPARE)
        .setDomain(Domain.DFU)
        .setExtension(DFUPrepareRequest.dfuPrepare, dFUPrepareRequest)
        .build();
  }
}