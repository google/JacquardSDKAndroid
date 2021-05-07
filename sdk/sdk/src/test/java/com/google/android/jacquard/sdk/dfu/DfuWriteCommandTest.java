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

import static com.google.android.jacquard.sdk.dfu.DfuWriteCommand.DFU_BLOCK_SIZE;
import static com.google.common.truth.Truth.assertThat;

import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.atap.jacquard.protocol.JacquardProtocol.DFUWriteRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DFUWriteResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link DfuWriteCommand}
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class DfuWriteCommandTest {

  private static final int OFF_SET = 0;
  private static final byte[] FIRMWARE = new byte[]{8, 7, 16, 0, 24, 0};

  private DfuWriteCommand dfuWriteCommand;

  @Before
  public void setup() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    dfuWriteCommand = new DfuWriteCommand(Component.TAG_ID, OFF_SET, FIRMWARE);
  }

  @Test
  public void getRequest() {
    // Assign
    int blockSize = Math.min(DFU_BLOCK_SIZE, FIRMWARE.length - OFF_SET);
    // Act
    Request request = dfuWriteCommand.getRequest();
    // Assert
    assertThat(request.getComponentId()).isEqualTo(Component.TAG_ID);
    assertThat(request.getId()).isEqualTo(/*expected =*/ 0);
    assertThat(request.getOpcode()).isEqualTo(Opcode.DFU_WRITE);
    assertThat(request.getDomain()).isEqualTo(Domain.DFU);
    assertThat(
        request.getExtension(DFUWriteRequest.dfuWrite).getData())
        .isEqualTo(ByteString.copyFrom(FIRMWARE, OFF_SET, blockSize));
    assertThat(
        request.getExtension(DFUWriteRequest.dfuWrite).getOffset())
        .isEqualTo(OFF_SET);
  }

  @Test
  public void parseResponse_throwsIllegalStateException() {
    // Arrange
    Response response = Response.newBuilder().setStatus(Status.STATUS_OK).setId(0).build();
    // Act
    Result<DFUWriteResponse> dfuResponse = dfuWriteCommand.parseResponse(response);
    // Assert
    assertThat(dfuResponse.failure()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void parseResponse_errorUnsupported() {
    // Arrange
    Response response = Response.newBuilder().setStatus(Status.ERROR_UNSUPPORTED).setId(0).build();
    // Act
    Result<DFUWriteResponse> dfuResponse = dfuWriteCommand.parseResponse(response);
    // Assert
    assertThat(dfuResponse.failure().getMessage().equals(Status.ERROR_UNSUPPORTED.name())).isTrue();
  }

  @Test
  public void parseResponse_successResponse() {
    // Arrange
    int crc = 1;
    DFUWriteResponse dfuWriteResponse = DFUWriteResponse.newBuilder().setCrc(crc).setOffset(OFF_SET)
        .build();
    Response response = Response.newBuilder()
        .setExtension(DFUWriteResponse.dfuWrite, dfuWriteResponse).setId(1)
        .setComponentId(Component.TAG_ID).setStatus(Status.STATUS_OK).build();
    // Act
    Result<DFUWriteResponse> result = dfuWriteCommand.parseResponse(response);
    // Assert
    DFUWriteResponse dfuWriterResponseResult = result.success();
    assertThat(dfuWriterResponseResult.getCrc()).isEqualTo(crc);
    assertThat(dfuWriterResponseResult.getOffset()).isEqualTo(OFF_SET);
  }
}
