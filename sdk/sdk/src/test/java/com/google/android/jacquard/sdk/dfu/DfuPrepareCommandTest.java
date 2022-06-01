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

import static com.google.common.truth.Truth.assertThat;

import android.os.Build.VERSION_CODES;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol.DFUPrepareRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link DfuPrepareCommand}
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class DfuPrepareCommandTest {

  private final static String VID = "74-a8-ce-54";
  private final static String PID = "8a-66-50-f4";
  private static final byte[] FIRMWARE = new byte[]{8, 7, 16, 0, 24, 0};

  private DfuPrepareCommand dfuPrepareCommand;

  @Before
  public void setup() {
    dfuPrepareCommand = new DfuPrepareCommand(VID, PID, Component.TAG_ID, FIRMWARE);
  }

  @Test
  public void getRequest() {
    // Act
    Request request = dfuPrepareCommand.getRequest();
    // Assert
    assertThat(request.getComponentId()).isEqualTo(Component.TAG_ID);
    assertThat(request.getOpcode()).isEqualTo(Opcode.DFU_PREPARE);
    assertThat(request.getDomain()).isEqualTo(Domain.DFU);
    assertThat(
        request.getExtension(DFUPrepareRequest.dfuPrepare).getVendorId())
        .isEqualTo(StringUtils.getInstance().hexStringToInteger(VID));
    assertThat(
        request.getExtension(DFUPrepareRequest.dfuPrepare).getProductId())
        .isEqualTo(StringUtils.getInstance().hexStringToInteger(PID));
    assertThat(
        request.getExtension(DFUPrepareRequest.dfuPrepare).getComponent())
        .isEqualTo(Component.TAG_ID);
    assertThat(
        request.getExtension(DFUPrepareRequest.dfuPrepare).getFinalSize())
        .isEqualTo(FIRMWARE.length);
    assertThat(
        request.getExtension(DFUPrepareRequest.dfuPrepare).getFinalCrc())
        .isEqualTo(DfuUtil.crc16(FIRMWARE, FIRMWARE.length));
  }

  @Test
  public void parseResponse_errorUnsupported() {
    // Arrange
    Response response = Response.newBuilder().setStatus(Status.ERROR_UNSUPPORTED).setId(0).build();
    // Act
    Result<Response> dfuResponse = dfuPrepareCommand.responseErrorCheck(response.toByteArray());
    // Assert
    assertThat(
            dfuResponse
                .failure()
                .getMessage()
                .equals("Command Failure: " + Status.ERROR_UNSUPPORTED.name()))
        .isTrue();
  }

  @Test
  public void parseResponse_successResponse() {
    // Arrange
    Response response = Response.newBuilder().setId(1)
        .setComponentId(Component.TAG_ID).setStatus(Status.STATUS_OK).build();
    // Act
    Result<Response> result = dfuPrepareCommand.responseErrorCheck(response.toByteArray());
    // Assert
    assertThat(result.success()).isEqualTo(response);
  }
}
