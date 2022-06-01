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

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol.DFUStatusRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DFUStatusResponse;
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
 * Unit tests for {@link DfuStatusCommand}
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class DfuStatusCommandTest {

  private final static String VID = "74-a8-ce-54";
  private final static String PID = "8a-66-50-f4";

  private DfuStatusCommand dfuStatusCommand;

  @Before
  public void setup() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    dfuStatusCommand = new DfuStatusCommand(VID, PID, Component.TAG_ID);
  }

  @Test
  public void getRequest() {
    // Act
    Request request = dfuStatusCommand.getRequest();
    // Assert
    assertThat(request.getComponentId()).isEqualTo(Component.TAG_ID);
    assertThat(request.getOpcode()).isEqualTo(Opcode.DFU_STATUS);
    assertThat(request.getDomain()).isEqualTo(Domain.DFU);
    assertThat(
        request.getExtension(DFUStatusRequest.dfuStatus).getVendorId())
        .isEqualTo(StringUtils.getInstance().hexStringToInteger(VID));
    assertThat(
        request.getExtension(DFUStatusRequest.dfuStatus).getProductId())
        .isEqualTo(StringUtils.getInstance().hexStringToInteger(PID));
  }

  @Test
  public void parseResponse_errorUnsupported() {
    // Arrange
    Response response = Response.newBuilder().setStatus(Status.ERROR_UNSUPPORTED).setId(0).build();
    // Act
    Result<DFUStatusResponse> dfuResponse = dfuStatusCommand.responseErrorCheck(response.toByteArray());
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
    int currentCrc = 1;
    int currentSize = 2;
    int finalCrc = 3;
    int finalSize = 4;
    DFUStatusResponse dFUStatusResponse = DFUStatusResponse.newBuilder()
        .setComponent(Component.TAG_ID).setCurrentCrc(currentCrc).setCurrentSize(currentSize)
        .setFinalCrc(finalCrc)
        .setFinalSize(finalSize)
        .build();
    Response response = Response.newBuilder()
        .setExtension(DFUStatusResponse.dfuStatus, dFUStatusResponse).setId(1)
        .setComponentId(Component.TAG_ID).setStatus(Status.STATUS_OK).build();
    // Act
    Result<DFUStatusResponse> result = dfuStatusCommand.responseErrorCheck(response.toByteArray());
    // Assert
    DFUStatusResponse dfuStatusResponse = result.success();
    assertThat(dfuStatusResponse.getComponent()).isEqualTo(Component.TAG_ID);
    assertThat(dfuStatusResponse.getCurrentCrc()).isEqualTo(currentCrc);
    assertThat(dfuStatusResponse.getCurrentSize()).isEqualTo(currentSize);
    assertThat(dfuStatusResponse.getFinalCrc()).isEqualTo(finalCrc);
    assertThat(dfuStatusResponse.getFinalSize()).isEqualTo(finalSize);
  }
}
