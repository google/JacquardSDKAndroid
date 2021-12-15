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
package com.google.android.jacquard.sdk.command;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build.VERSION_CODES;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.model.Component;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.atap.jacquard.protocol.JacquardProtocol.BleConfiguration;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;
import com.google.atap.jacquard.protocol.JacquardProtocol.UJTConfigReadRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.UJTConfigResponse;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link UjtReadConfigCommand} class.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class UjtReadConfigCommandTest {

  private final static String TAG_NAME = "Jacquard t";
  private UjtReadConfigCommand command;

  @Before
  public void setup() {
    command = new UjtReadConfigCommand();
  }

  @Test
  public void getRequest() {
    // Act
    Request request = command.getRequest();
    // Assert
    assertThat(request.getComponentId()).isEqualTo(Component.TAG_ID);
    assertThat(request.getId()).isEqualTo(/*expected =*/ 0);
    assertThat(request.getOpcode()).isEqualTo(Opcode.CONFIG_READ);
    assertThat(request.getDomain()).isEqualTo(Domain.BASE);
    assertThat(request.getExtension(UJTConfigReadRequest.configRead).getBleConfig()).isTrue();
  }

  @Test
  public void parseResponse_throwsIllegalStateException() {
    // Arrange
    Response response = Response.newBuilder().setComponentId(Component.TAG_ID).setId(0)
        .setStatus(Status.STATUS_OK).build();
    // Act
    Result<UJTConfigResponse> bleConfigurationResponse = command.parseResponse(response);
    // Assert
    assertThat(bleConfigurationResponse.failure()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void parseResponse_failureResponse() {
    // Arrange
    Response response = Response.newBuilder()
        .setId(1)
        .setComponentId(1)
        .setStatus(Status.ERROR_APP_UNKNOWN)
        .build();
    // Act
    Result<UJTConfigResponse> result = command.parseResponse(response);
    // Assert
    assertThat(result.failure()).isInstanceOf(Throwable.class);
  }

  @Test
  public void parseResponse_successResponse() {
    // Arrange
    BleConfiguration bleConfiguration = BleConfiguration.newBuilder()
        .setCustomAdvName(TAG_NAME)
        .setCustomAdvNameBytes(ByteString.copyFromUtf8(TAG_NAME))
        .setMaxConnInterval(1000)
        .setMinConnInterval(500)
        .build();
    UJTConfigResponse ujtConfigResponse = UJTConfigResponse.newBuilder()
        .setBleConfig(bleConfiguration)
        .build();
    Response response = JacquardProtocol.Response.newBuilder()
        .setExtension(UJTConfigResponse.configResponse, ujtConfigResponse)
        .setId(1).setComponentId(1).setStatus(Status.STATUS_OK).build();
    // Act
    Result<UJTConfigResponse> result = command.parseResponse(response);
    // Assert
    assertThat(result.success().getBleConfig().getCustomAdvName()).isEqualTo(TAG_NAME);
    assertThat(result.success().getBleConfig().getMaxConnInterval()).isEqualTo(1000);
    assertThat(result.success().getBleConfig().getMinConnInterval()).isEqualTo(500);
    assertThat(result.success().getBleConfig().getCustomAdvNameBytes().size()).isEqualTo(10);
  }
}
