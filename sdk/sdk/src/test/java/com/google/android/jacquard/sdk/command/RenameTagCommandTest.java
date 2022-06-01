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
package com.google.android.jacquard.sdk.command;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build.VERSION_CODES;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.model.Component;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;
import com.google.atap.jacquard.protocol.JacquardProtocol.UJTConfigResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.UJTConfigWriteRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link RenameTagCommand}
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class RenameTagCommandTest {

  private final static String TAG_NAME = "Jacquard Tag";

  private RenameTagCommand renameTagCommand;

  @Before
  public void setup() {
    renameTagCommand = new RenameTagCommand(TAG_NAME);
  }

  @Test
  public void getRequest() {
    // Act
    Request request = renameTagCommand.getRequest();
    // Assert
    assertThat(request.getComponentId()).isEqualTo(Component.TAG_ID);
    assertThat(request.getOpcode()).isEqualTo(Opcode.CONFIG_WRITE);
    assertThat(request.getDomain()).isEqualTo(Domain.BASE);
    assertThat(
        request.getExtension(UJTConfigWriteRequest.configWrite).getBleConfig().getCustomAdvName())
        .isEqualTo(TAG_NAME);
  }

  @Test
  public void parseResponse_successResponse() {
    // Assign
    String renameTagName = "Jacquard t";
    JacquardProtocol.BleConfiguration bleConfiguration = JacquardProtocol.BleConfiguration
        .newBuilder().setCustomAdvName(renameTagName)
        .build();
    UJTConfigResponse ujtConfigResponse = UJTConfigResponse.newBuilder()
        .setBleConfig(bleConfiguration).build();
    Response response =
        Response.newBuilder()
            .setComponentId(Component.TAG_ID)
            .setId(renameTagCommand.getId())
            .setStatus(Status.STATUS_OK)
            .setExtension(UJTConfigResponse.configResponse, ujtConfigResponse)
            .build();
    // Act
    Result<String> renameCommandResponse = renameTagCommand
        .responseErrorCheck(response.toByteArray());
    // Assert
    assertThat(renameCommandResponse.success().equals(renameTagName)).isTrue();
  }
}
